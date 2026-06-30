package com.github.tier940.legacycraft.mixins.logisticspipes;

import java.util.concurrent.PriorityBlockingQueue;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.ItemRoutingInformation;

@Mixin(value = CoreRoutedPipe.class, remap = false)
public class MixinCoreRoutedPipe {

    private static final String LC_TRANSIT_KEY = "lc_inTransitToMe";
    private static final int NBT_COMPOUND_TAG_ID = 10;

    @Shadow
    protected PriorityBlockingQueue<ItemRoutingInformation> _inTransitToMe;

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void lcCoreSaveInTransit(NBTTagCompound nbt, CallbackInfo ci) {
        NBTTagList list = new NBTTagList();
        for (ItemRoutingInformation info : _inTransitToMe) {
            NBTTagCompound tag = new NBTTagCompound();
            info.writeToNBT(tag);
            list.appendTag(tag);
        }
        nbt.setTag(LC_TRANSIT_KEY, list);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void lcCoreRestoreInTransit(NBTTagCompound nbt, CallbackInfo ci) {
        if (!nbt.hasKey(LC_TRANSIT_KEY)) return;
        NBTTagList list = nbt.getTagList(LC_TRANSIT_KEY, NBT_COMPOUND_TAG_ID);
        for (int i = 0; i < list.tagCount(); i++) {
            ItemRoutingInformation info = ItemRoutingInformation
                    .restoreFromNBT(list.getCompoundTagAt(i));
            if (info == null || info.getItem() == null) continue;
            info.resetDelay();
            _inTransitToMe.add(info);
        }
    }
}
