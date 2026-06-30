package com.github.tier940.legacycraft.mixins.buildcraft;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import buildcraft.api.transport.IInjectable;
import buildcraft.lib.inventory.ItemTransactorHelper;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.silicon.tile.TileAssemblyTable;

@Mixin(value = TileAssemblyTable.class, remap = false)
public class MixinTileAssemblyTable {

    @Redirect(
              method = "update",
              at = @At(
                       value = "INVOKE",
                       target = "Lbuildcraft/lib/misc/InventoryUtil;addToBestAcceptor(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;Lnet/minecraft/item/ItemStack;)V",
                       remap = false))
    private void lcCoreDeterministicOutput(World world, BlockPos pos, EnumFacing excludedFace, ItemStack stack) {
        for (EnumFacing side : EnumFacing.VALUES) {
            if (stack.isEmpty()) return;
            if (side == excludedFace) continue;
            IInjectable injectable = ItemTransactorHelper.getInjectable(
                    world.getTileEntity(pos.offset(side)), side.getOpposite());
            if (injectable == null) continue;
            stack = injectable.injectItem(stack, true, side.getOpposite(), null, 0.0);
        }
        if (stack.isEmpty()) return;
        stack = InventoryUtil.addToRandomInventory(world, pos, stack);
        if (stack.isEmpty()) return;
        InventoryUtil.drop(world, pos, stack);
    }
}
