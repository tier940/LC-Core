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
    private boolean lc$lsaDirty = false;

    @Invoker("SendNewLSA")
    abstract void lc$callSendNewLSA();

    @Redirect(method = "recheckAdjacent",
              at = @At(value = "INVOKE",
                       target = "Llogisticspipes/routing/ServerRouter;SendNewLSA()V"))
    private void lc$deferSendNewLSA(ServerRouter self) {
        lc$lsaDirty = true;
    }

    @Inject(method = "lazyUpdateRoutingTable", at = @At("HEAD"))
    private void lc$flushLsaOnLazyUpdate(CallbackInfo ci) {
        lc$flushDirtyLsa();
    }

    @Inject(method = "ensureLatestRoutingTable", at = @At("HEAD"))
    private void lc$flushLsaOnEnsureLatest(CallbackInfo ci) {
        lc$flushDirtyLsa();
    }

    @Unique
    private void lc$flushDirtyLsa() {
        if (lc$lsaDirty) {
            lc$lsaDirty = false;
            lc$callSendNewLSA();
        }
    }
}
