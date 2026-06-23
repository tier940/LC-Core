package com.github.tier940.legacycraft.core;

import java.util.Map;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

/**
 * FML loading plugin that registers ASM class transformers applied before any mod classes load.
 *
 * <p>
 * The transformers patch third-party bytecode that is incompatible with the versions of
 * BuildCraft and Logistics Pipes bundled with LegacyCraft.
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("LCCoreMod")
// FML sorts coremods ascending by sortIndex (default 0). 1001 guarantees our transformers
// load after all Forge-internal plugins (FMLForgePlugin, FMLCorePlugin) but still during
// the LaunchWrapper phase, before any mod class is first touched.
@IFMLLoadingPlugin.SortingIndex(1001)
public class LCCoreMod implements IFMLLoadingPlugin {

    // Shared package prefix for all transformer classes; avoids repeating and mis-spelling
    // the parent package in each getASMTransformerClass() entry.
    private static final String TRANSFORMER_PACKAGE = "com.github.tier940.legacycraft.core";

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                // Drops the removed EnumGateModifier arg from GateVariant's constructor.
                TRANSFORMER_PACKAGE + ".akutoengine.ObjHandlerTransformer",
                // Drops the removed TileEntity arg from getReceiverToPower().
                TRANSFORMER_PACKAGE + ".akutoengine.TileEntityTransformer",
                // Restores energy/IC2/EnderIO connectivity checks removed in LP 0.10.4.28+.
                TRANSFORMER_PACKAGE + ".logisticspipes.PipeTransportTransformer"
        };
    }

    // Returning null tells FML that this jar does not act as its own ModContainer.
    // The @Mod-annotated class in the same jar fills that role instead.
    @Override
    public String getModContainerClass() {
        return null;
    }

    // Returning null tells FML that this plugin does not provide a custom pre-launch
    // setup hook (IFMLCallHook). No custom setup is needed; transformers are statically configured.
    @Override
    public String getSetupClass() {
        return null;
    }

    // FML injects launch metadata (mcLocation, coremodList, etc.) here;
    // unused because our transformers are statically configured.
    @Override
    public void injectData(Map<String, Object> data) {}

    // This mod does not use an Access Transformer; returning null skips AT registration entirely.
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
