package com.github.tier940.legacycraft.integration.logisticspipes.spec.recipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleCrafterMk2;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleProviderMk2;

import logisticspipes.LPItems;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.recipes.NBTIngredient;
import logisticspipes.recipes.RecipeManager;
import logisticspipes.recipes.RecipeManager.RecipeIndex;
import logisticspipes.recipes.RecipeManager.RecipeLayout;

public class Mk2ModuleRecipes {

    public static void register() {
        registerProviderMk2();
        registerCrafterMk2();
    }

    private static void registerProviderMk2() {
        ResourceLocation mk2RL = LPItems.modules.get(ModuleProviderMk2.getName());
        ResourceLocation mk1RL = LPItems.modules.get("provider");
        if (mk2RL == null || mk1RL == null) return;
        Item mk1 = Item.REGISTRY.getObject(mk1RL);
        Item mk2 = Item.REGISTRY.getObject(mk2RL);
        if (mk1 == null || mk2 == null) return;

        registerModuleCategory(
                LogisticsProgramCompilerTileEntity.ProgrammCategories.CHASSIS_2, mk2RL);
        RecipeManager.craftingManager.addRecipe(
                new ItemStack(mk2),
                new RecipeLayout(" p ", "rar", "gmg"),
                new RecipeIndex('p', programmerIngredient(mk2RL)),
                new RecipeIndex('m', mk1),
                new RecipeIndex('a', LPItems.chipAdvanced),
                new RecipeIndex('r', "dustRedstone"),
                new RecipeIndex('g', "ingotGold"));
    }

    private static void registerCrafterMk2() {
        ResourceLocation mk2RL = LPItems.modules.get(ModuleCrafterMk2.getName());
        ResourceLocation mk1RL = LPItems.modules.get("crafter");
        if (mk2RL == null || mk1RL == null) return;
        Item mk1 = Item.REGISTRY.getObject(mk1RL);
        Item mk2 = Item.REGISTRY.getObject(mk2RL);
        if (mk1 == null || mk2 == null) return;

        registerModuleCategory(
                LogisticsProgramCompilerTileEntity.ProgrammCategories.CHASSIS_2, mk2RL);
        RecipeManager.craftingManager.addRecipe(
                new ItemStack(mk2),
                new RecipeLayout(" p ", "rar", "gmg"),
                new RecipeIndex('p', programmerIngredient(mk2RL)),
                new RecipeIndex('m', mk1),
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
