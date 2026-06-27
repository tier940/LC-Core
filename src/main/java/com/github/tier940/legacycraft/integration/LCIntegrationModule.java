package com.github.tier940.legacycraft.integration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.github.tier940.legacycraft.api.ModValues;
import com.github.tier940.legacycraft.api.modules.IModule;
import com.github.tier940.legacycraft.api.modules.TModule;
import com.github.tier940.legacycraft.modules.BaseModule;
import com.github.tier940.legacycraft.modules.Modules;

/**
 * Umbrella module that gates all integration submodules. Disabling this module disables every
 * integration submodule that declares it as a dependency.
 */
@TModule(
         moduleID = Modules.MODULE_INTEGRATION,
         containerID = ModValues.MODID,
         name = "LegacyCraft Integration",
         description = "General LegacyCraft Integration Module. Disabling this disables all integration submodules.")
public class LCIntegrationModule extends BaseModule implements IModule {

    public static final Logger logger = LogManager.getLogger("LegacyCraft Integration");

    @NotNull
    @Override
    public Logger getLogger() {
        return logger;
    }
}
