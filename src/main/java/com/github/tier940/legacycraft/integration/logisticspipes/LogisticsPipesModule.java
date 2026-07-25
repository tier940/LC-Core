package com.github.tier940.legacycraft.integration.logisticspipes;

import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

import com.github.tier940.legacycraft.api.ModValues;
import com.github.tier940.legacycraft.api.modules.TModule;
import com.github.tier940.legacycraft.api.util.Mods;
import com.github.tier940.legacycraft.integration.LCIntegrationModule;
import com.github.tier940.legacycraft.integration.LCIntegrationSubmodule;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleCrafterMk2;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleCrafterMk3;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleProviderMk2;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.pipes.PipeCraftingMk2;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.pipes.PipeCraftingMk3;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.pipes.PipeProviderMk2;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.recipes.Mk2ModuleRecipes;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.recipes.Mk2PipeRecipes;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.recipes.Mk3ModuleRecipes;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.recipes.Mk3PipeRecipes;
import com.github.tier940.legacycraft.modules.Modules;

import logisticspipes.items.ItemModule;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;

@TModule(
         moduleID = Modules.MODULE_LOGISTICS_PIPES,
         containerID = ModValues.MODID,
         name = "LegacyCraft Logistics Pipes Integration",
         description = "Routes Logistics Pipes traffic across Additional Pipes teleport pipes.",
         modDependencies = { Mods.Names.LOGISTICS_PIPES })
public class LogisticsPipesModule extends LCIntegrationSubmodule {

    @Override
    public void registerItems(RegistryEvent.Register<Item> event) {
        ItemModule.registerModule(event.getRegistry(),
                ModuleProviderMk2.getName(), ModuleProviderMk2::new);
        ItemModule.registerModule(event.getRegistry(),
                ModuleCrafterMk2.getName(), ModuleCrafterMk2::new);
        ItemModule.registerModule(event.getRegistry(),
                ModuleCrafterMk3.getName(), ModuleCrafterMk3::new);

        LogisticsBlockGenericPipe.registerPipe(event.getRegistry(),
                "provider_mk2", PipeProviderMk2::new);
        LogisticsBlockGenericPipe.registerPipe(event.getRegistry(),
                "crafting_mk2", PipeCraftingMk2::new);
        LogisticsBlockGenericPipe.registerPipe(event.getRegistry(),
                "crafting_mk3", PipeCraftingMk3::new);

        LCIntegrationModule.logger.info("Registered Mk2/Mk3 modules and pipes");
    }

    @Override
    public void registerRecipesNormal(RegistryEvent.Register<IRecipe> event) {
        Mk2ModuleRecipes.register();
        Mk2PipeRecipes.register();
        Mk3ModuleRecipes.register();
        Mk3PipeRecipes.register();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        if (Mods.AdditionalPipes.isModLoaded()) {
            registerAdditionalPipesTeleportConnection();
            MinecraftForge.EVENT_BUS.register(new TeleportPipeConnectionNotifier());
        }
    }

    private void registerAdditionalPipesTeleportConnection() {
        if (SimpleServiceLocator.specialpipeconnection == null) {
            LCIntegrationModule.logger.warn(
                    "LP specialpipeconnection is null — skipping AP teleport pipe registration");
            return;
        }
        AdditionalPipesTeleportConnection handler = new AdditionalPipesTeleportConnection();
        if (!handler.init()) {
            LCIntegrationModule.logger.info(
                    "Additional Pipes not detected — skipping teleport pipe connection handler");
            return;
        }
        SimpleServiceLocator.specialpipeconnection.registerHandler(handler);
        LCIntegrationModule.logger.info(
                "Registered Additional Pipes teleport connection handler (fix for RS485/LogisticsPipes#348)");
    }
}
