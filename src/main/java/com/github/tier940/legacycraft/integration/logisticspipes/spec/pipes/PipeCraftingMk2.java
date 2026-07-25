package com.github.tier940.legacycraft.integration.logisticspipes.spec.pipes;

import java.lang.reflect.Field;

import net.minecraft.item.Item;

import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleCrafterMk2;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;

public class PipeCraftingMk2 extends PipeItemsCraftingLogistics {

    public PipeCraftingMk2(Item item) {
        super(item);
        replaceModule();
    }

    private void replaceModule() {
        try {
            ModuleCrafterMk2 mk2 = new ModuleCrafterMk2();
            mk2.registerHandler(this, this);
            mk2.registerPosition(LogisticsModule.ModulePositionType.IN_PIPE, 0);
            Field f = PipeItemsCraftingLogistics.class.getDeclaredField("craftingModule");
            f.setAccessible(true);
            f.set(this, mk2);
        } catch (Exception ignored) {}
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
}
