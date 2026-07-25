package com.github.tier940.legacycraft.integration.logisticspipes.spec.recipes;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

import com.github.tier940.legacycraft.api.util.ModUtility;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleCrafterMk2;
import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleCrafterMk3;

import logisticspipes.LPItems;

public class Mk3ModuleRecipes {

    public static void register(RegistryEvent.Register<IRecipe> event) {
        registerCrafterMk3(event);
    }

    private static void registerCrafterMk3(RegistryEvent.Register<IRecipe> event) {
        ResourceLocation mk3RL = LPItems.modules.get(ModuleCrafterMk3.getName());
        ResourceLocation mk2RL = LPItems.modules.get(ModuleCrafterMk2.getName());
        if (mk3RL == null || mk2RL == null) return;
        Item mk2 = Item.REGISTRY.getObject(mk2RL);
        Item mk3 = Item.REGISTRY.getObject(mk3RL);
        if (mk2 == null || mk3 == null) return;

        event.getRegistry().register(new ShapedOreRecipe(
                ModUtility.id("module_crafter_mk3"),
                new ItemStack(mk3),
                " F ", "DMD", " F ",
                'M', new ItemStack(mk2),
                'F', new ItemStack(LPItems.chipFPGA),
                'D', "gemDiamond")
                        .setRegistryName(ModUtility.id("module_crafter_mk3")));
    }
}
