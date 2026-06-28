package com.github.tier940.legacycraft;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.github.tier940.legacycraft.api.ModValues;
import com.github.tier940.legacycraft.api.util.ModLog;
import com.github.tier940.legacycraft.api.util.Mods;
import com.github.tier940.legacycraft.modules.ModuleManager;
import com.github.tier940.legacycraft.modules.Modules;

@Mod(
     modid = Tags.MODID,
     name = Tags.MODNAME,
     version = Tags.VERSION,
     updateJSON = "https://forge.curseupdate.com/1582641/legacycraft",
     dependencies = "required-after:" + Mods.Names.MIXINBOOTER + "@[10.6,);" +
             "after:" + Mods.Names.LOGISTICS_PIPES + ";" +
             "after:" + Mods.Names.ADDITIONAL_PIPES + ";")
public class LegacyCraftMod {

    private ModuleManager moduleManager;

    @Mod.EventHandler
    public void onConstruction(FMLConstructionEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ModLog.logger.info("starting construction event...");
        moduleManager = ModuleManager.getInstance();
        moduleManager.registerContainer(new Modules());
        moduleManager.setup(event.getASMHarvestedData(), Loader.instance().getConfigDir());
        moduleManager.onConstruction(event);
        ModLog.logger.info("finished construction!");
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        moduleManager.onPreInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        moduleManager.onInit(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        moduleManager.onPostInit(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        moduleManager.onLoadComplete(event);
    }

    @Mod.EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        moduleManager.onServerAboutToStart(event);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        moduleManager.onServerStarting(event);
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        moduleManager.onServerStarted(event);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        moduleManager.onServerStopping(event);
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        moduleManager.onServerStopped(event);
    }

    @Mod.EventHandler
    public void respondIMC(FMLInterModComms.IMCEvent event) {
        moduleManager.processIMC(event.getMessages());
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        ModLog.logger.info("Registering Blocks...");
        moduleManager.registerBlocks(event);
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        ModLog.logger.info("Registering Items...");

        moduleManager.registerItems(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void registerRecipesHighest(RegistryEvent.Register<IRecipe> event) {
        moduleManager.registerRecipesHighest(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void registerRecipesHigh(RegistryEvent.Register<IRecipe> event) {
        moduleManager.registerRecipesHigh(event);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        moduleManager.registerRecipesNormal(event);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void registerRecipesLow(RegistryEvent.Register<IRecipe> event) {
        moduleManager.registerRecipesLow(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void registerRecipesLowest(RegistryEvent.Register<IRecipe> event) {
        moduleManager.registerRecipesLowest(event);
    }

    @SubscribeEvent
    public static void syncConfigValues(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(ModValues.MODID)) {
            ConfigManager.sync(ModValues.MODID, Config.Type.INSTANCE);
        }
    }
}
