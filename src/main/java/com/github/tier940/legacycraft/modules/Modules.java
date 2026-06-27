package com.github.tier940.legacycraft.modules;

import com.github.tier940.legacycraft.api.ModValues;
import com.github.tier940.legacycraft.api.modules.IModuleContainer;

public class Modules implements IModuleContainer {

    public static final String MODULE_CORE = "core";
    public static final String MODULE_TOOLS = "tools";
    public static final String MODULE_INTEGRATION = "integration";
    public static final String MODULE_ADDITIONAL_PIPES = "additionalpipes";
    public static final String MODULE_LOGISTICS_PIPES = "logisticspipes";

    @Override
    public String getID() {
        return ModValues.MODID;
    }
}
