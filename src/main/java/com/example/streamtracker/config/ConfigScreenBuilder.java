package com.example.streamtracker.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

public final class ConfigScreenBuilder {
    private ConfigScreenBuilder() {}

    public static Screen build(Screen parent) {
        TrackerConfig cfg = TrackerConfig.get();
        TrackerConfig def = new TrackerConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Stream Tracker"))
                .setSavingRunnable(cfg::save);
        ConfigEntryBuilder eb = builder.entryBuilder();

        // ---------------- Parsing ----------------
        ConfigCategory parsing = builder.getOrCreateCategory(Text.literal("Chat Parsing"));
        parsing.addEntry(eb.startStrList(Text.literal("Payment regexes (first match wins)"), cfg.paymentRegexes)
                .setDefaultValue(TrackerConfig.defaultRegexes())
                .setTooltip(Text.literal("Use named groups (?<name>...) and (?<amount>...). Amounts: 1,500 / 10M / 1.5B / 2T."))
                .setSaveConsumer(v -> cfg.paymentRegexes = new ArrayList<>(v)).build());
        parsing.addEntry(eb.startBooleanToggle(Text.literal("Also scan signed player chat"), cfg.listenToPlayerChat)
                .setDefaultValue(def.listenToPlayerChat)
                .setTooltip(Text.literal("Risky: other players can type fake payment messages."))
                .setSaveConsumer(v -> cfg.listenToPlayerChat = v).build());

        // ---------------- Overlay ----------------
        ConfigCategory overlay = builder.getOrCreateCategory(Text.literal("Overlay"));
        overlay.addEntry(eb.startBooleanToggle(Text.literal("Show overlay"), cfg.overlayEnabled)
                .setDefaultValue(def.overlayEnabled).setSaveConsumer(v -> cfg.overlayEnabled = v).build());
        overlay.addEntry(eb.startEnumSelector(Text.literal("Anchor corner"), TrackerConfig.Anchor.class, cfg.anchor)
                .setDefaultValue(def.anchor).setSaveConsumer(v -> cfg.anchor = v).build());
        overlay.addEntry(eb.startIntField(Text.literal("Offset X"), cfg.offsetX)
                .setDefaultValue(def.offsetX).setMin(0).setSaveConsumer(v -> cfg.offsetX = v).build());
        overlay.addEntry(eb.startIntField(Text.literal("Offset Y"), cfg.offsetY)
                .setDefaultValue(def.offsetY).setMin(0).setSaveConsumer(v -> cfg.offsetY = v).build());
        overlay.addEntry(eb.startIntSlider(Text.literal("Scale %"), cfg.scalePercent, 50, 250)
                .setDefaultValue(def.scalePercent).setSaveConsumer(v -> cfg.scalePercent = v).build());
        overlay.addEntry(eb.startIntSlider(Text.literal("Panel width"), cfg.panelWidth, 120, 320)
                .setDefaultValue(def.panelWidth).setSaveConsumer(v -> cfg.panelWidth = v).build());
        overlay.addEntry(eb.startIntSlider(Text.literal("Background opacity %"), cfg.backgroundOpacityPercent, 0, 100)
                .setDefaultValue(def.backgroundOpacityPercent).setSaveConsumer(v -> cfg.backgroundOpacityPercent = v).build());
        overlay.addEntry(eb.startColorField(Text.literal("Progress bar color"), cfg.barColor)
                .setDefaultValue(def.barColor).setSaveConsumer(v -> cfg.barColor = v).build());
        overlay.addEntry(eb.startIntSlider(Text.literal("Recent supporters shown"), cfg.feedSize, 1, 12)
                .setDefaultValue(def.feedSize).setSaveConsumer(v -> cfg.feedSize = v).build());
        overlay.addEntry(eb.startDoubleField(Text.literal("Goal amount ($)"), cfg.goalAmount)
                .setDefaultValue(def.goalAmount).setMin(1)
                .setTooltip(Text.literal("10000000000 = $10B"))
                .setSaveConsumer(v -> cfg.goalAmount = v).build());

        // ---------------- Animation tiers ----------------
        ConfigCategory alerts = builder.getOrCreateCategory(Text.literal("Animations"));
        String[] labels = {
                "Tier 1 (small firework)", "Tier 2 (star trio)", "Tier 3 (rainbow ring)",
                "Tier 4 (spiral + banner)", "Tier 5 (grand finale)", "Tier 6 (black hole)"
        };
        for (int i = 0; i < TrackerConfig.TIERS; i++) {
            final int idx = i;
            alerts.addEntry(eb.startDoubleField(Text.literal(labels[i] + " - min $"), cfg.tierThresholds[idx])
                    .setDefaultValue(def.tierThresholds[idx]).setMin(0)
                    .setSaveConsumer(v -> cfg.tierThresholds[idx] = v).build());
            alerts.addEntry(soundField(eb, labels[i] + " - sound", cfg.tierSounds[idx], def.tierSounds[idx],
                    v -> cfg.tierSounds[idx] = v));
        }
        alerts.addEntry(eb.startIntSlider(Text.literal("Sound volume %"), cfg.soundVolumePercent, 0, 100)
                .setDefaultValue(def.soundVolumePercent).setSaveConsumer(v -> cfg.soundVolumePercent = v).build());
        alerts.addEntry(eb.startBooleanToggle(Text.literal("Fireworks"), cfg.fireworksEnabled)
                .setDefaultValue(def.fireworksEnabled).setSaveConsumer(v -> cfg.fireworksEnabled = v).build());
        alerts.addEntry(eb.startBooleanToggle(Text.literal("Big text banner / screen flash (tier 4-5)"), cfg.flashEnabled)
                .setDefaultValue(def.flashEnabled).setSaveConsumer(v -> cfg.flashEnabled = v).build());
        alerts.addEntry(eb.startIntSlider(Text.literal("Banner duration (ms)"), cfg.flashDurationMs, 500, 8000)
                .setDefaultValue(def.flashDurationMs).setSaveConsumer(v -> cfg.flashDurationMs = v).build());
        alerts.addEntry(eb.startBooleanToggle(Text.literal("Particle burst around player"), cfg.particlesEnabled)
                .setDefaultValue(def.particlesEnabled).setSaveConsumer(v -> cfg.particlesEnabled = v).build());
        alerts.addEntry(eb.startStrField(Text.literal("Particle id"), cfg.particleId)
                .setDefaultValue(def.particleId)
                .setTooltip(Text.literal("e.g. minecraft:totem_of_undying, minecraft:firework, minecraft:heart"))
                .setErrorSupplier(ConfigScreenBuilder::validateId)
                .setSaveConsumer(v -> cfg.particleId = v).build());

        return builder.build();
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> soundField(
            ConfigEntryBuilder eb, String label, String value, String def, Consumer<String> save) {
        return eb.startStrField(Text.literal(label), value)
                .setDefaultValue(def)
                .setTooltip(Text.literal("Sound event id, e.g. minecraft:entity.player.levelup or yourpack:donation_big"))
                .setErrorSupplier(ConfigScreenBuilder::validateId)
                .setSaveConsumer(save).build();
    }

    private static Optional<Text> validateId(String v) {
        return Identifier.tryParse(v) == null ? Optional.of(Text.literal("Invalid identifier")) : Optional.empty();
    }
}
