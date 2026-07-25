package com.github.tier940.legacycraft.mixins.logisticspipes;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.PipeTransportLogistics;

@Mixin(value = PipeTransportLogistics.class, remap = false)
public abstract class MixinSpeedUpgradeBuff {

    @Final
    @Shadow
    public boolean isRouted;

    @Shadow
    protected abstract CoreRoutedPipe getRoutedPipe();

    @Unique
    private static final float SPEED_COEFFICIENT = 0.10f;

    @Unique
    private static final float COST_COEFFICIENT = 0.15f;

    /**
     * @author LegacyCraft
     * @reason Buff Speed Upgrade: 0.02 → 0.10 per upgrade
     */
    @Overwrite
    public void readjustSpeed(LPTravelingItem.LPTravelingItemServer item) {
        float baseSpeed = switch (item.getTransportMode()) {
            case Passive -> 25.0f;
            case Active -> 30.0f;
            default -> 20.0f;
        };

        if (!isRouted) return;

        int count = getRoutedPipe().getUpgradeManager().getSpeedUpgradeCount();
        float targetSpeed = 1.0f + SPEED_COEFFICIENT * count;
        float targetSpeedForCost = 1.0f + COST_COEFFICIENT * count;

        float wantedSpeed = Math.max(item.getSpeed(), 0.01f * baseSpeed * targetSpeedForCost);
        float delta = wantedSpeed - item.getSpeed();

        if (getRoutedPipe().useEnergy((int) (delta * 50.0f + 0.5f))) {
            float newSpeed = Math.max(item.getSpeed(), 0.01f * baseSpeed * targetSpeed);
            item.setSpeed(Math.min(newSpeed, 1.0f));
        }
    }
}
