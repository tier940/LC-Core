package com.github.tier940.legacycraft.integration.logisticspipes.spec.recipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleCrafterMk2;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleCrafterMk3;

import logisticspipes.LPItems;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.recipes.NBTIngredient;
import logisticspipes.recipes.RecipeManager;
import logisticspipes.recipes.RecipeManager.RecipeIndex;
import logisticspipes.recipes.RecipeManager.RecipeLayout;

public class Mk3ModuleRecipes {

    public static void register() {
        registerCrafterMk3();
    }

    private static void registerCrafterMk3() {
        ResourceLocation mk3RL = LPItems.modules.get(ModuleCrafterMk3.getName());
        ResourceLocation mk2RL = LPItems.modules.get(ModuleCrafterMk2.getName());
        if (mk3RL == null || mk2RL == null) return;
        Item mk2 = Item.REGISTRY.getObject(mk2RL);
        Item mk3 = Item.REGISTRY.getObject(mk3RL);
        if (mk2 == null || mk3 == null) return;

        registerModuleCategory(
                LogisticsProgramCompilerTileEntity.ProgrammCategories.CHASSIS_3, mk3RL);
        RecipeManager.craftingManager.addRecipe(
                new ItemStack(mk3),
                new RecipeLayout("fpf", "rar", "gmg"),
                new RecipeIndex('p', programmerIngredient(mk3RL)),
                new RecipeIndex('m', mk2),
                new RecipeIndex('f', LPItems.chipFPGA),
                new RecipeIndex('a', LPItems.chipAdvanced),
                new RecipeIndex('r', "dustRedstone"),
                new RecipeIndex('g', "ingotGold"));
    }

    private static Ingredient programmerIngredient(ResourceLocation moduleRL) {
        ItemStack stack = new ItemStack(LPItems.logisticsProgrammer);
        stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setString("LogisticsRecipeTarget", moduleRL.toString());
        return NBTIngredient.fromStacks(stack);
    }

    private static void registerModuleCategory(
            ResourceLocation category, ResourceLocation moduleRL) {
        if (!LogisticsProgramCompilerTileEntity.programByCategory.containsKey(category)) {
            LogisticsProgramCompilerTileEntity.programByCategory.put(
                    category, new java.util.HashSet<>());
        }
        LogisticsProgramCompilerTileEntity.programByCategory.get(category).add(moduleRL);
    }
}
