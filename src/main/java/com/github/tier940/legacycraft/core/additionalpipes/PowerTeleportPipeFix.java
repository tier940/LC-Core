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

/**
 * Registers the AP power teleport pipe definition, item, model, and recipe that AP 6.0.0.8
 * omits from its own {@code preInit}.
 *
 * <p>
 * {@link #preInit} calls {@code define()} during {@code FMLPreInitializationEvent} so that
 * BCR's {@code TextureStitchEvent.Pre} handler finds the definition in
 * {@code PipeApi.pipeRegistry} and registers the atlas sprite before texture stitching.
 */
public class PowerTeleportPipeFix {

    // Deferred to the Forge item registry event because items cannot be registered directly during preInit.
    // Non-null only when preInit() succeeded; all other methods gate on this value before acting.
    private static ItemPipeHolder pendingItem = null;

    public static void preInit(FMLPreInitializationEvent event, Logger logger) {
        // AP must be loaded before touching any AP class; Loader.isModLoaded() is safe at preInit.
        if (!Loader.isModLoaded("additionalpipes")) return;
        // Skip if another source already registered the definition (e.g. a future AP update).
        if (APPipeDefintions.powerTeleportPipeDef != null) return;

        try {
            PipeDefinition.PipeDefinitionBuilder builder = new PipeDefinition.PipeDefinitionBuilder();
            // identifier is the registry key (ResourceLocation) used for PipeApi lookup and
            // world-save serialization; it is not a Java class name.
            builder.identifier = new ResourceLocation("additionalpipes", "pipe_power_teleport");
            // texturePrefix is the atlas path used by BCR's TextureStitchEvent.Pre handler to
            // load the 7 directional variants (e.g. _u/_d/_n/_s/_e/_w + _closed).
            builder.texturePrefix = "additionalpipes:pipes/pipe_power_teleport";
            // logicConstructor: invoked when a player places a new pipe block in the world.
            builder.logicConstructor = PipeBehaviorTeleportPowerFixed::new;

            // logicLoader is used when deserializing pipes from a world save (NBT path).
            builder.logicLoader = PipeBehaviorTeleportPowerFixed::new;
            // PipeApi.flowPower matches BCR's built-in power flow handled by PipeFlowPower;
            // any other flow type would produce a ClassCastException at runtime.
            builder.flowType = PipeApi.flowPower;
            PipeDefinition def = builder.define();

            ItemPipeHolder item = ItemPipeHolder.create(def);
            item.setRegistryName(new ResourceLocation("additionalpipes", "pipe_power_teleport"));
            // Unlocalized name must match the lang-file key in AP's lang files so the tooltip
            // resolves correctly; the "pipe.ap." prefix is AP's convention for all its pipe items.
            item.setUnlocalizedName("pipe.ap.pipe_power_teleport");

            // Required so BCR's pipe wrench and other pipe tools recognise this item.
            item.registerWithPipeApi();
            // Defensive null check: AP declares this field and should have set it before our
            // preInit runs (mod dependency order guarantees AP preInit completes first), but
            // guard anyway in case AP itself encountered an initialisation failure.
            if (AdditionalPipes.instance != null && AdditionalPipes.instance.creativeTab != null) {
                try {
                    item.setCreativeTab(AdditionalPipes.instance.creativeTab);
                } catch (NoSuchMethodError ignored) {
                    // Cleanroom changes Item.setCreativeTab return type from Item to void;
                    // the compiled descriptor mismatches at runtime. Creative tab is cosmetic only.
                }
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

    /**
     * Registers the item model variants on the client side so the pipe renders with the correct
     * BCR directional textures. Must be called during {@code ModelRegistryEvent}; calling it
     * earlier causes the model manager to miss the registration.
     */
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event, Logger logger) {
        // pendingItem is non-null only when preInit() fully succeeded; use it directly to
        // avoid casting through APPipeDefintions, which duplicates the reference unnecessarily.
        if (pendingItem == null) return;
        try {
            // Cast to IItemBuildCraft to reach registerVariants(), which submits all directional
            // model variants to the Forge model registry so each face gets its own baked model.
            ((IItemBuildCraft) pendingItem).registerVariants();
            logger.info("Registered AP power teleport pipe item model variants");
        } catch (Exception e) {
            logger.error("Failed to register AP power teleport pipe item model variants", e);
        }
    }

    /**
     * Registers the shapeless crafting recipe: 1x items-teleport pipe + 1x redstone dust
     * → 1x power-teleport pipe. Mirrors the recipe AP would normally provide.
     */
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event, Logger logger) {
        // Guard: if power pipe item is absent, there is nothing to register a recipe for.
        if (APPipeDefintions.powerTeleportPipeItem == null) return;
        // Guard: the items-teleport pipe is the crafting ingredient; skip if it is also absent.
        if (APPipeDefintions.itemsTeleportPipeItem == null) return;
        try {
            ItemStack result = new ItemStack(APPipeDefintions.powerTeleportPipeItem, 1);
            // ShapelessOreRecipe: first arg is the recipe group ResourceLocation (used by the
            // recipe book), second is the output, remaining varargs are the shapeless ingredients.
            // "dustRedstone" matches the OreDictionary entry for any redstone dust variant.
            ShapelessOreRecipe recipe = new ShapelessOreRecipe(
                    new ResourceLocation("additionalpipes", "pipe_power_teleport"),
                    result,
                    new ItemStack(APPipeDefintions.itemsTeleportPipeItem, 1),
                    "dustRedstone");
            // setRegistryName() is required for Forge's recipe registry; reusing the pipe's
            // ResourceLocation is valid because recipe and item registries are separate namespaces.
            recipe.setRegistryName(new ResourceLocation("additionalpipes", "pipe_power_teleport"));
            event.getRegistry().register(recipe);
            logger.info("Registered AP power teleport pipe recipe");
        } catch (Exception e) {
            logger.error("Failed to register AP power teleport pipe recipe", e);
        }
    }
}
