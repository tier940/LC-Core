package com.github.tier940.legacycraft.core;

import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
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

@TModule(
         moduleID = Modules.MODULE_CORE,
         containerID = ModValues.MODID,
         name = "LegacyCraftMod Core",
         description = "Core of LegacyCraftMod",
         coreModule = true)
public class LCCoreModule implements IModule {

    public static final Logger logger = LogManager.getLogger(Tags.MODNAME + " Core");

    @SidedProxy(modId = ModValues.MODID,
                clientSide = "com.github.tier940.legacycraft.client.ClientProxy",
                serverSide = "com.github.tier940.legacycraft.common.CommonProxy")
    public static CommonProxy proxy;

    @Override
    public @NotNull Logger getLogger() {
        return logger;
    }

    @Override
    public void construction(FMLConstructionEvent event) {}

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }
}
