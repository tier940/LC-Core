package com.github.tier940.legacycraft.api.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.tier940.legacycraft.Tags;

public class ModLog {

    private ModLog() {}

    public static final Logger logger = LogManager.getLogger(Tags.MODNAME);
}
