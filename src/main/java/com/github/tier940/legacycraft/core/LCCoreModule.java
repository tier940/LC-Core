package com.github.tier940.legacycraft.core;

import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.github.tier940.legacycraft.Tags;
import com.github.tier940.legacycraft.api.ModValues;
import com.github.tier940.legacycraft.api.modules.IModule;
import com.github.tier940.legacycraft.api.modules.TModule;
import com.github.tier940.legacycraft.common.CommonProxy;
import com.github.tier940.legacycraft.modules.Modules;

/**
 * Core module skeleton: owns the sided proxy and the shared logger. Mod-specific integration work
 * lives under {@code integration/} as submodules of
 * {@link com.github.tier940.legacycraft.integration.LCIntegrationModule}.
 */
@TModule(
         moduleID = Modules.MODULE_CORE,
         containerID = ModValues.MODID,
         name = "LegacyCraftMod Core",
         description = "Core of LegacyCraftMod",
         coreModule = true)
public class LCCoreModule implements IModule {

    private static final String LOGGER_NAME = Tags.MODNAME + " Core";
    public static final Logger logger = LogManager.getLogger(LOGGER_NAME);

    // FML's @SidedProxy injects ClientProxy on the client JVM and CommonProxy on the server JVM
    // at mod construction time. The field must be public static non-final for FML reflection.
    @SidedProxy(modId = ModValues.MODID,
                clientSide = "com.github.tier940.legacycraft.client.ClientProxy",
                serverSide = "com.github.tier940.legacycraft.common.CommonProxy")
    public static CommonProxy proxy;

    @Override
    public @NotNull Logger getLogger() {
        return logger;
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        // proxy.preInit first: sets up client/server-specific resource managers that item
        // and pipe definition registration may depend on (e.g. texture atlases on the client).
        proxy.preInit(event);
    }
}
