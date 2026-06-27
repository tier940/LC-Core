package com.github.tier940.legacycraft.integration;

import java.util.Collections;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.github.tier940.legacycraft.api.util.ModUtility;
import com.github.tier940.legacycraft.modules.BaseModule;
import com.github.tier940.legacycraft.modules.Modules;

/**
 * Base class for feature-specific integration submodules. Each submodule declares the integration
 * umbrella as its dependency so it loads only when {@link LCIntegrationModule} is enabled.
 */
public abstract class LCIntegrationSubmodule extends BaseModule {

    private static final Set<ResourceLocation> DEPENDENCY_UID = Collections.singleton(
            ModUtility.id(Modules.MODULE_INTEGRATION));

    @NotNull
    @Override
    public Logger getLogger() {
        return LCIntegrationModule.logger;
    }

    @NotNull
    @Override
    public Set<ResourceLocation> getDependencyUids() {
        return DEPENDENCY_UID;
    }
}
