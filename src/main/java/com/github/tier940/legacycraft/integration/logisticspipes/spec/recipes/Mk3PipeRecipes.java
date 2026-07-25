package com.github.tier940.legacycraft.integration.logisticspipes.spec.recipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.github.tier940.legacycraft.api.util.ModUtility;
import com.github.tier940.legacycraft.api.util.Mods;

import logisticspipes.LPItems;

public class Mk3PipeRecipes {

    public static void register(RegistryEvent.Register<IRecipe> event) {
        registerCraftingPipeMk3(event);
    }

    private static void registerCraftingPipeMk3(RegistryEvent.Register<IRecipe> event) {
        Item pipeMk2 = Item.REGISTRY.getObject(Mods.LogisticsPipes.getResource("pipe_crafting_mk2"));
        Item pipeMk3 = Item.REGISTRY.getObject(Mods.LogisticsPipes.getResource("pipe_crafting_mk3"));
        if (pipeMk2 == null || pipeMk3 == null) return;

        event.getRegistry().register(new ShapedOreRecipe(
                ModUtility.id("pipe_crafting_mk3"),
                new ItemStack(pipeMk3),
                " F ", "DPD", " F ",
                'P', new ItemStack(pipeMk2),
                'F', new ItemStack(LPItems.chipFPGA),
                'D', "gemDiamond")
                        .setRegistryName(ModUtility.id("pipe_crafting_mk3")));
    }
}
