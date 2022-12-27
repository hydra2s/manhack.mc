package org.hydra2s.manhack;

import net.fabricmc.api.ModInitializer;
import org.hydra2s.noire.descriptors.RendererCInfo;
import org.hydra2s.noire.objects.RendererObj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MainObj  implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("modid");
    //public static RendererObj rendererObj = null;

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Hello Fabric world!");
        //try {

        //} catch (IOException e) {
            //throw new RuntimeException(e);
        //}
    }
}
