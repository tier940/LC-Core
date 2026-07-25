package com.github.tier940.legacycraft.integration.logisticspipes.spec.pipes;

import java.lang.reflect.Field;

import net.minecraft.item.Item;

import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleCrafterMk3;

import logisticspipes.interfaces.IBufferItems;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.utils.item.ItemIdentifierStack;

public class PipeCraftingMk3 extends PipeItemsCraftingLogistics implements IBufferItems {

    public PipeCraftingMk3(Item item) {
        super(item);
        replaceModule();
    }

    private void replaceModule() {
        try {
            ModuleCrafterMk3 mk3 = new ModuleCrafterMk3();
            mk3.registerHandler(this, this);
            mk3.registerPosition(LogisticsModule.ModulePositionType.IN_PIPE, 0);
            Field f = PipeItemsCraftingLogistics.class.getDeclaredField("craftingModule");
            f.setAccessible(true);
            f.set(this, mk3);
        } catch (Exception ignored) {}
    }

    @Override
    public Textures.TextureType getCenterTexture() {
        return Textures.LOGISTICSPIPE_CRAFTERMK3_TEXTURE;
    }

    @Override
    public ModuleCrafter getLogisticsModule() {
        try {
            Field f = PipeItemsCraftingLogistics.class.getDeclaredField("craftingModule");
            f.setAccessible(true);
            return (ModuleCrafter) f.get(this);
        } catch (Exception e) {
            return super.getLogisticsModule();
        }
    }

    @Override
    public CoreRoutedPipe.ItemSendMode getItemSendMode() {
        return CoreRoutedPipe.ItemSendMode.Fast;
    }

    @Override
    public int addToBuffer(ItemIdentifierStack stack,
                           logisticspipes.interfaces.routing.IAdditionalTargetInformation info) {
        ModuleCrafter module = getLogisticsModule();
        if (module instanceof ModuleCrafterMk3) {
            return ((ModuleCrafterMk3) module).addToBuffer(stack, info);
        }
        return stack != null ? stack.getStackSize() : 0;
    }

    @Override
    public void onAllowedRemoval() {
        super.onAllowedRemoval();
        ModuleCrafter module = getLogisticsModule();
        if (module instanceof ModuleCrafterMk3 && container != null) {
            ((ModuleCrafterMk3) module).dropBuffer(
                    container.getWorld(), container.getPos());
        }
    }
}
