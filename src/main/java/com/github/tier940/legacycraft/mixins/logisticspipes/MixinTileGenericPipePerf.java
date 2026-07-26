package com.github.tier940.legacycraft.mixins.logisticspipes;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StackTraceUtil;

@Mixin(value = LogisticsTileGenericPipe.class, remap = false)
public class MixinTileGenericPipePerf {

    @Unique
    private static final StackTraceUtil.Info LC$DUMMY_INFO;

    static {
        StackTraceUtil.Info dummy = null;
        try {
            java.lang.reflect.Constructor<?> c = Class.forName("logisticspipes.utils.StackTraceUtil$DummyInfo")
                    .getDeclaredConstructor(Class.forName("logisticspipes.utils.StackTraceUtil$1"));
            c.setAccessible(true);
            dummy = (StackTraceUtil.Info) c.newInstance((Object) null);
        } catch (Exception ignored) {}
        LC$DUMMY_INFO = dummy;
    }

    @Redirect(method = "update",
              at = @At(value = "INVOKE",
                       target = "Llogisticspipes/utils/StackTraceUtil;addSuperTraceInformation(Ljava/util/function/Supplier;[Llogisticspipes/utils/StackTraceUtil$Info;)Llogisticspipes/utils/StackTraceUtil$Info;"))
    private StackTraceUtil.Info lc$skipSuperTrace(Supplier<String> supplier, StackTraceUtil.Info[] parents) {
        if (LC$DUMMY_INFO != null && !logisticspipes.LogisticsPipes.isDEBUG()) return LC$DUMMY_INFO;
        return StackTraceUtil.addSuperTraceInformation(supplier, parents);
    }

    @Redirect(method = "update",
              at = @At(value = "INVOKE",
                       target = "Llogisticspipes/utils/StackTraceUtil;addTraceInformation(Ljava/util/function/Supplier;[Llogisticspipes/utils/StackTraceUtil$Info;)Llogisticspipes/utils/StackTraceUtil$Info;"))
    private StackTraceUtil.Info lc$skipTrace(Supplier<String> supplier, StackTraceUtil.Info[] parents) {
        if (LC$DUMMY_INFO != null && !logisticspipes.LogisticsPipes.isDEBUG()) return LC$DUMMY_INFO;
        return StackTraceUtil.addTraceInformation(supplier, parents);
    }
}
