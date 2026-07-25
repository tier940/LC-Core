package com.github.tier940.legacycraft.integration.logisticspipes.spec.modules;

import java.lang.ref.WeakReference;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import network.rs485.logisticspipes.connection.NeighborTileEntity;

import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.logistics.LogisticsManager;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.LogisticsItemOrder;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.utils.CacheHolder;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ModuleCrafterMk2 extends ModuleCrafter {

    public static String getName() {
        return "crafter_mk2";
    }

    @Override
    public String getLPName() {
        return getName();
    }

    @Override
    protected int neededEnergy() {
        return 15;
    }

    @Override
    protected int itemsToExtract() {
        return 64;
    }

    @Override
    protected int stacksToExtract() {
        return 1;
    }

    @Override
    public void enabledUpdateEntity() {
        IPipeServiceProvider service = this._service;
        if (service == null) return;

        if (service.getItemOrderManager().hasOrders(
                IOrderInfoProvider.ResourceType.CRAFTING, IOrderInfoProvider.ResourceType.EXTRA)) {
            if (service.isNthTick(6)) {
                this.cacheAreAllOrderesToBuffer();
            }
            if (service.getItemOrderManager().isFirstOrderWatched()) {
                TileEntity tile = getLastAccessedCrafter();
                if (tile != null) {
                    service.getItemOrderManager().setMachineProgress(
                            SimpleServiceLocator.machineProgressProvider.getProgressForTile(tile));
                } else {
                    service.getItemOrderManager().setMachineProgress((byte) 0);
                }
            }
        } else {
            setCachedAreAllOrderesToBuffer(false);
        }

        if (!service.isNthTick(6)) return;

        List<NeighborTileEntity<TileEntity>> adjacentInventories = service.getAvailableAdjacent().inventories();

        if (!service.getItemOrderManager().hasOrders(
                IOrderInfoProvider.ResourceType.CRAFTING, IOrderInfoProvider.ResourceType.EXTRA)) {
            ISlotUpgradeManager upgradeManager = this.getUpgradeManager();
            if (upgradeManager.getCrafterCleanup() > 0) {
                adjacentInventories.stream()
                        .map(neighbor -> extractFilteredForCleanup(neighbor, upgradeManager))
                        .filter(stack -> !stack.isEmpty())
                        .findFirst()
                        .ifPresent(extracted -> {
                            service.queueRoutedItem(
                                    SimpleServiceLocator.routedItemHelper.createNewTravelItem(extracted),
                                    EnumFacing.UP);
                            service.getCacheHolder().trigger(CacheHolder.CacheTypes.Inventory);
                        });
            }
            return;
        }

        if (adjacentInventories.isEmpty()) {
            if (service.getItemOrderManager().hasOrders(
                    IOrderInfoProvider.ResourceType.CRAFTING, IOrderInfoProvider.ResourceType.EXTRA)) {
                service.getItemOrderManager().sendFailed();
            }
            return;
        }

        List<ItemIdentifierStack> wantedItem = this.getCraftedItems();
        if (wantedItem == null || wantedItem.isEmpty()) return;

        service.spawnParticle(Particles.VioletParticle, 2);
        int itemsLeft = this.itemsToExtract();
        int stacksLeft = this.stacksToExtract();

        block0:
        while (itemsLeft > 0 && stacksLeft > 0 && service.getItemOrderManager()
                .hasOrders(IOrderInfoProvider.ResourceType.CRAFTING, IOrderInfoProvider.ResourceType.EXTRA)) {
            LogisticsItemOrder nextOrder = service.getItemOrderManager()
                    .peekAtTopRequest(IOrderInfoProvider.ResourceType.CRAFTING,
                            IOrderInfoProvider.ResourceType.EXTRA);

            int maxToSend = Math.min(itemsLeft, nextOrder.getResource().stack.getStackSize());
            maxToSend = Math.min(nextOrder.getResource().getItem().getMaxStackSize(), maxToSend);

            ItemStack extracted = ItemStack.EMPTY;
            NeighborTileEntity<TileEntity> adjacent = null;
            for (NeighborTileEntity<TileEntity> candidate : adjacentInventories) {
                adjacent = candidate;
                extracted = this.extract(adjacent, nextOrder.getResource(), maxToSend);
                if (!extracted.isEmpty()) break;
            }

            if (extracted.isEmpty()) {
                service.getItemOrderManager().deferSend();
                break;
            }

            service.getCacheHolder().trigger(CacheHolder.CacheTypes.Inventory);
            setLastAccessedCrafter(adjacent.getTileEntity());
            ItemIdentifier extractedID = ItemIdentifier.get(extracted);

            while (!extracted.isEmpty()) {
                if (this.isExtractedMismatch(nextOrder, extractedID)) {
                    LogisticsItemOrder startOrder = nextOrder;
                    if (service.getItemOrderManager().hasOrders(
                            IOrderInfoProvider.ResourceType.CRAFTING, IOrderInfoProvider.ResourceType.EXTRA)) {
                        do {
                            service.getItemOrderManager().deferSend();
                            nextOrder = (LogisticsItemOrder) service.getItemOrderManager()
                                    .peekAtTopRequest(IOrderInfoProvider.ResourceType.CRAFTING,
                                            IOrderInfoProvider.ResourceType.EXTRA);
                        } while (this.isExtractedMismatch(nextOrder, extractedID) && startOrder != nextOrder);
                    }
                    if (startOrder == nextOrder) {
                        int numToSend = Math.min(extracted.getCount(), extractedID.getMaxStackSize());
                        if (numToSend == 0) continue block0;
                        --stacksLeft;
                        itemsLeft -= numToSend;
                        ItemStack stackToSend = extracted.splitStack(numToSend);
                        service.sendStack(stackToSend, -1, CoreRoutedPipe.ItemSendMode.Normal, null,
                                adjacent.getDirection());
                        continue;
                    }
                }

                int numToSend = Math.min(extracted.getCount(), extractedID.getMaxStackSize());
                numToSend = Math.min(numToSend, nextOrder.getResource().stack.getStackSize());
                if (numToSend == 0) continue block0;

                --stacksLeft;
                itemsLeft -= numToSend;
                ItemStack stackToSend = extracted.splitStack(numToSend);

                if (nextOrder.getDestination() != null) {
                    boolean deferSend = false;
                    SinkReply reply = LogisticsManager.canSink(stackToSend,
                            nextOrder.getDestination().getRouter(), null, true,
                            extractedID, null, true, false);
                    if (reply != null) {
                        deferSend = reply.bufferMode != SinkReply.BufferMode.NONE || reply.maxNumberOfItems < 1;
                    }
                    LPTravelingItem.LPTravelingItemServer item = SimpleServiceLocator.routedItemHelper
                            .createNewTravelItem(stackToSend);
                    item.setDestination(nextOrder.getDestination().getRouter().getSimpleID());
                    item.setTransportMode(IRoutedItem.TransportMode.Active);
                    item.setAdditionalTargetInformation(nextOrder.getInformation());
                    service.queueRoutedItem(item, adjacent.getDirection());
                    service.getItemOrderManager().sendSuccessfull(stackToSend.getCount(), deferSend, item);
                } else {
                    service.sendStack(stackToSend, -1, CoreRoutedPipe.ItemSendMode.Normal,
                            nextOrder.getInformation(), adjacent.getDirection());
                    service.getItemOrderManager().sendSuccessfull(stackToSend.getCount(), false, null);
                }

                if (!service.getItemOrderManager().hasOrders(
                        IOrderInfoProvider.ResourceType.CRAFTING, IOrderInfoProvider.ResourceType.EXTRA)) {
                    continue;
                }
                nextOrder = (LogisticsItemOrder) service.getItemOrderManager()
                        .peekAtTopRequest(IOrderInfoProvider.ResourceType.CRAFTING,
                                IOrderInfoProvider.ResourceType.EXTRA);
            }
        }
    }

    private TileEntity getLastAccessedCrafter() {
        try {
            java.lang.reflect.Field f = ModuleCrafter.class.getDeclaredField("lastAccessedCrafter");
            f.setAccessible(true);
            WeakReference<?> ref = (WeakReference<?>) f.get(this);
            return ref != null ? (TileEntity) ref.get() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void setLastAccessedCrafter(TileEntity tile) {
        try {
            java.lang.reflect.Field f = ModuleCrafter.class.getDeclaredField("lastAccessedCrafter");
            f.setAccessible(true);
            f.set(this, new WeakReference<>(tile));
        } catch (Exception ignored) {}
    }

    private void setCachedAreAllOrderesToBuffer(boolean value) {
        try {
            java.lang.reflect.Field f = ModuleCrafter.class.getDeclaredField("cachedAreAllOrderesToBuffer");
            f.setAccessible(true);
            f.setBoolean(this, value);
        } catch (Exception ignored) {}
    }

    private ItemStack extract(NeighborTileEntity<TileEntity> adjacent,
                              logisticspipes.request.resources.IResource item, int amount) {
        try {
            java.lang.reflect.Method m = ModuleCrafter.class.getDeclaredMethod(
                    "extract", NeighborTileEntity.class,
                    logisticspipes.request.resources.IResource.class, int.class);
            m.setAccessible(true);
            return (ItemStack) m.invoke(this, adjacent, item, amount);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private ItemStack extractFilteredForCleanup(NeighborTileEntity<TileEntity> neighbor,
                                                ISlotUpgradeManager upgradeManager) {
        try {
            java.lang.reflect.Method m = ModuleCrafter.class.getDeclaredMethod(
                    "extractFiltered", NeighborTileEntity.class,
                    network.rs485.logisticspipes.inventory.IItemIdentifierInventory.class,
                    boolean.class, int.class);
            m.setAccessible(true);
            return (ItemStack) m.invoke(this, neighbor, this.cleanupInventory,
                    this.cleanupModeIsExclude.getValue(), upgradeManager.getCrafterCleanup() * 3);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private boolean isExtractedMismatch(LogisticsItemOrder nextOrder, ItemIdentifier extractedID) {
        try {
            java.lang.reflect.Method m = ModuleCrafter.class.getDeclaredMethod(
                    "isExtractedMismatch", LogisticsItemOrder.class, ItemIdentifier.class);
            m.setAccessible(true);
            return (boolean) m.invoke(this, nextOrder, extractedID);
        } catch (Exception e) {
            return false;
        }
    }
}
