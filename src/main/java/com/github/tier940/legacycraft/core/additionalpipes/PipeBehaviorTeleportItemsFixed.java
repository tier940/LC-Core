package com.github.tier940.legacycraft.core.additionalpipes;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import buildcraft.additionalpipes.pipes.PipeBehaviorTeleportItems;
import buildcraft.api.transport.pipe.IPipe;

/**
 * Fixes the item teleport pipe crash when the receiver is placed in a straight-pipe run.
 *
 * <p>
 * AP's {@code getTeleportSide()} searches for a face that is unconnected while its opposite
 * IS connected. That condition is never satisfied for a straight pipe (e.g. NORTH+SOUTH only),
 * so the method returns {@code null}. Passing {@code null} to {@code insertItemsForce()} causes
 * an NPE. This subclass falls back to the first unconnected face in that case.
 *
 * <p>
 * Note: {@code orderSides()} in the upstream class also calls {@code getTeleportSide()} and
 * already swallows the NPE with a bare catch block. This override silently fixes that path too
 * by ensuring {@code getTeleportSide()} never returns {@code null} on the server.
 */
public class PipeBehaviorTeleportItemsFixed extends PipeBehaviorTeleportItems {

    public PipeBehaviorTeleportItemsFixed(IPipe pipe) {
        super(pipe);
    }

    public PipeBehaviorTeleportItemsFixed(IPipe pipe, NBTTagCompound nbt) {
        super(pipe, nbt);
    }

    @Override
    public EnumFacing getTeleportSide() {
        // super searches for a face f where !connected(f) && connected(f.opposite); if none exists
        // (straight pipe: e.g. NORTH+SOUTH only) it returns null after its own recalculation attempt.
        EnumFacing result = super.getTeleportSide();
        if (result != null) return result; // non-null: super found a valid teleport face; forward it directly.
        // super.getTeleportSide() returns null on both the client and the straight-pipe case.
        // isClient() below guards the server-only TeleportManager lookup; returning null on the
        // client is safe because the movement pipeline does not run there.
        if (isClient()) return null;
        // Pick any face that has no pipe connection; insertItemsForce() uses this as the arrival
        // direction when injecting into the pipe network — any free face is safe because items
        // are routed by the network, not delivered to the specific neighbour block.
        // EnumFacing.VALUES order: DOWN, UP, NORTH, SOUTH, WEST, EAST (enum declaration order).
        for (EnumFacing f : EnumFacing.VALUES) {
            if (!pipe.isConnected(f)) return f;
        }
        // All six faces are connected — this should not occur in normal gameplay but can happen
        // transiently during chunk load. DOWN is returned as a last resort; insertItemsForce() will
        // still inject items into the pipe flow even though DOWN has a connected neighbour.
        return EnumFacing.DOWN;
    }
}
