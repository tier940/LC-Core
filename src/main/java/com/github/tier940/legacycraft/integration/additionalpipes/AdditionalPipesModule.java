package com.github.tier940.legacycraft.integration.additionalpipes;

import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.github.tier940.legacycraft.api.ModValues;
import com.github.tier940.legacycraft.api.modules.TModule;
import com.github.tier940.legacycraft.api.util.Mods;
import com.github.tier940.legacycraft.integration.LCIntegrationModule;
import com.github.tier940.legacycraft.integration.LCIntegrationSubmodule;
import com.github.tier940.legacycraft.modules.Modules;

/**
 * Owns AP-specific lifecycle work: the Power Teleport Pipe completion, and the item/fluid
 * teleport pipe behavior swap. Only loads when Additional Pipes is present.
 */
@TModule(
         moduleID = Modules.MODULE_ADDITIONAL_PIPES,
         containerID = ModValues.MODID,
         name = "LegacyCraft Additional Pipes Integration",
         description = "Power Teleport Pipe completion and item/fluid teleport pipe behavior fixes.",
         modDependencies = { Mods.Names.ADDITIONAL_PIPES })
public class AdditionalPipesModule extends LCIntegrationSubmodule {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        PowerTeleportPipeFix.preInit(event, LCIntegrationModule.logger);
        TeleportPipeBehaviorFix.apply(LCIntegrationModule.logger);
    }

    @Override
    public void registerItems(RegistryEvent.Register<Item> event) {
        PowerTeleportPipeFix.registerItems(event, LCIntegrationModule.logger);
    }

    @Override
    public void registerRecipesNormal(RegistryEvent.Register<IRecipe> event) {
        PowerTeleportPipeFix.registerRecipes(event, LCIntegrationModule.logger);
    }
}
