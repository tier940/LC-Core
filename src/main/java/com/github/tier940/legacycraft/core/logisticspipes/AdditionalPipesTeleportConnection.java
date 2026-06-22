package com.github.tier940.legacycraft.core.logisticspipes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import buildcraft.additionalpipes.api.ITeleportPipe;
import buildcraft.additionalpipes.api.TeleportManagerBase;
import buildcraft.additionalpipes.api.TeleportPipeType;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.transport.tile.TilePipeHolder;
import logisticspipes.interfaces.routing.ISpecialPipedConnection;
import logisticspipes.interfaces.routing.ISpecialTileConnection;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.specialconnection.SpecialPipeConnection.ConnectionInformation;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.utils.item.ItemIdentifierStack;

// Bridges LP routing pathfinder with AP teleport pipes (Issue RS485/LogisticsPipes#348).
public class AdditionalPipesTeleportConnection implements ISpecialPipedConnection, ISpecialTileConnection {

    @Override
    public boolean init() {
        return TeleportManagerBase.INSTANCE != null;
    }

    // ── ISpecialPipedConnection ──────────────────────────────────────────────

    @Override
    public boolean isType(IPipeInformationProvider startPipe) {
        World world = startPipe.getWorld();
        if (world == null) return false;
        BlockPos pos = new BlockPos(startPipe.getX(), startPipe.getY(), startPipe.getZ());
        for (EnumFacing side : EnumFacing.VALUES) {
            if (isSupportedTeleportPipe(world.getTileEntity(pos.offset(side)))) return true;
        }
        return false;
    }

    @Override
    public List<ConnectionInformation> getConnections(
            IPipeInformationProvider startPipe,
            EnumSet<PipeRoutingConnectionType> connection,
            EnumFacing side) {
        List<ConnectionInformation> result = new ArrayList<>();

        TeleportManagerBase manager = TeleportManagerBase.INSTANCE;
        if (manager == null) return result;

        World world = startPipe.getWorld();
        if (world == null) return result;
        BlockPos pos = new BlockPos(startPipe.getX(), startPipe.getY(), startPipe.getZ());

        // side == null is PathFinder's initial "scan all directions" call.
        EnumFacing[] facesToCheck = (side != null) ? new EnumFacing[]{side} : EnumFacing.VALUES;

        for (EnumFacing face : facesToCheck) {
            TileEntity adjacentTile = world.getTileEntity(pos.offset(face));
            if (!isSupportedTeleportPipe(adjacentTile)) continue;
            ITeleportPipe sourceTeleport = getTeleportPipe(adjacentTile);

            // getConnectedPipes(pipe, includeSend, includeReceive):
            // From a send-end, find receive-capable peers; from a receive-end, find send-capable peers.
            List<ITeleportPipe> destinations;
            if (sourceTeleport.canSend()) {
                destinations = manager.getConnectedPipes(sourceTeleport, false, true);
            } else if (sourceTeleport.canReceive()) {
                destinations = manager.getConnectedPipes(sourceTeleport, true, false);
            } else {
                continue;
            }

            for (ITeleportPipe dest : destinations) {
                TilePipeHolder destContainer = dest.getContainer();
                if (destContainer == null || destContainer.isInvalid()) continue;

                World destWorld = destContainer.getWorld();
                if (destWorld == null) continue;
                for (EnumFacing exitSide : EnumFacing.VALUES) {
                    BlockPos neighborPos = destContainer.getPos().offset(exitSide);
                    TileEntity neighbor = destWorld.getTileEntity(neighborPos);
                    if (neighbor == null) continue;

                    IPipeInformationProvider provider = SimpleServiceLocator.pipeInformationManager
                            .getInformationProviderFor(neighbor);
                    if (provider != null && provider.isRoutingPipe()) {
                        result.add(new ConnectionInformation(
                                provider, connection, exitSide.getOpposite(), face, 1.0));
                    }
                }
            }
        }

        return result;
    }

    // ── ISpecialTileConnection ───────────────────────────────────────────────

    @Override
    public boolean isType(TileEntity tile) {
        return isSupportedTeleportPipe(tile);
    }

    @Override
    public Collection<TileEntity> getConnections(TileEntity tile) {
        if (!isSupportedTeleportPipe(tile)) return Collections.emptyList();
        TilePipeHolder holder = (TilePipeHolder) tile;
        World world = holder.getWorld();
        if (world == null) return Collections.emptyList();
        BlockPos pos = holder.getPos();
        List<TileEntity> result = new ArrayList<>();
        for (EnumFacing face : EnumFacing.VALUES) {
            TileEntity neighbor = world.getTileEntity(pos.offset(face));
            if (neighbor instanceof LogisticsTileGenericPipe) {
                result.add(neighbor);
            }
        }
        return result;
    }

    @Override
    public boolean needsInformationTransition() {
        return true;
    }

    @Override
    public void transmit(TileEntity tile, IRoutedItem data) {
        if (!(tile instanceof TilePipeHolder)) return;
        TilePipeHolder holder = (TilePipeHolder) tile;
        World world = holder.getWorld();
        if (world == null) return;
        BlockPos pos = holder.getPos();
        for (EnumFacing face : EnumFacing.VALUES) {
            TileEntity neighbor = world.getTileEntity(pos.offset(face));
            if (!(neighbor instanceof LogisticsTileGenericPipe)) continue;
            CoreUnroutedPipe unrouted = ((LogisticsTileGenericPipe) neighbor).pipe;
            if (unrouted instanceof CoreRoutedPipe) {
                ((CoreRoutedPipe) unrouted).queueUnroutedItemInformation(
                        new ItemIdentifierStack(data.getItemIdentifierStack()),
                        data.getInfo());
                return;
            }
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private boolean isSupportedTeleportPipe(TileEntity tile) {
        ITeleportPipe tp = getTeleportPipe(tile);
        if (tp == null) return false;
        TeleportPipeType type = tp.getType();
        return type == TeleportPipeType.ITEMS || type == TeleportPipeType.POWER;
    }

    private ITeleportPipe getTeleportPipe(TileEntity tile) {
        if (!(tile instanceof TilePipeHolder)) return null;
        IPipe pipe = ((TilePipeHolder) tile).getPipe();
        if (pipe == null) return null;
        PipeBehaviour behaviour = pipe.getBehaviour();
        return behaviour instanceof ITeleportPipe ? (ITeleportPipe) behaviour : null;
    }
}
