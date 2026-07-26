package com.github.tier940.legacycraft.mixins.logisticspipes;

import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import network.rs485.logisticspipes.module.AsyncExtractorModule;
import network.rs485.logisticspipes.module.PipeServiceProviderUtilKt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.modules.LogisticsModule;

@Mixin(value = AsyncExtractorModule.class, remap = false)
public abstract class MixinExtractorInventoryCache extends LogisticsModule {

    @Unique
    private long lc$cachedTick = -1;

    @Unique
    private IInventoryUtil lc$cachedInventory;

    @Shadow
    public abstract EnumFacing getSneakyDirection();

    /**
     * @author LC-Core
     * @reason Cache connected inventory per tick to avoid rebuilding list every call
     */
    @Overwrite
    private IInventoryUtil getConnectedInventory() {
        IPipeServiceProvider service = this._service;
        if (service == null) return null;

        World world = getWorld();
        long tick = world != null ? world.getTotalWorldTime() : -1;

        if (tick != lc$cachedTick) {
            lc$cachedTick = tick;
            java.util.List<IInventoryUtil> list = PipeServiceProviderUtilKt.availableSneakyInventories(service,
                    getSneakyDirection());
            lc$cachedInventory = (list != null && !list.isEmpty()) ? list.get(0) : null;
        }

        return lc$cachedInventory;
    }
}
