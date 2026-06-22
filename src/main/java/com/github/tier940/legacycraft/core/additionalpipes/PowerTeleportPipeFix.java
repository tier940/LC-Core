package com.github.tier940.legacycraft.core.additionalpipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import org.apache.logging.log4j.Logger;

import buildcraft.additionalpipes.APPipeDefintions;
import buildcraft.additionalpipes.AdditionalPipes;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.lib.item.IItemBuildCraft;
import buildcraft.transport.item.ItemPipeHolder;

// AP 6.0.0.8 omits powerTeleportPipeDef registration entirely. preInit() calls define()
// during FMLPreInitializationEvent so BCR's TextureStitchEvent.Pre handler picks up the
// definition from PipeApi.pipeRegistry and registers the atlas sprite before stitching.
public class PowerTeleportPipeFix {

    private static ItemPipeHolder pendingItem = null;

    public static void preInit(FMLPreInitializationEvent event, Logger logger) {
        if (!Loader.isModLoaded("additionalpipes")) return;
        if (APPipeDefintions.powerTeleportPipeDef != null) return;

        try {
            PipeDefinition.PipeDefinitionBuilder builder = new PipeDefinition.PipeDefinitionBuilder();
            builder.identifier = new ResourceLocation("additionalpipes", "pipe_power_teleport");
            builder.texturePrefix = "additionalpipes:pipes/pipe_power_teleport";
            builder.logicConstructor = PipeBehaviorTeleportPowerFixed::new;
            builder.logicLoader = (pipe, nbt) -> new PipeBehaviorTeleportPowerFixed(pipe, nbt);
            builder.flowType = PipeApi.flowPower;
            PipeDefinition def = builder.define();

            ItemPipeHolder item = ItemPipeHolder.create(def);
            item.setRegistryName(new ResourceLocation("additionalpipes", "pipe_power_teleport"));
            item.setUnlocalizedName("pipe.ap.pipe_power_teleport");
            item.registerWithPipeApi();
            if (AdditionalPipes.instance != null && AdditionalPipes.instance.creativeTab != null) {
                item.setCreativeTab(AdditionalPipes.instance.creativeTab);
            }

            APPipeDefintions.powerTeleportPipeDef = def;
            APPipeDefintions.powerTeleportPipeItem = item;
            pendingItem = item;

            logger.info("Initialized AP power teleport pipe definition (AP upstream bug)");
        } catch (Exception e) {
            logger.error("Failed to initialize AP power teleport pipe definition", e);
        }
    }

    public static void registerItems(RegistryEvent.Register<Item> event, Logger logger) {
        if (pendingItem == null) return;
        try {
            event.getRegistry().register(pendingItem);
            logger.info("Registered missing AP power teleport pipe item");
        } catch (Exception e) {
            logger.error("Failed to register AP power teleport pipe item", e);
        }
    }

    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event, Logger logger) {
        if (APPipeDefintions.powerTeleportPipeItem == null) return;
        try {
            ((IItemBuildCraft) APPipeDefintions.powerTeleportPipeItem).registerVariants();
            logger.info("Registered AP power teleport pipe item model variants");
        } catch (Exception e) {
            logger.error("Failed to register AP power teleport pipe item model variants", e);
        }
    }

    public static void registerRecipes(RegistryEvent.Register<IRecipe> event, Logger logger) {
        if (APPipeDefintions.powerTeleportPipeItem == null) return;
        if (APPipeDefintions.itemsTeleportPipeItem == null) return;
        try {
            ItemStack result = new ItemStack(APPipeDefintions.powerTeleportPipeItem, 1);
            ShapelessOreRecipe recipe = new ShapelessOreRecipe(
                    new ResourceLocation("additionalpipes", "pipe_power_teleport"),
                    result,
                    new ItemStack(APPipeDefintions.itemsTeleportPipeItem, 1),
                    "dustRedstone");
            recipe.setRegistryName(new ResourceLocation("additionalpipes", "pipe_power_teleport"));
            event.getRegistry().register(recipe);
            logger.info("Registered AP power teleport pipe recipe");
        } catch (Exception e) {
            logger.error("Failed to register AP power teleport pipe recipe", e);
        }
    }
}
