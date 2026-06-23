package com.github.tier940.legacycraft.core;

import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import com.github.tier940.legacycraft.Tags;
import com.github.tier940.legacycraft.api.ModValues;
import com.github.tier940.legacycraft.api.modules.IModule;
import com.github.tier940.legacycraft.api.modules.TModule;
import com.github.tier940.legacycraft.common.CommonProxy;
import com.github.tier940.legacycraft.core.additionalpipes.PowerTeleportPipeFix;
import com.github.tier940.legacycraft.core.additionalpipes.TeleportPipeBehaviorFix;
import com.github.tier940.legacycraft.core.logisticspipes.AdditionalPipesTeleportConnection;
import com.github.tier940.legacycraft.modules.Modules;

import logisticspipes.proxy.SimpleServiceLocator;

/**
 * Core module for LegacyCraft. Applies AP power/item/fluid teleport pipe fixes, and registers
 * the Logistics Pipes teleport connection handler during post-initialization.
 */
@TModule(
         moduleID = Modules.MODULE_CORE,
         containerID = ModValues.MODID,
         name = "LegacyCraftMod Core",
         description = "Core of LegacyCraftMod",
         coreModule = true)
public class LCCoreModule implements IModule {

    // public (not private) so ClientProxy can pass it to PowerTeleportPipeFix.registerModels
    // without needing its own Logger instance for a one-line delegation call.
    private static final String LOGGER_NAME = Tags.MODNAME + " Core";
    public static final Logger logger = LogManager.getLogger(LOGGER_NAME);
    // FML's @SidedProxy injects ClientProxy on the client JVM and CommonProxy on the server JVM
    // at mod construction time. The field must be public static non-final for FML reflection.
    @SidedProxy(modId = ModValues.MODID,
                clientSide = "com.github.tier940.legacycraft.client.ClientProxy",
                serverSide = "com.github.tier940.legacycraft.common.CommonProxy")
    public static CommonProxy proxy;

    // IModule contract: the module framework calls getLogger() to prefix log output with module context.
    @Override
    public @NotNull Logger getLogger() {
        return logger;
    }

    @Override
    public void construction(FMLConstructionEvent event) {} // Nothing to do at construction time for the core module.

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        // proxy.preInit first: sets up client/server-specific resource managers that item
        // and pipe definition registration may depend on (e.g. texture atlases on the client).
        proxy.preInit(event);

        // PowerTeleportPipeFix registers the power pipe definition (powerTeleportPipeDef / Item).
        // It does not write itemsTeleportPipeDef or liquidsTeleportPipeDef, so there is no direct
        // ordering dependency with TeleportPipeBehaviorFix; it simply runs first by convention.
        PowerTeleportPipeFix.preInit(event, logger);

        // AP runs its own preInit before ours (mod dependency order), so itemsTeleportPipeDef
        // and liquidsTeleportPipeDef are already populated and safe to replace here.
        TeleportPipeBehaviorFix.apply(logger);
    }

    @Override
    public void init(FMLInitializationEvent event) {} // Nothing to do at init time for the core module.

    // Delegates to PowerTeleportPipeFix because it holds the pendingItem reference created in preInit;
    // items cannot be registered during preInit, only during the Forge registry event.
    @Override
    public void registerItems(RegistryEvent.Register<Item> event) {
        PowerTeleportPipeFix.registerItems(event, logger);
    }

    // Registers the items-pipe + redstone dust → power-teleport-pipe recipe added by the fix.
    // AP registers its own itemsTeleportPipeItem in the same Forge item registry wave; our recipe
    // references it as an ingredient, so both registrations happen during the same event phase.
    @Override
    public void registerRecipesNormal(RegistryEvent.Register<IRecipe> event) {
        PowerTeleportPipeFix.registerRecipes(event, logger);
    }

    // postInit: all mods have completed init() — the earliest point at which LP's service locator
    // can be queried. Fields such as specialpipeconnection may still be null (guarded below).
    @Override
    public void postInit(FMLPostInitializationEvent event) {
        registerAdditionalPipesTeleportConnection();
    }

    private void registerAdditionalPipesTeleportConnection() {
        if (SimpleServiceLocator.specialpipeconnection == null) {
            logger.warn("LP specialpipeconnection is null — skipping AP teleport pipe registration");
            return;
        }
        AdditionalPipesTeleportConnection handler = new AdditionalPipesTeleportConnection();
        // handler.init() returns false when TeleportManagerBase.INSTANCE is null, which means
        // AP either is not loaded or failed its own initialization — safe to skip entirely.
        if (!handler.init()) {
            logger.info("Additional Pipes not detected — skipping teleport pipe connection handler");
            return;
        }
        SimpleServiceLocator.specialpipeconnection.registerHandler(handler);

        // specialtileconnection was added in LP 0.10.x mid-cycle; older LP builds lack the field
        // entirely and set it to null in SimpleServiceLocator's static initializer. Registering
        // handler here as an ISpecialTileConnection lets LP resolve the remote TileEntity during
        // routing tree construction, not just pipe-to-pipe connections.
        if (SimpleServiceLocator.specialtileconnection != null) {
            SimpleServiceLocator.specialtileconnection.registerHandler(handler);
        }
        logger.info("Registered Additional Pipes teleport connection handler (fix for RS485/LogisticsPipes#348)");
    }
}
