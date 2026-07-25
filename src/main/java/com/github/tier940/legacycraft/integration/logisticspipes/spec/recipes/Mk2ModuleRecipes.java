package com.github.tier940.legacycraft.integration.logisticspipes.spec.recipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.github.tier940.legacycraft.api.util.ModUtility;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleCrafterMk2;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleProviderMk2;

import logisticspipes.LPItems;

public class Mk2ModuleRecipes {

    public static void register(RegistryEvent.Register<IRecipe> event) {
        registerProviderMk2(event);
        registerCrafterMk2(event);
    }

    private static void registerProviderMk2(RegistryEvent.Register<IRecipe> event) {
        ResourceLocation mk2RL = LPItems.modules.get(ModuleProviderMk2.getName());
        ResourceLocation mk1RL = LPItems.modules.get("provider");
        if (mk2RL == null || mk1RL == null) return;
        Item mk1 = Item.REGISTRY.getObject(mk1RL);
        Item mk2 = Item.REGISTRY.getObject(mk2RL);
        if (mk1 == null || mk2 == null) return;

        event.getRegistry().register(new ShapedOreRecipe(
                ModUtility.id("module_provider_mk2"),
                new ItemStack(mk2),
                " G ", "CMC", " G ",
                'M', new ItemStack(mk1),
                'C', new ItemStack(LPItems.chipAdvanced),
                'G', "ingotGold")
                        .setRegistryName(ModUtility.id("module_provider_mk2")));
    }

    private static void registerCrafterMk2(RegistryEvent.Register<IRecipe> event) {
        ResourceLocation mk2RL = LPItems.modules.get(ModuleCrafterMk2.getName());
        ResourceLocation mk1RL = LPItems.modules.get("crafter");
        if (mk2RL == null || mk1RL == null) return;
        Item mk1 = Item.REGISTRY.getObject(mk1RL);
        Item mk2 = Item.REGISTRY.getObject(mk2RL);
        if (mk1 == null || mk2 == null) return;

        event.getRegistry().register(new ShapedOreRecipe(
                ModUtility.id("module_crafter_mk2"),
                new ItemStack(mk2),
                " G ", "CMC", " G ",
                'M', new ItemStack(mk1),
                'C', new ItemStack(LPItems.chipAdvanced),
                'G', "ingotGold")
                        .setRegistryName(ModUtility.id("module_crafter_mk2")));
    }
}
