package com.github.tier940.legacycraft.mixins.logisticspipes;

import network.rs485.logisticspipes.module.AsyncExtractorModule;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.modules.LogisticsModule;

@Mixin(value = AsyncExtractorModule.class, remap = false)
public abstract class MixinExtractorBuff extends LogisticsModule {

    /**
     * @author LegacyCraft
     * @reason Buff Extractor: base interval 80 → 20 ticks
     */
    @Overwrite
    public int getEveryNthTick() {
        int actionSpeedUpgrade = getUpgradeManager().getActionSpeedUpgrade();
        return Math.max(1, (int) (20.0 / Math.pow(2.0, actionSpeedUpgrade)));
    }

    /**
     * @author LegacyCraft
     * @reason Buff Extractor: base extraction 1 → 8 items
     */
    @Overwrite
    public final int getItemsToExtract() {
        ISlotUpgradeManager mgr = getUpgradeManager();
        int itemExtraction = mgr.getItemExtractionUpgrade();
        int stackExtraction = mgr.getItemStackExtractionUpgrade();
        return Math.max(1, 8 + 4 * itemExtraction + 64 * stackExtraction);
    }
}
