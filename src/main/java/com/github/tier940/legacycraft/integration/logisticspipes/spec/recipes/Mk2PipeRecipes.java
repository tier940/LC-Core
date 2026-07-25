package com.github.tier940.legacycraft.integration.logisticspipes.spec.recipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;

import com.github.tier940.legacycraft.api.util.Mods;

import logisticspipes.LPItems;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.recipes.NBTIngredient;
import logisticspipes.recipes.RecipeManager;
import logisticspipes.recipes.RecipeManager.RecipeIndex;
import logisticspipes.recipes.RecipeManager.RecipeLayout;

public class Mk2PipeRecipes {

    public static void register() {
        registerProviderPipeMk2();
        registerCraftingPipeMk2();
    }

    private static void registerProviderPipeMk2() {
        Item pipe = Item.REGISTRY.getObject(Mods.LogisticsPipes.getResource("pipe_provider_mk2"));
        if (LPItems.pipeProvider == null || pipe == null) return;

        registerPipeRecipeCategory(
                LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, pipe);
        RecipeManager.craftingManager.addRecipe(
                new ItemStack(pipe),
                new RecipeLayout(" p ", "rar", "gsg"),
                new RecipeIndex('p', programmerIngredient(pipe)),
                new RecipeIndex('s', LPItems.pipeProvider),
                new RecipeIndex('a', LPItems.chipAdvanced),
                new RecipeIndex('r', "dustRedstone"),
                new RecipeIndex('g', "ingotGold"));
    }

    private static void registerCraftingPipeMk2() {
        Item pipe = Item.REGISTRY.getObject(Mods.LogisticsPipes.getResource("pipe_crafting_mk2"));
        if (LPItems.pipeCrafting == null || pipe == null) return;

        registerPipeRecipeCategory(
                LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_2, pipe);
        RecipeManager.craftingManager.addRecipe(
                new ItemStack(pipe),
                new RecipeLayout(" p ", "rar", "gsg"),
                new RecipeIndex('p', programmerIngredient(pipe)),
                new RecipeIndex('s', LPItems.pipeCrafting),
                new RecipeIndex('a', LPItems.chipAdvanced),
                new RecipeIndex('r', "dustRedstone"),
                new RecipeIndex('g', "ingotGold"));
    }

    private static Ingredient programmerIngredient(Item target) {
        ItemStack stack = new ItemStack(LPItems.logisticsProgrammer);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setString("LogisticsRecipeTarget",
                target.getRegistryName().toString());
        return NBTIngredient.fromStacks(stack);
    }

    private static void registerPipeRecipeCategory(
            net.minecraft.util.ResourceLocation category, Item pipe) {
        if (!LogisticsProgramCompilerTileEntity.programByCategory.containsKey(category)) {
            LogisticsProgramCompilerTileEntity.programByCategory.put(
                    category, new java.util.HashSet<>());
        }
        LogisticsProgramCompilerTileEntity.programByCategory.get(category)
                .add(pipe.getRegistryName());
    }
}
