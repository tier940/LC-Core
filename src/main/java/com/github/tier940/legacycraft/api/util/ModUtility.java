package com.github.tier940.legacycraft.api.util;

import java.util.Objects;
import java.util.Random;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

import org.jetbrains.annotations.NotNull;

import com.github.tier940.legacycraft.api.ModValues;

public class ModUtility {

    private static final Random RANDOM = new Random();

    public static @NotNull ItemStack getModItem(String modID, String itemName) {
        return GameRegistry.makeItemStack(modID + ":" + itemName, 0, 1, null);
    }

    public static @NotNull ItemStack getModItem(String modID, String itemName, int amount) {
        return GameRegistry.makeItemStack(modID + ":" + itemName, 0, amount, null);
    }

    public static @NotNull ItemStack getModItem(String modID, String itemName, int amount, int meta) {
        return GameRegistry.makeItemStack(modID + ":" + itemName, meta, amount, null);
    }

    public static @NotNull ItemStack getModItem(String modID, String itemName, int amount, int meta,
                                                NBTTagCompound nbt) {
        return GameRegistry.makeItemStack(modID + ":" + itemName, meta, amount, nbt != null ? nbt.toString() : null);
    }

    public static @NotNull FluidStack getModFluid(String fluidName) {
        return Objects.requireNonNull(FluidRegistry.getFluidStack(fluidName, 1000));
    }

    public static @NotNull FluidStack getModFluid(String fluidName, int amount) {
        return Objects.requireNonNull(FluidRegistry.getFluidStack(fluidName, amount));
    }

    public static @NotNull ResourceLocation id(String path) {
        return new ResourceLocation(ModValues.MODID, path);
    }

    public static void registerOre(String dictName, ItemStack... itemStacks) {
        for (ItemStack stack : itemStacks) {
            OreDictionary.registerOre(dictName, stack);
        }
    }

    public static String generateRandomString(int length) {
        StringBuilder stringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            stringBuilder.append(RANDOM.nextInt(10));
        }

        return stringBuilder.toString();
    }
}
