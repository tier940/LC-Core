package com.github.tier940.legacycraft.integration.logisticspipes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import buildcraft.additionalpipes.api.ITeleportPipe;
import buildcraft.additionalpipes.api.TeleportManagerBase;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.transport.tile.TilePipeHolder;
import logisticspipes.interfaces.routing.ISpecialPipedConnection;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.specialconnection.SpecialPipeConnection.ConnectionInformation;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;

/**
 * Bridges the Logistics Pipes routing pathfinder with Additional Pipes teleport pipes, exposing the
 * LP routing pipes on the far side of a teleport link as routable connections.
 */
public class AdditionalPipesTeleportConnection implements ISpecialPipedConnection {

    private static final double TELEPORT_EDGE_WEIGHT = 1.0;
    private static final EnumSet<PipeRoutingConnectionType> ITEM_FLAGS = EnumSet
            .of(PipeRoutingConnectionType.canRouteTo, PipeRoutingConnectionType.canRequestFrom);

    @Override
    public boolean init() {
        return TeleportManagerBase.INSTANCE != null;
    }

    @Override
    public boolean isType(IPipeInformationProvider startPipe) {
        World world = getWorld(startPipe);
        if (world == null) return false;
        BlockPos pos = new BlockPos(startPipe.getX(), startPipe.getY(), startPipe.getZ());
        for (EnumFacing side : EnumFacing.VALUES) {
            if (isTeleportPipe(world.getTileEntity(pos.offset(side)))) return true;
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

        World world = getWorld(startPipe);
        if (world == null) return result;

        // LP intersects routing flags per hop, so each edge must keep both or routing/requesting
        // across the teleport breaks.
        EnumSet<PipeRoutingConnectionType> activeFlags = EnumSet.copyOf(connection);
        activeFlags.retainAll(ITEM_FLAGS);
        if (activeFlags.isEmpty()) return result;

        BlockPos pos = new BlockPos(startPipe.getX(), startPipe.getY(), startPipe.getZ());
        EnumFacing[] facesToCheck = (side != null) ? new EnumFacing[] { side } : EnumFacing.VALUES;
        Set<IPipeInformationProvider> seen = new HashSet<>();

        for (EnumFacing face : facesToCheck) {
            TileEntity adjacentTile = world.getTileEntity(pos.offset(face));
            if (!isTeleportPipe(adjacentTile)) continue;
            ITeleportPipe sourceTeleport = getTeleportPipe(adjacentTile);

            if (sourceTeleport.canSend()) {
                addRemoteEdges(result, manager.getConnectedPipes(sourceTeleport, false, true),
                        activeFlags, face, seen);
            }
            if (sourceTeleport.canReceive()) {
                addRemoteEdges(result, manager.getConnectedPipes(sourceTeleport, true, false),
                        activeFlags, face, seen);
            }
        }

        return result;
    }

    private void addRemoteEdges(
                                List<ConnectionInformation> result,
                                List<ITeleportPipe> destinations,
                                EnumSet<PipeRoutingConnectionType> flags,
                                EnumFacing face,
                                Set<IPipeInformationProvider> seen) {
        for (ITeleportPipe dest : destinations) {
            TilePipeHolder remoteHolder = dest.getContainer();
            if (remoteHolder == null || remoteHolder.isInvalid()) continue;

            World remoteWorld = remoteHolder.getWorld();
            if (remoteWorld == null) continue;
            for (EnumFacing exitSide : EnumFacing.VALUES) {
                TileEntity neighbor = remoteWorld.getTileEntity(remoteHolder.getPos().offset(exitSide));
                if (neighbor == null) continue;

                IPipeInformationProvider provider = SimpleServiceLocator.pipeInformationManager
                        .getInformationProviderFor(neighbor);
                if (provider != null && provider.isRoutingPipe() && seen.add(provider)) {
                    result.add(new ConnectionInformation(
                            provider, EnumSet.copyOf(flags), exitSide.getOpposite(), face, TELEPORT_EDGE_WEIGHT));
                }
            }
        }
    }

    // startPipe.getWorld() throws AbstractMethodError for LogisticsTileGenericPipe; go via the tile.
    private World getWorld(IPipeInformationProvider startPipe) {
        TileEntity tile = startPipe.getTile();
        return tile != null ? tile.getWorld() : null;
    }

    private boolean isTeleportPipe(TileEntity tile) {
        return getTeleportPipe(tile) != null;
    }

    // An AP teleport pipe is a PipeBehaviour inside a BCR TilePipeHolder, not a TileEntity itself.
    private ITeleportPipe getTeleportPipe(TileEntity tile) {
        if (!(tile instanceof TilePipeHolder)) return null;
        IPipe pipe = ((TilePipeHolder) tile).getPipe();
        if (pipe == null) return null;
        PipeBehaviour behaviour = pipe.getBehaviour();
        return behaviour instanceof ITeleportPipe ? (ITeleportPipe) behaviour : null;
    }
}
