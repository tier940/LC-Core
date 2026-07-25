package com.github.tier940.legacycraft.integration.logisticspipes.spec.recipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.github.tier940.legacycraft.api.util.ModUtility;
import com.github.tier940.legacycraft.api.util.Mods;

import logisticspipes.LPItems;

public class Mk2PipeRecipes {

    public static void register(RegistryEvent.Register<IRecipe> event) {
        registerProviderPipeMk2(event);
        registerCraftingPipeMk2(event);
    }

    private static void registerProviderPipeMk2(RegistryEvent.Register<IRecipe> event) {
        Item pipe = Item.REGISTRY.getObject(Mods.LogisticsPipes.getResource("pipe_provider_mk2"));
        if (LPItems.pipeProvider == null || pipe == null) return;

        event.getRegistry().register(new ShapedOreRecipe(
                ModUtility.id("pipe_provider_mk2"),
                new ItemStack(pipe),
                " G ", "CPC", " G ",
                'P', new ItemStack(LPItems.pipeProvider),
                'C', new ItemStack(LPItems.chipAdvanced),
                'G', "ingotGold")
                        .setRegistryName(ModUtility.id("pipe_provider_mk2")));
    }

    private static void registerCraftingPipeMk2(RegistryEvent.Register<IRecipe> event) {
        Item pipe = Item.REGISTRY.getObject(Mods.LogisticsPipes.getResource("pipe_crafting_mk2"));
        if (LPItems.pipeCrafting == null || pipe == null) return;

        event.getRegistry().register(new ShapedOreRecipe(
                ModUtility.id("pipe_crafting_mk2"),
                new ItemStack(pipe),
                " G ", "CPC", " G ",
                'P', new ItemStack(LPItems.pipeCrafting),
                'C', new ItemStack(LPItems.chipAdvanced),
                'G', "ingotGold")
                        .setRegistryName(ModUtility.id("pipe_crafting_mk2")));
    }
}
