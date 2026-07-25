package com.github.tier940.legacycraft.integration.logisticspipes.spec.pipes;

import java.lang.reflect.Field;

import net.minecraft.item.Item;

import com.github.tier940.legacycraft.integration.logisticspipes.spec.modules.ModuleProviderMk2;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleProvider;
import logisticspipes.pipes.PipeItemsProviderLogistics;

public class PipeProviderMk2 extends PipeItemsProviderLogistics {

    public PipeProviderMk2(Item item) {
        super(item);
        replaceModule();
    }

    private void replaceModule() {
        try {
            ModuleProviderMk2 mk2 = new ModuleProviderMk2();
            mk2.registerHandler(this, this);
            mk2.registerPosition(LogisticsModule.ModulePositionType.IN_PIPE, 0);
            Field f = PipeItemsProviderLogistics.class.getDeclaredField("providerModule");
            f.setAccessible(true);
            f.set(this, mk2);
        } catch (Exception ignored) {}
    }

    @Override
    public ModuleProvider getLogisticsModule() {
        try {
            Field f = PipeItemsProviderLogistics.class.getDeclaredField("providerModule");
            f.setAccessible(true);
            return (ModuleProvider) f.get(this);
        } catch (Exception e) {
            return super.getLogisticsModule();
        }
    }
}
