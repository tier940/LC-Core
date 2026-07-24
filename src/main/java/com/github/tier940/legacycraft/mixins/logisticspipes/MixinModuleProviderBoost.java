package com.github.tier940.legacycraft.mixins.logisticspipes;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.tier940.legacycraft.integration.logisticspipes.TeleportPipeAdjacency;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleProvider;

@Mixin(value = ModuleProvider.class, remap = false)
public abstract class MixinModuleProviderBoost extends LogisticsModule {

    @Unique
    private static final int LC_CORE_BOOST_ITEMS = 4096;

    @Unique
    private static final int LC_CORE_BOOST_STACKS = 64;

    @Unique
    private long lcCore_cachedTick = -1;

    @Unique
    private boolean lcCore_cachedAdjacent = false;

    @Inject(method = "itemsToExtract", at = @At("RETURN"), cancellable = true)
    private void lcCore_boostItemsThroughTeleport(CallbackInfoReturnable<Integer> cir) {
        if (lcCore_isAdjacentToItemTeleport()) {
            cir.setReturnValue(Math.max(cir.getReturnValueI(), LC_CORE_BOOST_ITEMS));
        }
    }

    @Inject(method = "stacksToExtract", at = @At("RETURN"), cancellable = true)
    private void lcCore_boostStacksThroughTeleport(CallbackInfoReturnable<Integer> cir) {
        if (lcCore_isAdjacentToItemTeleport()) {
            cir.setReturnValue(Math.max(cir.getReturnValueI(), LC_CORE_BOOST_STACKS));
        }
    }

    @Unique
    private boolean lcCore_isAdjacentToItemTeleport() {
        World world = getWorld();
        if (world == null) return false;
        long tick = world.getTotalWorldTime();
        if (tick != lcCore_cachedTick) {
            lcCore_cachedTick = tick;
            lcCore_cachedAdjacent = TeleportPipeAdjacency.isAdjacentToItemTeleport(world, getBlockPos());
        }
        return lcCore_cachedAdjacent;
    }
}
