package com.github.tier940.legacycraft.mixins.logisticspipes;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.tier940.legacycraft.integration.logisticspipes.TeleportPipeAdjacency;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleProvider;

/**
 * Raises ModuleProvider per-tick extraction limits when the provider pipe is adjacent to an item
 * teleport pipe, so a request drains in one tick instead of dribbling out one stack per tick
 * (each piece teleports immediately, making the throttle visible at the destination).
 */
@Mixin(value = ModuleProvider.class, remap = false)
public abstract class MixinModuleProviderBoost extends LogisticsModule {

    private static final int LC_CORE_BOOST_ITEMS = 4096;
    private static final int LC_CORE_BOOST_STACKS = 64;

    private long lcCoreCachedTick = -1;
    private boolean lcCoreCachedAdjacent = false;

    @Inject(method = "itemsToExtract", at = @At("RETURN"), cancellable = true)
    private void lcCoreBoostItemsThroughTeleport(CallbackInfoReturnable<Integer> cir) {
        if (lcCoreIsAdjacentToItemTeleport()) {
            cir.setReturnValue(Math.max(cir.getReturnValueI(), LC_CORE_BOOST_ITEMS));
        }
    }

    @Inject(method = "stacksToExtract", at = @At("RETURN"), cancellable = true)
    private void lcCoreBoostStacksThroughTeleport(CallbackInfoReturnable<Integer> cir) {
        if (lcCoreIsAdjacentToItemTeleport()) {
            cir.setReturnValue(Math.max(cir.getReturnValueI(), LC_CORE_BOOST_STACKS));
        }
    }

    private boolean lcCoreIsAdjacentToItemTeleport() {
        World world = getWorld();
        if (world == null) return false;
        long tick = world.getTotalWorldTime();
        if (tick != lcCoreCachedTick) {
            lcCoreCachedTick = tick;
            lcCoreCachedAdjacent = TeleportPipeAdjacency.isAdjacentToItemTeleport(world, getBlockPos());
        }
        return lcCoreCachedAdjacent;
    }
}
