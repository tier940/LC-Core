package com.github.tier940.legacycraft.client;

import net.minecraftforge.client.event.ModelRegistryEvent;

import org.apache.logging.log4j.Logger;

import com.github.tier940.legacycraft.integration.additionalpipes.PowerTeleportPipeFix;

public class AdditionalPipesClientHelper {

    public static void registerModels(ModelRegistryEvent event, Logger logger) {
        PowerTeleportPipeFix.registerModels(event, logger);
    }
}
