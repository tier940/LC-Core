package com.github.tier940.legacycraft.mixins.logisticspipes;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import logisticspipes.interfaces.IPipeUpgradeManager;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.transport.PipeTransportLogistics;

/**
 * Restores the RF and IC2 power-supplier connectivity checks dropped from LP 0.10.4.28+, so LP
 * pipes connect to Forge Energy receivers and IC2 energy sinks again. Injected at HEAD ahead of
 * the original early-return guards. EnderIO is not restored: LP 0.10.4.49 removed the EnderIO proxy
 * entirely (no field, no interface), so the check has no target.
 */
@Mixin(value = PipeTransportLogistics.class, remap = false)
public abstract class MixinPipeTransportLogistics {

    @Shadow
    protected abstract CoreUnroutedPipe getPipe();

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
}
