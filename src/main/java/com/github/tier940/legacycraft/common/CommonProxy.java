package com.github.tier940.legacycraft.common;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import com.github.tier940.legacycraft.api.ModValues;

@Mod.EventBusSubscriber(modid = ModValues.MODID)
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {}
}
