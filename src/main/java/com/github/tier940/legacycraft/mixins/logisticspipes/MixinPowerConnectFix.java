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

@Mixin(value = PipeTransportLogistics.class, remap = false)
public abstract class MixinPowerConnectFix {

    @Shadow
    protected abstract CoreUnroutedPipe getPipe();

    @Inject(method = "canPipeConnect_internal", at = @At("HEAD"), cancellable = true)
    private void lc$restorePowerConnectivity(
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
