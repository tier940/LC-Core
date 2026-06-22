package com.github.tier940.legacycraft.core.additionalpipes;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import buildcraft.additionalpipes.api.ITeleportPipe;
import buildcraft.additionalpipes.pipes.PipeBehaviorTeleportPower;
import buildcraft.additionalpipes.pipes.TeleportManager;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeFlow;
import buildcraft.transport.pipe.flow.PipeFlowPower;
import buildcraft.transport.tile.TilePipeHolder;

// AP 6.0.0.8: IPipeTransportPowerHook is on the behaviour but BCR checks the Pipe entity
// (final, never implements the interface), so the hook is unreachable. Fix via onTick().
public class PipeBehaviorTeleportPowerFixed extends PipeBehaviorTeleportPower {

    // PipeFlowPower.powerQuery holds the previous-tick demand snapshot committed by step2().
    // step2() runs at most once per tick (currentDate guard), so powerQuery is stable for the
    // entire tick regardless of which sender pipe calls receiveEnergy() first. nextPowerQuery
    // would be zeroed by the first receiveEnergy() triggering step2(), causing later senders
    // in the same tick to see demand = 0 and bypass the fair-share cap.
    private static final Field POWER_QUERY_FIELD;

    static {
        Field f = null;
        try {
            f = PipeFlowPower.class.getDeclaredField("powerQuery");
            f.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException ignored) {}
        POWER_QUERY_FIELD = f;
    }

    private static int[] powerQuery(PipeFlowPower flow) {
        if (POWER_QUERY_FIELD != null) {
            try {
                int[] pq = (int[]) POWER_QUERY_FIELD.get(flow);
                if (pq != null) return pq;
            } catch (IllegalAccessException ignored) {}
        }
        return flow.nextPowerQuery;
    }

    public PipeBehaviorTeleportPowerFixed(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviorTeleportPowerFixed(IPipe pipe, NBTTagCompound nbt) {
        super(pipe, nbt);
    }

    @Override
    public void onTick() {
        super.onTick();

        World world = pipe.getHolder().getPipeWorld();
        if (world == null || world.isRemote) return;
        if (!canSend()) return;

        PipeFlow rawFlow = pipe.getFlow();
        if (!(rawFlow instanceof PipeFlowPower)) return;
        PipeFlowPower localFlow = (PipeFlowPower) rawFlow;
        localFlow.maxPower = Integer.MAX_VALUE;

        List<ITeleportPipe> destinations = TeleportManager.instance.getConnectedPipes(this, false, true);
        if (destinations.isEmpty()) return;

        // Read from powerQuery (previous-tick snapshot) so all senders on this channel
        // see the same demand value within a single tick.
        long receiverDemand = 0;
        for (ITeleportPipe dest : destinations) {
            if (!(dest instanceof PipeBehaviorTeleportPower)) continue;
            PipeFlow destRaw = ((PipeBehaviorTeleportPower) dest).pipe.getFlow();
            if (!(destRaw instanceof PipeFlowPower)) continue;
            for (int q : powerQuery((PipeFlowPower) destRaw)) {
                receiverDemand += q;
            }
        }

        int numSenders = TeleportManager.instance.getConnectedPipes(this, true, false).size() + 1;

        double total = 0;
        for (int i = 0; i < localFlow.internalNextPower.length; i++) {
            total += localFlow.internalNextPower[i];
            localFlow.internalNextPower[i] = 0;
        }
        // Cap at fair share so N senders collectively deliver receiverDemand, not N x receiverDemand.
        if (numSenders > 1 && receiverDemand > 0) {
            total = Math.min(total, (double) receiverDemand / numSenders);
        }

        if (total > 0) {
            double perPipe = total / destinations.size();
            for (ITeleportPipe dest : destinations) {
                if (!(dest instanceof PipeBehaviorTeleportPower)) continue;
                PipeFlow destRaw = ((PipeBehaviorTeleportPower) dest).pipe.getFlow();
                if (!(destRaw instanceof PipeFlowPower)) continue;
                PipeFlowPower destFlow = (PipeFlowPower) destRaw;
                destFlow.maxPower = Integer.MAX_VALUE;

                IPipeHolder dHolder = ((PipeBehaviorTeleportPower) dest).pipe.getHolder();
                World dWorld = dHolder.getPipeWorld();
                BlockPos dPos = dHolder.getPipePos();
                EnumFacing injectFace = EnumFacing.DOWN;
                if (dWorld != null) {
                    for (EnumFacing f : EnumFacing.VALUES) {
                        if (!(dWorld.getTileEntity(dPos.offset(f)) instanceof TilePipeHolder)) {
                            injectFace = f;
                            break;
                        }
                    }
                }
                destFlow.receiveEnergy(injectFace, perPipe);
            }
        }

        if (receiverDemand <= 0) return;

        long demandPerSender = receiverDemand / numSenders;
        if (demandPerSender <= 0) return;
        int demand = (int) Math.min(demandPerSender, Integer.MAX_VALUE);

        BlockPos pos = pipe.getHolder().getPipePos();
        for (EnumFacing face : EnumFacing.VALUES) {
            TileEntity adjTe = world.getTileEntity(pos.offset(face));
            if (!(adjTe instanceof TilePipeHolder)) continue;
            PipeFlow adjFlow = ((TilePipeHolder) adjTe).getPipe().getFlow();
            if (!(adjFlow instanceof PipeFlowPower)) continue;
            ((PipeFlowPower) adjFlow).requestEnergy(face.getOpposite(), demand);
        }
    }
}
