package com.example.streamtracker;

import com.example.streamtracker.chat.ChatListener;
import com.example.streamtracker.commands.TrackerCommands;
import com.example.streamtracker.config.TrackerConfig;
import com.example.streamtracker.effects.AlertEffects;
import com.example.streamtracker.hud.HudRenderer;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamTrackerClient implements ClientModInitializer {
    public static final String MOD_ID = "streamtracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        TrackerConfig.get(); // load config early
        ChatListener.register();
        HudRenderer.register();
        AlertEffects.register();
        TrackerCommands.register();
        LOGGER.info("Stream Tracker loaded (client-only).");
    }
}
