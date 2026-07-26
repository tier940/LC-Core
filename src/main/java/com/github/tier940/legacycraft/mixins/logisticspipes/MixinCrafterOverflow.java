package com.github.tier940.legacycraft.mixins.logisticspipes;

import java.lang.ref.WeakReference;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.inventory.IItemIdentifierInventory;
import network.rs485.logisticspipes.property.BooleanProperty;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.logistics.LogisticsManager;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.LogisticsItemOrder;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.utils.CacheHolder;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

@Mixin(value = ModuleCrafter.class, remap = false)
public abstract class MixinCrafterOverflow extends LogisticsModule {

    @Shadow
    private WeakReference<TileEntity> lastAccessedCrafter;

    @Shadow
    private boolean cachedAreAllOrderesToBuffer;

    @Final
    @Shadow
    public ItemIdentifierInventoryProperty cleanupInventory;

    @Final
    @Shadow
    public BooleanProperty cleanupModeIsExclude;

    @Shadow
    public abstract void cacheAreAllOrderesToBuffer();

    @Shadow
    public abstract List<ItemIdentifierStack> getCraftedItems();

    @Shadow
    protected abstract int itemsToExtract();

    @Shadow
    protected abstract int stacksToExtract();

    @Shadow
    private ItemStack extract(NeighborTileEntity<TileEntity> adjacent, IResource item, int amount) {
        throw new AssertionError();
    }

    @Shadow
    private ItemStack extractFiltered(NeighborTileEntity<TileEntity> neighbor,
                                      IItemIdentifierInventory inv, boolean isExcluded, int filterInvLimit) {
        throw new AssertionError();
    }

    @Shadow
    private boolean isExtractedMismatch(LogisticsItemOrder nextOrder, ItemIdentifier extractedID) {
        throw new AssertionError();
    }

    /**
     * @author LC-Core
     * @reason Fix item loss: never skip sending after extraction
     */
    @Overwrite
    public void enabledUpdateEntity() {
        IPipeServiceProvider service = this._service;
        if (service == null) return;

        if (service.getItemOrderManager().hasOrders(
                IOrderInfoProvider.ResourceType.CRAFTING, IOrderInfoProvider.ResourceType.EXTRA)) {
            if (service.isNthTick(6)) {
                this.cacheAreAllOrderesToBuffer();
            }
            if (service.getItemOrderManager().isFirstOrderWatched()) {
                TileEntity tile = this.lastAccessedCrafter.get();
                if (tile != null) {
                    service.getItemOrderManager().setMachineProgress(
                            SimpleServiceLocator.machineProgressProvider.getProgressForTile(tile));
                } else {
                    service.getItemOrderManager().setMachineProgress((byte) 0);
                }
            }
        } else {
            this.cachedAreAllOrderesToBuffer = false;
        }

        if (!service.isNthTick(6)) return;

        List<NeighborTileEntity<TileEntity>> adjacentInventories = service.getAvailableAdjacent().inventories();

        if (!service.getItemOrderManager().hasOrders(
                IOrderInfoProvider.ResourceType.CRAFTING, IOrderInfoProvider.ResourceType.EXTRA)) {
            ISlotUpgradeManager upgradeManager = this.getUpgradeManager();
            if (upgradeManager.getCrafterCleanup() > 0) {
                adjacentInventories.stream()
                        .map(neighbor -> this.extractFiltered(neighbor, this.cleanupInventory,
                                this.cleanupModeIsExclude.getValue(), upgradeManager.getCrafterCleanup() * 3))
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
            this.lastAccessedCrafter = new WeakReference<>(adjacent.getTileEntity());
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
}
