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

/**
 * Implements working energy transfer for the AP power teleport pipe.
 *
 * <p>
 * AP 6.0.0.8 declares {@code IPipeTransportPowerHook} on the behaviour, but BCR checks the
 * {@code Pipe} entity (which is {@code final} and never implements the interface), making the hook
 * unreachable. Energy transfer is re-implemented in {@link #onTick()} instead.
 *
 * <p>
 * Also enforces fair-share energy distribution when multiple sender pipes share the same
 * channel, using a tick-stable demand snapshot read from the private {@code powerQuery} field.
 */
public class PipeBehaviorTeleportPowerFixed extends PipeBehaviorTeleportPower {

    // PipeFlowPower.powerQuery holds the previous-tick demand snapshot committed by step2().
    // step2() runs at most once per tick (currentDate guard), so powerQuery is stable for the
    // entire tick regardless of which sender pipe calls receiveEnergy() first. nextPowerQuery
    // would be zeroed by the first receiveEnergy() triggering step2(), causing later senders
    // in the same tick to see demand = 0 and bypass the fair-share cap.
    private static final Field POWER_QUERY_FIELD;

    // Initialised once at class-load; null if the field is renamed or obfuscated, in which case
    // powerQuery() falls back to nextPowerQuery (acceptable but slightly less accurate).
    static {
        Field f = null;
        try {
            f = PipeFlowPower.class.getDeclaredField("powerQuery");
            f.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException ignored) {}
        POWER_QUERY_FIELD = f;
    }

    // Returns the committed demand array for {@code flow}. Falls back to nextPowerQuery when
    // reflection is unavailable; nextPowerQuery is correct for single-sender scenarios and merely
    // risks a 0-demand race for the second-or-later sender in the same tick (acceptable fallback).
    private static int[] powerQuery(PipeFlowPower flow) {
        if (POWER_QUERY_FIELD != null) {
            try {
                int[] pq = (int[]) POWER_QUERY_FIELD.get(flow);
                if (pq != null) return pq;
            } catch (IllegalAccessException ignored) {}
        }
        return flow.nextPowerQuery;
    }

    /** IPipeCreator path: called when a new power teleport pipe is placed in the world. */
    public PipeBehaviorTeleportPowerFixed(IPipe pipe) {
        super(pipe);
    }

    /** IPipeLoader path: called when deserializing an existing pipe from a world save (NBT). */
    public PipeBehaviorTeleportPowerFixed(IPipe pipe, NBTTagCompound nbt) {
        super(pipe, nbt);
    }

    @Override
    public void onTick() {
        super.onTick();

        World world = pipe.getHolder().getPipeWorld();
        // world == null during construction / before the chunk fully loads; isRemote skips clients
        // because TeleportManager is server-side only.
        if (world == null || world.isRemote) return;
        // canSend() returns false when the pipe has no channel set or is unregistered.
        if (!canSend()) return;

        // Guard: only PipeFlowPower pipes carry RF/MJ; any other flow type means misconfiguration.
        PipeFlow rawFlow = pipe.getFlow();
        if (!(rawFlow instanceof PipeFlowPower)) return;
        PipeFlowPower localFlow = (PipeFlowPower) rawFlow;
        localFlow.maxPower = Integer.MAX_VALUE; // BCR caps power by default; remove the cap so we can pull freely.

        // getConnectedPipes(pipe, includeSenders=false, includeReceivers=true): returns only the receiver endpoints.
        List<ITeleportPipe> destinations = TeleportManager.instance.getConnectedPipes(this, false, true);
        if (destinations.isEmpty()) return;

        // Use powerQuery (previous-tick snapshot) for tick-stable demand across all senders.
        // powerQuery is a per-face array; summing all faces gives the total demand the receiver
        // exposed to the network last tick, regardless of which face ultimately delivers power.
        long receiverDemand = 0;
        for (ITeleportPipe dest : destinations) {
            if (!(dest instanceof PipeBehaviorTeleportPower)) continue;
            PipeFlow destRaw = ((PipeBehaviorTeleportPower) dest).pipe.getFlow();
            if (!(destRaw instanceof PipeFlowPower)) continue;
            for (int q : powerQuery((PipeFlowPower) destRaw)) {
                receiverDemand += q;
            }
        }

        // getConnectedPipes(pipe, includeSenders=true, includeReceivers=false) returns OTHER senders
        // on the same channel, not this pipe. +1 accounts for this pipe so the fair-share divisor
        // reflects the true total number of senders.
        int numSenders = TeleportManager.instance.getConnectedPipes(this, true, false).size() + 1;

        // Drain internalNextPower: BCR accumulates power here each tick before distributing.
        // We pull it out manually because the normal distribution path never reaches receivers.
        // The array is indexed by EnumFacing ordinal; we sum all faces and zero each slot so
        // BCR does not double-count the same energy in its own distribution pass this tick.
        double total = 0;
        for (int i = 0; i < localFlow.internalNextPower.length; i++) {
            total += localFlow.internalNextPower[i];
            localFlow.internalNextPower[i] = 0;
        }
        // Cap at fair share so N senders collectively deliver receiverDemand, not N x receiverDemand.
        // Skip the cap when numSenders == 1 (no contention) or receiverDemand == 0 (receiver full
        // or offline); in either case the raw total is safe to forward without division.
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

                // receiveEnergy() requires a non-null face; pick the first face not occupied by
                // another pipe. DOWN is the safe fallback when all neighbours are pipes (unusual),
                // because receiveEnergy() still accepts energy regardless of which face is given.
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

        // No demand means receivers are full or offline; skip the pull-signal entirely to avoid
        // pushing a zero-demand request that confuses adjacent pipes.
        if (receiverDemand <= 0) return;

        // Signal adjacent BC power pipes to supply energy next tick (pull-based model).
        long demandPerSender = receiverDemand / numSenders;
        // demandPerSender can be 0 from integer division when receiverDemand < numSenders;
        // requesting 0 energy has no effect but wastes the adjacency scan below.
        if (demandPerSender <= 0) return;
        // Clamp to int: requestEnergy() takes an int, and receiverDemand is a long to avoid
        // overflow when summing per-face demand values across many receiver pipes.
        int demand = (int) Math.min(demandPerSender, Integer.MAX_VALUE);

        // Walk all six neighbours: only BC power pipes (TilePipeHolder + PipeFlowPower) understand
        // requestEnergy(). Non-pipe neighbours (machines, air) are silently skipped.
        // face.getOpposite() is used because requestEnergy() expects the face *on the adjacent pipe*
        // from which this sender is pulling, i.e. the face that points back toward this pipe.
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
