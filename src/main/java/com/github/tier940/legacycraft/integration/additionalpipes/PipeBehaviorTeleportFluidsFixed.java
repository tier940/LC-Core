package com.github.tier940.legacycraft.integration.additionalpipes;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.Fluid;

import buildcraft.additionalpipes.pipes.PipeBehaviorTeleportFluids;
import buildcraft.additionalpipes.pipes.TeleportManager;
import buildcraft.api.transport.pipe.IPipe;

/**
 * Fixes the fluid teleport pipe when multiple sender pipes share the same channel.
 *
 * <p>
 * Each sender's {@code preMoveCenter} reads receiver capacity before any sender has committed
 * fluid. Without this fix every sender tries to claim the full capacity, causing all but one to
 * fail and stall their upstream. This subclass caps the reported capacity to each sender's fair
 * share ({@code capacity / numSenders}).
 */
public class PipeBehaviorTeleportFluidsFixed extends PipeBehaviorTeleportFluids {

    /** IPipeCreator path: called when a new fluid teleport pipe is placed in the world. */
    public PipeBehaviorTeleportFluidsFixed(IPipe pipe) {
        super(pipe);
    }

    /** IPipeLoader path: called when deserializing an existing pipe from a world save (NBT). */
    public PipeBehaviorTeleportFluidsFixed(IPipe pipe, NBTTagCompound nbt) {
        super(pipe, nbt);
    }

    /**
     * Returns the fair-share capacity this sender may claim from the shared receiver this tick.
     *
     * <p>
     * Called by AP's {@code preMoveCenter} on the server before any fluid is committed, so all
     * senders on the same channel see the same {@code actual} value. The unit is millibuckets (mB).
     * Returns 0 when the receiver is full or absent (super returns 0 in those cases).
     */
    @Override
    public int getMaxAcceptableMB(Fluid fluid) {
        int actual = super.getMaxAcceptableMB(fluid);
        // getConnectedPipes excludes this pipe itself (AP uses `pipe != other` internally).
        // +1 to include this sender so numSenders reflects the true total number of senders.
        // TeleportManager.instance is a server-only singleton; calling it on the client would NPE.
        // AP's fluid pipe movement runs server-side, so this method is only reached on the server.
        int numSenders = TeleportManager.instance.getConnectedPipes(this, true, false).size() + 1;
        // numSenders == 1: this pipe is the sole sender; full capacity is correct.
        // The original value from super is safe to return unchanged.
        if (numSenders <= 1) return actual;
        // Integer division intentionally rounds down (conservative under-claim is safe for fluids).
        // Math.max(0, ...) guards against a hypothetical negative sentinel from super.
        return Math.max(0, actual / numSenders);
    }
}
