package com.github.tier940.legacycraft.client;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import com.github.tier940.legacycraft.api.util.Mods;
import com.github.tier940.legacycraft.common.CommonProxy;
import com.github.tier940.legacycraft.core.LCCoreModule;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (Loader.isModLoaded(Mods.Names.ADDITIONAL_PIPES)) {
            AdditionalPipesClientHelper.registerModels(event, LCCoreModule.logger);
        }
    }
}
