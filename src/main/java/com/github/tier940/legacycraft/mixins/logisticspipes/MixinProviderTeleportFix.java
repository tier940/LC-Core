package com.github.tier940.legacycraft.mixins.logisticspipes;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.tier940.legacycraft.api.util.Mods;
import com.github.tier940.legacycraft.integration.logisticspipes.TeleportPipeAdjacency;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleProvider;

@Mixin(value = ModuleProvider.class, remap = false)
public abstract class MixinProviderTeleportFix extends LogisticsModule {

    @Unique
    private static final int LC$BOOST_ITEMS = 4096;

    @Unique
    private static final int LC$BOOST_STACKS = 64;

    @Unique
    private long lc$cachedTick = -1;

    @Unique
    private boolean lc$cachedAdjacent = false;

    @Inject(method = "itemsToExtract", at = @At("RETURN"), cancellable = true)
    private void lc$boostItemsThroughTeleport(CallbackInfoReturnable<Integer> cir) {
        if (lc$isAdjacentToItemTeleport()) {
            cir.setReturnValue(Math.max(cir.getReturnValueI(), LC$BOOST_ITEMS));
        }
    }

    @Inject(method = "stacksToExtract", at = @At("RETURN"), cancellable = true)
    private void lc$boostStacksThroughTeleport(CallbackInfoReturnable<Integer> cir) {
        if (lc$isAdjacentToItemTeleport()) {
            cir.setReturnValue(Math.max(cir.getReturnValueI(), LC$BOOST_STACKS));
        }
    }

    @Unique
    private boolean lc$isAdjacentToItemTeleport() {
        if (!Mods.AdditionalPipes.isModLoaded()) return false;
        World world = getWorld();
        if (world == null) return false;
        long tick = world.getTotalWorldTime();
        if (tick != lc$cachedTick) {
            lc$cachedTick = tick;
            lc$cachedAdjacent = TeleportPipeAdjacency.isAdjacentToItemTeleport(world, getBlockPos());
        }
        return lc$cachedAdjacent;
    }
}
