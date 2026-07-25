package com.github.tier940.legacycraft.integration.logisticspipes.spec.modules;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import network.rs485.logisticspipes.connection.NeighborTileEntity;

import logisticspipes.interfaces.IBufferItems;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.utils.CacheHolder;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ModuleCrafterMk3 extends ModuleCrafterMk2 implements IBufferItems {

    private static final int BUFFER_SLOTS = 16;
    private static final int BUFFER_STACK_LIMIT = 127;

    private final ItemStack[] buffer = new ItemStack[BUFFER_SLOTS];

    public ModuleCrafterMk3() {
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            buffer[i] = ItemStack.EMPTY;
        }
    }

    public static String getName() {
        return "crafter_mk3";
    }

    @Override
    public String getLPName() {
        return getName();
    }

    @Override
    protected int neededEnergy() {
        return 20;
    }

    @Override
    protected int itemsToExtract() {
        return 128;
    }

    @Override
    protected int stacksToExtract() {
        return 8;
    }

    @Override
    public int addToBuffer(ItemIdentifierStack stack,
                           logisticspipes.interfaces.routing.IAdditionalTargetInformation info) {
        if (stack == null) return 0;
        ItemStack toInsert = stack.makeNormalStack();
        int remaining = toInsert.getCount();

        for (int i = 0; i < BUFFER_SLOTS && remaining > 0; i++) {
            if (buffer[i].isEmpty()) {
                int toAdd = Math.min(remaining, BUFFER_STACK_LIMIT);
                buffer[i] = toInsert.copy();
                buffer[i].setCount(toAdd);
                remaining -= toAdd;
            } else if (ItemStack.areItemsEqual(buffer[i], toInsert) &&
                    ItemStack.areItemStackTagsEqual(buffer[i], toInsert)) {
                        int space = BUFFER_STACK_LIMIT - buffer[i].getCount();
                        int toAdd = Math.min(remaining, space);
                        if (toAdd > 0) {
                            buffer[i].grow(toAdd);
                            remaining -= toAdd;
                        }
                    }
        }

        return remaining;
    }

    @Override
    public SinkReply sinksItem(ItemStack stack, ItemIdentifier item,
                               int bestPriority, int bestCustomPriority,
                               boolean allowDefault, boolean includeInTransit,
                               boolean forcePassive) {
        SinkReply parentReply = super.sinksItem(stack, item, bestPriority,
                bestCustomPriority, allowDefault, includeInTransit, forcePassive);
        if (parentReply != null) return parentReply;

        int bufferSpace = bufferSpaceFor(stack);
        if (bufferSpace > 0 && _sinkReply != null) {
            if (bestPriority > _sinkReply.fixedPriority.ordinal()) return null;
            if (bestPriority == _sinkReply.fixedPriority.ordinal() && bestCustomPriority >= _sinkReply.customPriority)
                return null;
            return new SinkReply(_sinkReply, bufferSpace, SinkReply.BufferMode.BUFFERED);
        }
        return null;
    }

    private int bufferSpaceFor(ItemStack stack) {
        int space = 0;
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            if (buffer[i].isEmpty()) {
                space += BUFFER_STACK_LIMIT;
            } else if (ItemStack.areItemsEqual(buffer[i], stack) && ItemStack.areItemStackTagsEqual(buffer[i], stack)) {
                space += BUFFER_STACK_LIMIT - buffer[i].getCount();
            }
        }
        return space;
    }

    @Override
    public void tick() {
        super.tick();
        IPipeServiceProvider service = this._service;
        if (service == null) return;
        if (!service.isNthTick(6)) return;

        pushBufferToAdjacentInventories(service);
    }

    private void pushBufferToAdjacentInventories(IPipeServiceProvider service) {
        List<NeighborTileEntity<TileEntity>> inventories = service.getAvailableAdjacent().inventories();
        if (inventories.isEmpty()) return;

        for (int i = 0; i < BUFFER_SLOTS; i++) {
            if (buffer[i].isEmpty()) continue;

            if (service.getItemOrderManager().hasOrders(
                    IOrderInfoProvider.ResourceType.CRAFTING, IOrderInfoProvider.ResourceType.EXTRA)) {
                for (NeighborTileEntity<TileEntity> neighbor : inventories) {
                    ISlotUpgradeManager upgradeManager = this.getUpgradeManager();
                    EnumFacing sneaky = upgradeManager.hasSneakyUpgrade() ? upgradeManager.getSneakyOrientation() :
                            null;
                    EnumFacing insertDir = sneaky != null ? sneaky : neighbor.getDirection();

                    net.minecraftforge.items.IItemHandler handler = neighbor.getTileEntity()
                            .getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY,
                                    insertDir.getOpposite());
                    if (handler == null) continue;

                    ItemStack remaining = net.minecraftforge.items.ItemHandlerHelper
                            .insertItemStacked(handler, buffer[i].copy(), false);
                    if (remaining.isEmpty()) {
                        buffer[i] = ItemStack.EMPTY;
                        service.getCacheHolder().trigger(CacheHolder.CacheTypes.Inventory);
                        break;
                    } else if (remaining.getCount() < buffer[i].getCount()) {
                        buffer[i].setCount(remaining.getCount());
                        service.getCacheHolder().trigger(CacheHolder.CacheTypes.Inventory);
                    }
                }
            } else {
                service.queueRoutedItem(
                        SimpleServiceLocator.routedItemHelper.createNewTravelItem(buffer[i]),
                        EnumFacing.UP);
                buffer[i] = ItemStack.EMPTY;
                service.getCacheHolder().trigger(CacheHolder.CacheTypes.Inventory);
            }
        }
    }

    public void dropBuffer(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos) {
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            if (!buffer[i].isEmpty()) {
                net.minecraft.block.Block.spawnAsEntity(world, pos, buffer[i]);
                buffer[i] = ItemStack.EMPTY;
            }
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (nbt.hasKey("lc_buffer")) {
            NBTTagCompound bufferTag = nbt.getCompoundTag("lc_buffer");
            for (int i = 0; i < BUFFER_SLOTS; i++) {
                if (bufferTag.hasKey("slot_" + i)) {
                    buffer[i] = new ItemStack(bufferTag.getCompoundTag("slot_" + i));
                } else {
                    buffer[i] = ItemStack.EMPTY;
                }
            }
        }
    }

    public void writeToNBT(NBTTagCompound nbt) {
        try {
            java.lang.reflect.Method m = logisticspipes.modules.ModuleCrafter.class
                    .getMethod("writeToNBT", NBTTagCompound.class);
            if (m.getDeclaringClass() != ModuleCrafterMk3.class) {
                m.invoke(this, nbt);
            }
        } catch (Exception ignored) {}

        NBTTagCompound bufferTag = new NBTTagCompound();
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            if (!buffer[i].isEmpty()) {
                bufferTag.setTag("slot_" + i, buffer[i].writeToNBT(new NBTTagCompound()));
            }
        }
        nbt.setTag("lc_buffer", bufferTag);
    }
}
