package com.craftulator;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Craftulator implements ModInitializer {

    public static final String MOD_ID = "craftulator";
    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Craftulator loaded!");
    }
}