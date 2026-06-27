package com.github.tier940.legacycraft.integration.logisticspipes;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import buildcraft.additionalpipes.api.ITeleportPipe;
import buildcraft.additionalpipes.api.TeleportManagerBase;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.transport.tile.TilePipeHolder;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.CacheHolder;

public class TeleportPipeConnectionNotifier {

    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getWorld() instanceof World)) return;
        World world = (World) event.getWorld();
        if (world.isRemote) return;

        BlockPos pos = event.getPos();
        TileEntity te = world.getTileEntity(pos);

        ITeleportPipe teleportPipe = getTeleportPipe(te);
        if (teleportPipe != null) {
            // Teleport pipe changed: notify LP pipes here and at the remote end
            scheduleAdjacentLP(world, pos);
            scheduleRemoteAdjacentLP(teleportPipe);
        } else if (te instanceof LogisticsTileGenericPipe && TeleportManagerBase.INSTANCE != null) {
            // LP pipe changed: notify LP pipes at the remote end of any adjacent teleport pipe
            for (EnumFacing face : EnumFacing.VALUES) {
                ITeleportPipe adjacent = getTeleportPipe(world.getTileEntity(pos.offset(face)));
                if (adjacent != null) {
                    scheduleRemoteAdjacentLP(adjacent);
                }
            }
        }
    }

    private void scheduleAdjacentLP(World world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.VALUES) {
            TileEntity neighbor = world.getTileEntity(pos.offset(face));
            if (neighbor instanceof LogisticsTileGenericPipe) {
                LogisticsTileGenericPipe lp = (LogisticsTileGenericPipe) neighbor;
                lp.scheduleNeighborChange();
                CoreRoutedPipe routingPipe = lp.getRoutingPipe();
                if (routingPipe != null) {
                    CacheHolder cacheHolder = routingPipe.getCacheHolder();
                    if (cacheHolder != null) {
                        cacheHolder.trigger(CacheHolder.CacheTypes.Routing);
                    }
                }
            }
        }
    }

    private void scheduleRemoteAdjacentLP(ITeleportPipe pipe) {
        TeleportManagerBase manager = TeleportManagerBase.INSTANCE;
        if (manager == null) return;
        Set<ITeleportPipe> remotes = new HashSet<>();
        if (pipe.canSend()) remotes.addAll(manager.getConnectedPipes(pipe, false, true));
        if (pipe.canReceive()) remotes.addAll(manager.getConnectedPipes(pipe, true, false));
        for (ITeleportPipe remote : remotes) {
            scheduleAtHolder(remote.getContainer());
        }
    }

    private void scheduleAtHolder(TilePipeHolder holder) {
        if (holder == null || holder.isInvalid()) return;
        World remoteWorld = holder.getWorld();
        if (remoteWorld == null) return;
        scheduleAdjacentLP(remoteWorld, holder.getPos());
    }

    private ITeleportPipe getTeleportPipe(TileEntity tile) {
        if (!(tile instanceof TilePipeHolder)) return null;
        IPipe pipe = ((TilePipeHolder) tile).getPipe();
        if (pipe == null) return null;
        PipeBehaviour behaviour = pipe.getBehaviour();
        return behaviour instanceof ITeleportPipe ? (ITeleportPipe) behaviour : null;
    }
}
