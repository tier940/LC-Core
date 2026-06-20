package com.github.tier940.legacycraft.core;

import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("LCCoreMod")
@IFMLLoadingPlugin.SortingIndex(1001)
public class LCCoreMod implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        String ASM_BASE_PATH = "com.github.tier940.legacycraft.core";
        return new String[] {
                ASM_BASE_PATH + ".akutoengine.ObjHandlerTransformer",
                ASM_BASE_PATH + ".akutoengine.TileEntityTransformer",
                ASM_BASE_PATH + ".logisticspipes.PipeTransportTransformer"
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
