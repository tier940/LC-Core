package com.github.tier940.legacycraft.mixins.logisticspipes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import logisticspipes.routing.ServerRouter;

@Mixin(value = ServerRouter.class, remap = false)
public abstract class MixinServerRouterPerf {

    @Unique
    private boolean lcCore_lsaDirty = false;

    @Invoker("SendNewLSA")
    abstract void lcCore_callSendNewLSA();

    @Redirect(method = "recheckAdjacent",
              at = @At(value = "INVOKE",
                       target = "Llogisticspipes/routing/ServerRouter;SendNewLSA()V"))
    private void lcCore_deferSendNewLSA(ServerRouter self) {
        lcCore_lsaDirty = true;
    }

    @Inject(method = "lazyUpdateRoutingTable", at = @At("HEAD"))
    private void lcCore_flushLsaOnLazyUpdate(CallbackInfo ci) {
        lcCore_flushDirtyLsa();
    }

    @Inject(method = "ensureLatestRoutingTable", at = @At("HEAD"))
    private void lcCore_flushLsaOnEnsureLatest(CallbackInfo ci) {
        lcCore_flushDirtyLsa();
    }

    @Unique
    private void lcCore_flushDirtyLsa() {
        if (lcCore_lsaDirty) {
            lcCore_lsaDirty = false;
            lcCore_callSendNewLSA();
        }
    }
}
