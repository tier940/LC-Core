package com.github.tier940.legacycraft.mixins.logisticspipes;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import logisticspipes.interfaces.IPipeUpgradeManager;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.PipeTransportLogistics;

@Mixin(value = PipeTransportLogistics.class, remap = false)
public abstract class MixinPipeTransportLogistics {

    @Shadow
    private boolean isRouted;

    @Shadow
    protected abstract CoreUnroutedPipe getPipe();

    @Shadow
    protected abstract CoreRoutedPipe getRoutedPipe();

    @Inject(method = "canPipeConnect_internal", at = @At("HEAD"), cancellable = true)
    private void lcCoreRestorePowerConnectivity(
                                                TileEntity tile, EnumFacing side,
                                                CallbackInfoReturnable<Boolean> cir) {
        IPipeUpgradeManager upgradeManager = getPipe().getUpgradeManager();

        if (upgradeManager.hasRFPowerSupplierUpgrade() &&
                tile.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite())) {
            cir.setReturnValue(true);
            return;
        }

        if (upgradeManager.getIC2PowerLevel() > 0 && SimpleServiceLocator.IC2Proxy.isEnergySink(tile)) {
            cir.setReturnValue(true);
        }
    }

    private static final float SPEED_COEFFICIENT = 0.10f;
    private static final float COST_COEFFICIENT = 0.15f;

    /**
     * @author LegacyCraft
     * @reason Buff Speed Upgrade: 0.02 → 0.10 per upgrade
     */
    @Overwrite
    public void readjustSpeed(LPTravelingItem.LPTravelingItemServer item) {
        float baseSpeed;
        switch (item.getTransportMode()) {
            case Default:
                baseSpeed = 20.0f;
                break;
            case Passive:
                baseSpeed = 25.0f;
                break;
            case Active:
                baseSpeed = 30.0f;
                break;
            default:
                baseSpeed = 20.0f;
                break;
        }

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
