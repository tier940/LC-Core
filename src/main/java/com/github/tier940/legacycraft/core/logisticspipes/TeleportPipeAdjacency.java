package com.github.tier940.legacycraft.core.logisticspipes;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import buildcraft.additionalpipes.api.ITeleportPipe;
import buildcraft.additionalpipes.api.TeleportPipeType;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.transport.tile.TilePipeHolder;

public final class TeleportPipeAdjacency {

    private TeleportPipeAdjacency() {}

    public static boolean isAdjacentToItemTeleport(World world, BlockPos pos) {
        if (world == null || pos == null) return false;
        for (EnumFacing side : EnumFacing.VALUES) {
            ITeleportPipe pipe = getTeleportPipe(world.getTileEntity(pos.offset(side)));
            if (pipe != null && pipe.getType() == TeleportPipeType.ITEMS) return true;
        }
        return false;
    }

    public static ITeleportPipe getTeleportPipe(TileEntity tile) {
        if (!(tile instanceof TilePipeHolder)) return null;
        IPipe pipe = ((TilePipeHolder) tile).getPipe();
        if (pipe == null) return null;
        PipeBehaviour behaviour = pipe.getBehaviour();
        return behaviour instanceof ITeleportPipe ? (ITeleportPipe) behaviour : null;
    }
}
