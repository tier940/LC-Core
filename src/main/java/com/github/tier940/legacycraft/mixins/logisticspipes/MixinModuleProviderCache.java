package com.github.tier940.legacycraft.mixins.logisticspipes;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import network.rs485.logisticspipes.connection.NeighborTileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleProvider;

@Mixin(value = ModuleProvider.class, remap = false)
public abstract class MixinModuleProviderCache extends LogisticsModule {

    @Unique
    private long lcCore_cachedInventoriesTick = -1;

    @Unique
    private List<IInventoryUtil> lcCore_cachedInventories;

    @Shadow
    private IInventoryUtil getInventoryUtilWithMode(NeighborTileEntity<TileEntity> neighbor) {
        throw new AssertionError();
    }

    /**
     * @author LC-Core
     * @reason Cache inventoriesWithMode result per tick to avoid rebuilding Stream pipeline each call
     */
    @Overwrite
    @Nonnull
    public Stream<IInventoryUtil> inventoriesWithMode() {
        IPipeServiceProvider service = this._service;
        if (service == null) return Stream.empty();
        World world = getWorld();
        long tick = world != null ? world.getTotalWorldTime() : -1;
        if (tick != lcCore_cachedInventoriesTick || lcCore_cachedInventories == null) {
            lcCore_cachedInventoriesTick = tick;
            lcCore_cachedInventories = service.getAvailableAdjacent().inventories().stream()
                    .map(this::getInventoryUtilWithMode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return lcCore_cachedInventories.stream();
    }
}
