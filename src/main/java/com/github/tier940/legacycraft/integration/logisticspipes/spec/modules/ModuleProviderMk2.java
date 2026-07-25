package com.github.tier940.legacycraft.integration.logisticspipes.spec.modules;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import network.rs485.logisticspipes.connection.NeighborTileEntity;

import com.github.tier940.legacycraft.api.util.Mods;
import com.github.tier940.legacycraft.integration.logisticspipes.TeleportPipeAdjacency;

import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.modules.ModuleProvider;
import logisticspipes.pipes.basic.CoreRoutedPipe;

public class ModuleProviderMk2 extends ModuleProvider {

    private static final int TELEPORT_BOOST_ITEMS = 4096;
    private static final int TELEPORT_BOOST_STACKS = 64;

    private long cachedInventoriesTick = -1;
    private List<IInventoryUtil> cachedInventories;
    private long cachedAdjacentTick = -1;
    private boolean cachedAdjacent = false;

    public static String getName() {
        return "provider_mk2";
    }

    @Override
    public String getLPName() {
        return getName();
    }

    @Override
    protected int neededEnergy() {
        return 2;
    }

    @Override
    protected int itemsToExtract() {
        int base = 128;
        if (isAdjacentToItemTeleport()) {
            base = Math.max(base, TELEPORT_BOOST_ITEMS);
        }
        return base;
    }

    @Override
    protected int stacksToExtract() {
        int base = 8;
        if (isAdjacentToItemTeleport()) {
            base = Math.max(base, TELEPORT_BOOST_STACKS);
        }
        return base;
    }

    @Override
    public CoreRoutedPipe.ItemSendMode itemSendMode() {
        return CoreRoutedPipe.ItemSendMode.Fast;
    }

    @Nonnull
    @Override
    public Stream<IInventoryUtil> inventoriesWithMode() {
        IPipeServiceProvider service = this._service;
        if (service == null) return Stream.empty();
        World world = getWorld();
        long tick = world != null ? world.getTotalWorldTime() : -1;
        if (tick != cachedInventoriesTick || cachedInventories == null) {
            cachedInventoriesTick = tick;
            cachedInventories = service.getAvailableAdjacent().inventories().stream()
                    .map((NeighborTileEntity<TileEntity> n) -> getInventoryUtilWithMode(n))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return cachedInventories.stream();
    }

    private IInventoryUtil getInventoryUtilWithMode(NeighborTileEntity<TileEntity> neighbor) {
        try {
            java.lang.reflect.Method m = ModuleProvider.class.getDeclaredMethod(
                    "getInventoryUtilWithMode", NeighborTileEntity.class);
            m.setAccessible(true);
            return (IInventoryUtil) m.invoke(this, neighbor);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAdjacentToItemTeleport() {
        if (!Mods.AdditionalPipes.isModLoaded()) return false;
        World world = getWorld();
        if (world == null) return false;
        long tick = world.getTotalWorldTime();
        if (tick != cachedAdjacentTick) {
            cachedAdjacentTick = tick;
            cachedAdjacent = TeleportPipeAdjacency.isAdjacentToItemTeleport(world, getBlockPos());
        }
        return cachedAdjacent;
    }
}
