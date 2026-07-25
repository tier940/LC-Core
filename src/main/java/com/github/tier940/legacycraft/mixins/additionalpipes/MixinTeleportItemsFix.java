package com.github.tier940.legacycraft.mixins.additionalpipes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import buildcraft.additionalpipes.api.ITeleportPipe;
import buildcraft.additionalpipes.pipes.PipeBehaviorTeleport;
import buildcraft.additionalpipes.pipes.PipeBehaviorTeleportItems;
import buildcraft.additionalpipes.pipes.TeleportManager;
import buildcraft.api.transport.pipe.PipeEventItem;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.tile.TilePipeHolder;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.routing.PipeRoutingConnectionType;

@Mixin(value = PipeBehaviorTeleportItems.class, remap = false)
public abstract class MixinTeleportItemsFix {

    @Unique
    private static final String LC$ROUTING_NBT_KEY = "logisticspipes:routingdata_buildcraft"; // NOSONAR

    @Unique
    private static final double LC$TELEPORTED_ITEM_SPEED = 0.1;

    @Unique
    private static final double LC$DIRECT_NEIGHBOR_DISTANCE = 0.0;

    @Inject(method = "onTryDrop", at = @At("HEAD"), cancellable = true)
    private void lc$destinationAwareTeleport(PipeEventItem.Drop event, CallbackInfo ci) {
        PipeBehaviorTeleport self = (PipeBehaviorTeleport) (Object) this;
        TilePipeHolder holder = self.getContainer();
        if (holder == null) return;
        World world = holder.getWorld();
        if (world.isRemote || !self.canSend() || (self.state & 1) == 0) return;

        ArrayList<ITeleportPipe> candidates = TeleportManager.instance
                .getConnectedPipes(self, false, true);
        if (candidates.isEmpty()) {
            event.setStack(ItemStack.EMPTY);
            ci.cancel();
            return;
        }

        UUID destinationUUID = lc$extractDestinationUUID(event.getStack());
        PipeBehaviorTeleportItems target = lc$selectTarget(candidates, destinationUUID);
        if (target == null) {
            event.setStack(ItemStack.EMPTY);
            ci.cancel();
            return;
        }

        EnumFacing exitSide = target.getTeleportSide();
        if (exitSide == null) {
            event.setStack(ItemStack.EMPTY);
            ci.cancel();
            return;
        }

        ((PipeFlowItems) target.pipe.getFlow())
                .insertItemsForce(event.getStack(), exitSide, null, LC$TELEPORTED_ITEM_SPEED);
        event.setStack(ItemStack.EMPTY);
        ci.cancel();
    }

    @Unique
    private UUID lc$extractDestinationUUID(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(LC$ROUTING_NBT_KEY)) return null;
        try {
            ItemRoutingInformation info = ItemRoutingInformation
                    .restoreFromNBT(tag.getCompoundTag(LC$ROUTING_NBT_KEY));
            return info != null ? info.destinationUUID : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private PipeBehaviorTeleportItems lc$selectTarget(
                                                      ArrayList<ITeleportPipe> candidates, UUID destinationUUID) {
        int destinationSimpleID = -1;
        if (destinationUUID != null && SimpleServiceLocator.routerManager != null) {
            destinationSimpleID = SimpleServiceLocator.routerManager.getIDforUUID(destinationUUID);
        }

        PipeBehaviorTeleportItems best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        PipeBehaviorTeleportItems fallback = null;

        for (ITeleportPipe candidate : candidates) {
            if (!(candidate instanceof PipeBehaviorTeleportItems items)) continue;
            if (!lc$hasConnectedPipe(items)) continue;
            if (fallback == null) fallback = items;
            if (destinationSimpleID < 0) continue;

            double distance = lc$shortestDistanceToDestination(items, destinationSimpleID, destinationUUID);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = items;
            }
        }

        return best != null ? best : fallback;
    }

    @Unique
    private boolean lc$hasConnectedPipe(PipeBehaviorTeleportItems candidate) {
        for (EnumFacing face : EnumFacing.VALUES) {
            if (candidate.pipe.isConnected(face)) return true;
        }
        return false;
    }

    @Unique
    private double lc$shortestDistanceToDestination(
                                                    PipeBehaviorTeleportItems candidate,
                                                    int destinationSimpleID,
                                                    UUID destinationUUID) {
        TilePipeHolder holder = candidate.getContainer();
        if (holder == null) return Double.POSITIVE_INFINITY;
        World world = holder.getWorld();
        BlockPos pos = holder.getPos();

        double shortest = Double.POSITIVE_INFINITY;
        for (EnumFacing face : EnumFacing.VALUES) {
            TileEntity neighbor = world.getTileEntity(pos.offset(face));
            if (!(neighbor instanceof LogisticsTileGenericPipe lpTile)) continue;
            CoreRoutedPipe routingPipe = lpTile.getRoutingPipe();
            if (routingPipe == null) continue;
            IRouter router = routingPipe.getRouter();

            if (destinationUUID != null && destinationUUID.equals(router.getId()))
                return LC$DIRECT_NEIGHBOR_DISTANCE;

            List<List<ExitRoute>> routeTable = router.getRouteTable();
            if (routeTable == null || destinationSimpleID >= routeTable.size()) continue;
            List<ExitRoute> routes = routeTable.get(destinationSimpleID);
            if (routes == null) continue;
            for (ExitRoute route : routes) {
                if (route.connectionDetails != null &&
                        route.connectionDetails.contains(PipeRoutingConnectionType.canRouteTo) &&
                        route.distanceToDestination < shortest) {
                    shortest = route.distanceToDestination;
                }
            }
        }
        return shortest;
    }
}
