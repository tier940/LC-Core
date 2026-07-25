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

public class Mk3PipeRecipes {

    public static void register() {
        registerCraftingPipeMk3();
    }

    private static void registerCraftingPipeMk3() {
        Item pipeMk2 = Item.REGISTRY.getObject(Mods.LogisticsPipes.getResource("pipe_crafting_mk2"));
        Item pipeMk3 = Item.REGISTRY.getObject(Mods.LogisticsPipes.getResource("pipe_crafting_mk3"));
        if (pipeMk2 == null || pipeMk3 == null) return;

        registerPipeRecipeCategory(
                LogisticsProgramCompilerTileEntity.ProgrammCategories.TIER_3, pipeMk3);
        RecipeManager.craftingManager.addRecipe(
                new ItemStack(pipeMk3),
                new RecipeLayout("fpf", "rar", "gsg"),
                new RecipeIndex('p', programmerIngredient(pipeMk3)),
                new RecipeIndex('s', pipeMk2),
                new RecipeIndex('f', LPItems.chipFPGA),
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
