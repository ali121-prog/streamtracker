package com.example.streamtracker.config;

import com.example.streamtracker.StreamTrackerClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class TrackerConfig {
    public enum Anchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    public static final int TIERS = 6;

    // Each pattern needs named groups (?<name>...) and (?<amount>...) (or group 1 = name, 2 = amount).
    // First pattern that matches wins. Covers the most common /pay message wordings.
    private static final String NAME = "(?<name>[\\w.]{3,20})";
    private static final String AMT = "[\\d,]+(?:\\.\\d+)?[KkMmBbTt]?(?=$|[\\s.!,])";
    private static final String PRE = "^(?:\\[[^\\]]{1,20}\\]\\s*)?";

    public static List<String> defaultRegexes() {
        List<String> l = new ArrayList<>();
        l.add(PRE + NAME + " (?:has )?(?:paid|sent|gave|given) you \\$?(?<amount>" + AMT + ")");
        l.add(PRE + "You (?:have )?(?:received|got) \\$?(?<amount>" + AMT + ") from " + NAME);
        l.add(PRE + "\\$?(?<amount>" + AMT + ") (?:has been )?received from " + NAME);
        return l;
    }

    // --- Parsing ---
    public List<String> paymentRegexes = defaultRegexes();
    /** Off by default: player chat can be spoofed ("X paid you $1B"). */
    public boolean listenToPlayerChat = false;

    // --- Overlay ---
    public boolean overlayEnabled = true;
    public Anchor anchor = Anchor.TOP_LEFT;
    public int offsetX = 6;
    public int offsetY = 6;
    public int scalePercent = 100;
    public int panelWidth = 170;
    public int backgroundOpacityPercent = 55;
    public int barColor = 0x3FD16B;
    public int feedSize = 5;
    public double goalAmount = 100_000_000.0;

    // --- Animation tiers (ascending). Tier N plays when amount >= tierThresholds[N-1] ---
    public double[] tierThresholds = {10_000, 50_000, 100_000, 250_000, 1_000_000, 10_000_000};
    /** Any sound event id. Custom files: add via a resource pack's sounds.json. */
    public String[] tierSounds = {
            "minecraft:entity.experience_orb.pickup",
            "minecraft:block.note_block.bell",
            "minecraft:entity.player.levelup",
            "minecraft:ui.toast.challenge_complete",
            "minecraft:event.raid.horn",
            "minecraft:entity.ender_dragon.growl"
    };
    public int soundVolumePercent = 80;

    // --- Big-donation effects (tier 4 and 5) ---
    public boolean flashEnabled = true;
    public int flashDurationMs = 3000;
    public boolean particlesEnabled = true;
    public String particleId = "minecraft:totem_of_undying";
    public boolean fireworksEnabled = true;

    // ------------------------------------------------------------------

    private transient List<Pattern> compiled;
    private transient List<String> compiledFrom;

    public synchronized List<Pattern> getPatterns() {
        if (compiled == null || !paymentRegexes.equals(compiledFrom)) {
            List<Pattern> out = new ArrayList<>();
            for (String r : paymentRegexes) {
                try {
                    out.add(Pattern.compile(r));
                } catch (PatternSyntaxException e) {
                    StreamTrackerClient.LOGGER.warn("Skipping invalid regex '{}'", r);
                }
            }
            compiled = out;
            compiledFrom = new ArrayList<>(paymentRegexes);
        }
        return compiled;
    }

    private void normalize() {
        TrackerConfig d = new TrackerConfig();
        if (paymentRegexes == null || paymentRegexes.isEmpty()) paymentRegexes = d.paymentRegexes;
        if (tierThresholds == null || tierThresholds.length != TIERS) tierThresholds = d.tierThresholds;
        if (tierSounds == null || tierSounds.length != TIERS) tierSounds = d.tierSounds;
        if (anchor == null) anchor = Anchor.TOP_LEFT;
    }

    // ------------------------------------------------------------------

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static TrackerConfig instance;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("streamtracker.json");
    }

    public static synchronized TrackerConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static TrackerConfig load() {
        Path f = file();
        if (Files.exists(f)) {
            try (Reader r = Files.newBufferedReader(f)) {
                TrackerConfig cfg = GSON.fromJson(r, TrackerConfig.class);
                if (cfg != null) { cfg.normalize(); return cfg; }
            } catch (Exception e) {
                StreamTrackerClient.LOGGER.error("Failed to read config, using defaults", e);
            }
        }
        TrackerConfig cfg = new TrackerConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            StreamTrackerClient.LOGGER.error("Failed to save config", e);
        }
    }
}
