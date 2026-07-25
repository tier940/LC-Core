package com.github.tier940.legacycraft.mixins.logisticspipes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import logisticspipes.pipes.basic.debug.DebugLogController;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.transport.PipeTransportLogistics;

@Mixin(value = PipeTransportLogistics.class, remap = false)
public abstract class MixinInjectItemPerf {

    @Redirect(
              method = "injectItem(Llogisticspipes/transport/LPTravelingItem;Lnet/minecraft/util/EnumFacing;)I",
              at = @At(value = "INVOKE",
                       target = "Llogisticspipes/routing/ItemRoutingInformation;clone()Llogisticspipes/routing/ItemRoutingInformation;"))
    private ItemRoutingInformation lc$skipInfoClone(ItemRoutingInformation info) {
        return info;
    }

    @Redirect(
              method = "injectItem(Llogisticspipes/transport/LPTravelingItem;Lnet/minecraft/util/EnumFacing;)I",
              at = @At(value = "INVOKE",
                       target = "Llogisticspipes/pipes/basic/debug/DebugLogController;log(Ljava/lang/String;)V"))
    private void lc$skipDebugLog(DebugLogController controller, String info) {}
}
