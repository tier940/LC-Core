package com.github.tier940.legacycraft.core.additionalpipes;

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
import buildcraft.api.transport.pipe.PipeFlow;
import buildcraft.transport.pipe.flow.PipeFlowPower;
import buildcraft.transport.tile.TilePipeHolder;

// AP 6.0.0.8: IPipeTransportPowerHook is on the behaviour but BCR checks the Pipe entity
// (final, never implements the interface), so the hook is unreachable. Fix via onTick().
public class PipeBehaviorTeleportPowerFixed extends PipeBehaviorTeleportPower {

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
        // receiveEnergy() hard-caps at maxPower per side; remove the limit on sender.
        localFlow.maxPower = Integer.MAX_VALUE;

        // Drain internalNextPower[] before flow.onTick() → step2() promotes it to
        // internalPower[]. BCR discards any internalPower that has no downstream demand,
        // so we intercept here and push directly to receivers.
        double total = 0;
        for (int i = 0; i < localFlow.internalNextPower.length; i++) {
            total += localFlow.internalNextPower[i];
            localFlow.internalNextPower[i] = 0;
        }

        List<ITeleportPipe> destinations = TeleportManager.instance.getConnectedPipes(this, false, true);
        if (destinations.isEmpty()) return;

        // Mechanism 1: teleport drained power to receivers.
        if (total > 0) {
            double perPipe = total / destinations.size();
            for (ITeleportPipe dest : destinations) {
                if (!(dest instanceof PipeBehaviorTeleportPower)) continue;
                PipeFlow destRaw = ((PipeBehaviorTeleportPower) dest).pipe.getFlow();
                if (!(destRaw instanceof PipeFlowPower)) continue;
                PipeFlowPower destFlow = (PipeFlowPower) destRaw;
                // Remove receiver cap so the full amount is accepted.
                destFlow.maxPower = Integer.MAX_VALUE;
                destFlow.receiveEnergy(EnumFacing.DOWN, perPipe);
            }
        }

        // Mechanism 2: register demand on upstream pipes equal to what receivers'
        // consumers actually want (nextPowerQuery[] from the previous tick).
        // BCR only pushes power when powerQuery[side] > 0; the teleport pipe has no
        // physical downstream consumers so we must propagate demand manually.
        // Using receiver demand prevents over-pulling that would be silently discarded.
        long receiverDemand = 0;
        for (ITeleportPipe dest : destinations) {
            if (!(dest instanceof PipeBehaviorTeleportPower)) continue;
            PipeFlow destRaw = ((PipeBehaviorTeleportPower) dest).pipe.getFlow();
            if (!(destRaw instanceof PipeFlowPower)) continue;
            for (int q : ((PipeFlowPower) destRaw).nextPowerQuery) {
                receiverDemand += q;
            }
        }

        if (receiverDemand <= 0) return;
        int demand = (int) Math.min(receiverDemand, Integer.MAX_VALUE);

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
