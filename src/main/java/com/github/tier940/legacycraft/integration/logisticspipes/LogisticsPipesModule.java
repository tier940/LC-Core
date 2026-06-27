package com.github.tier940.legacycraft.integration.logisticspipes;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

import com.github.tier940.legacycraft.api.ModValues;
import com.github.tier940.legacycraft.api.modules.TModule;
import com.github.tier940.legacycraft.api.util.Mods;
import com.github.tier940.legacycraft.integration.LCIntegrationModule;
import com.github.tier940.legacycraft.integration.LCIntegrationSubmodule;
import com.github.tier940.legacycraft.modules.Modules;

import logisticspipes.proxy.SimpleServiceLocator;

/**
 * Bridges Additional Pipes teleport pipes into the Logistics Pipes routing graph and keeps the
 * routing cache primed on placement. Only loads when both Logistics Pipes and Additional Pipes
 * are present.
 */
@TModule(
         moduleID = Modules.MODULE_LOGISTICS_PIPES,
         containerID = ModValues.MODID,
         name = "LegacyCraft Logistics Pipes Integration",
         description = "Routes Logistics Pipes traffic across Additional Pipes teleport pipes.",
         modDependencies = { Mods.Names.LOGISTICS_PIPES, Mods.Names.ADDITIONAL_PIPES })
public class LogisticsPipesModule extends LCIntegrationSubmodule {

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        registerAdditionalPipesTeleportConnection();
        MinecraftForge.EVENT_BUS.register(new TeleportPipeConnectionNotifier());
    }

    private void registerAdditionalPipesTeleportConnection() {
        if (SimpleServiceLocator.specialpipeconnection == null) {
            LCIntegrationModule.logger.warn(
                    "LP specialpipeconnection is null — skipping AP teleport pipe registration");
            return;
        }
        AdditionalPipesTeleportConnection handler = new AdditionalPipesTeleportConnection();
        // handler.init() returns false when AP failed its own init — safe to skip entirely.
        if (!handler.init()) {
            LCIntegrationModule.logger.info(
                    "Additional Pipes not detected — skipping teleport pipe connection handler");
            return;
        }
        // ISpecialPipedConnection only; also registering ISpecialTileConnection double-counts routes.
        SimpleServiceLocator.specialpipeconnection.registerHandler(handler);
        LCIntegrationModule.logger.info(
                "Registered Additional Pipes teleport connection handler (fix for RS485/LogisticsPipes#348)");
    }
}
