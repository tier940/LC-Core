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

/**
 * Bridges the Logistics Pipes routing pathfinder with Additional Pipes teleport pipes.
 *
 * <p>
 * Without this handler, LP's pathfinder treats a teleport pipe as a dead-end because it has
 * no physical neighbour on the remote side. This class implements {@link ISpecialPipedConnection}
 * and {@link ISpecialTileConnection} so LP follows the teleport link to the remote end and
 * discovers LP pipes connected there. Fixes RS485/LogisticsPipes#348.
 */
public class AdditionalPipesTeleportConnection implements ISpecialPipedConnection, ISpecialTileConnection {

    // Weight assigned to each teleport hop in LP's routing tree; 1.0 treats it as one pipe segment.
    // Raise this value to bias routing toward physical pipe paths when cheaper alternatives exist.
    private static final double TELEPORT_EDGE_WEIGHT = 1.0;

    // TeleportManagerBase.INSTANCE is null when Additional Pipes is absent or failed to load;
    // returning false causes the caller to skip registration of this handler entirely.
    @Override
    public boolean init() {
        return TeleportManagerBase.INSTANCE != null;
    }

    // Called by LP's pathfinder to decide whether to ask getConnections() for this pipe.
    // Returns true if ANY adjacent tile is a teleport pipe, not just the one on `side`,
    // because LP does not pass a side to this overload.
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
        BlockPos pos = new BlockPos(startPipe.getX(), startPipe.getY(), startPipe.getZ());

        // LP calls getConnections() twice: once with side == null during the initial fan-out
        // (scan every face) and again with a specific side during directed traversal. When a
        // specific side is given we only need to inspect that face's adjacent tile.
        EnumFacing[] facesToCheck = (side != null) ? new EnumFacing[] { side } : EnumFacing.VALUES;

        for (EnumFacing face : facesToCheck) {
            TileEntity adjacentTile = world.getTileEntity(pos.offset(face));
            if (!isTeleportPipe(adjacentTile)) continue;
            ITeleportPipe sourceTeleport = getTeleportPipe(adjacentTile);

            // getConnectedPipes(pipe, includeSend, includeReceive): when the local pipe is a
            // send-end we want the receive-end peers (includeSend=false, includeReceive=true),
            // and vice-versa. A pipe that can neither send nor receive is a standalone stub;
            // skip it rather than returning an empty list that pollutes the routing table.
            List<ITeleportPipe> destinations;
            if (sourceTeleport.canSend()) {
                destinations = manager.getConnectedPipes(sourceTeleport, false, true);
            } else if (sourceTeleport.canReceive()) {
                destinations = manager.getConnectedPipes(sourceTeleport, true, false);
            } else {
                continue;
            }

            for (ITeleportPipe dest : destinations) {
                TilePipeHolder remoteHolder = dest.getContainer();
                if (remoteHolder == null || remoteHolder.isInvalid()) continue;

                World remoteWorld = remoteHolder.getWorld();
                if (remoteWorld == null) continue;
                // Only expose LP routing pipes, not ordinary BC pipes or other tiles.
                // ConnectionInformation args: provider, connectionTypes, incomingSide (the side
                // the LP pipe sees this connection arrive on = exitSide.getOpposite()), the
                // face on the *origin* LP pipe where the teleport is attached, and edge weight.
                // Weight 1.0 treats the teleport hop as one pipe segment; raise it to bias
                // routing away from teleports when cheaper physical paths exist.
                for (EnumFacing exitSide : EnumFacing.VALUES) {
                    BlockPos neighborPos = remoteHolder.getPos().offset(exitSide);
                    TileEntity neighbor = remoteWorld.getTileEntity(neighborPos);
                    if (neighbor == null) continue;

                    IPipeInformationProvider provider = SimpleServiceLocator.pipeInformationManager
                            .getInformationProviderFor(neighbor);
                    if (provider != null && provider.isRoutingPipe()) {
                        result.add(new ConnectionInformation(
                                provider, connection, exitSide.getOpposite(), face, TELEPORT_EDGE_WEIGHT));
                    }
                }
            }
        }

        return result;
    }

    // ISpecialTileConnection overload: LP calls this to decide whether to delegate tile
    // connections to getConnections(TileEntity). Returns true if the tile IS a teleport
    // pipe (not an LP pipe adjacent to one, as in the IPipeInformationProvider overload).
    @Override
    public boolean isType(TileEntity tile) {
        return isTeleportPipe(tile);
    }

    // ISpecialTileConnection path: LP calls this when it needs to discover LP tiles reachable
    // from a teleport pipe tile. Returns LogisticsTileGenericPipe neighbours at the remote
    // end(s) so LP can fold them into its tile-neighbour graph without needing a physical edge.
    @Override
    public Collection<TileEntity> getConnections(TileEntity tile) {
        if (!isTeleportPipe(tile)) return Collections.emptyList();
        ITeleportPipe sourceTeleport = getTeleportPipe(tile);

        TeleportManagerBase manager = TeleportManagerBase.INSTANCE;
        if (manager == null) return Collections.emptyList();

        List<ITeleportPipe> destinations;
        if (sourceTeleport.canSend()) {
            destinations = manager.getConnectedPipes(sourceTeleport, false, true);
        } else if (sourceTeleport.canReceive()) {
            destinations = manager.getConnectedPipes(sourceTeleport, true, false);
        } else {
            return Collections.emptyList();
        }

        List<TileEntity> result = new ArrayList<>();
        for (ITeleportPipe dest : destinations) {
            TilePipeHolder remoteHolder = dest.getContainer();
            if (remoteHolder == null || remoteHolder.isInvalid()) continue;
            World remoteWorld = remoteHolder.getWorld();
            if (remoteWorld == null) continue;
            BlockPos remotePos = remoteHolder.getPos();
            for (EnumFacing face : EnumFacing.VALUES) {
                TileEntity neighbor = remoteWorld.getTileEntity(remotePos.offset(face));
                if (neighbor instanceof LogisticsTileGenericPipe) {
                    result.add(neighbor);
                }
            }
        }
        return result;
    }

    // Returning true tells LP that items routed through this special connection must pass
    // routing information to the receiving pipe via transmit(). Without this, LP would
    // hand off the physical item but lose the routing metadata (destination, priority, etc.),
    // causing items to be stuck or mis-routed at the remote end.
    @Override
    public boolean needsInformationTransition() {
        return true;
    }

    // Called by LP after physical item delivery to copy routing metadata from the outgoing
    // IRoutedItem onto the receiving CoreRoutedPipe via queueUnroutedItemInformation().
    // `tile` is the remote TilePipeHolder (the AP teleport pipe at the destination end);
    // we scan its neighbours for the LP pipe that will actually process the item.
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
            if (!(unrouted instanceof CoreRoutedPipe)) continue;
            // Copy-construct ItemIdentifierStack so the queued entry is independent of the
            // IRoutedItem's mutable stack; queueUnroutedItemInformation stores a reference.
            ((CoreRoutedPipe) unrouted).queueUnroutedItemInformation(
                    new ItemIdentifierStack(data.getItemIdentifierStack()),
                    data.getInfo());
            return; // Stop at the first routed LP pipe found; a teleport pipe has at most one.
        }
    }

    // LogisticsTileGenericPipe declares IPipeInformationProvider but does not implement
    // its getWorld() — calling it directly throws AbstractMethodError. Resolve the world
    // through the underlying TileEntity instead, which always implements MC's getWorld().
    private World getWorld(IPipeInformationProvider startPipe) {
        TileEntity tile = startPipe.getTile();
        return tile != null ? tile.getWorld() : null;
    }

    // Delegates to getTeleportPipe() so the null check is defined in one place.
    private boolean isTeleportPipe(TileEntity tile) {
        return getTeleportPipe(tile) != null;
    }

    // AP teleport pipes are BCR PipeBehaviour objects, not TileEntities: the TileEntity is the
    // BCR generic TilePipeHolder and the actual pipe logic lives in its PipeBehaviour. Both
    // ITeleportPipe and ISpecialPipedConnection expect a TileEntity, so we unwrap through the
    // holder → IPipe → PipeBehaviour chain to reach the AP-specific ITeleportPipe interface.
    private ITeleportPipe getTeleportPipe(TileEntity tile) {
        if (!(tile instanceof TilePipeHolder)) return null;
        IPipe pipe = ((TilePipeHolder) tile).getPipe();
        if (pipe == null) return null;
        PipeBehaviour behaviour = pipe.getBehaviour();
        return behaviour instanceof ITeleportPipe ? (ITeleportPipe) behaviour : null;
    }
}
