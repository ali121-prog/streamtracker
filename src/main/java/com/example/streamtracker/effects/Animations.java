package com.example.streamtracker.effects;

import com.example.streamtracker.chat.MoneyParser;
import com.example.streamtracker.config.TrackerConfig;
import com.example.streamtracker.hud.HudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleTypes;

import java.util.concurrent.ThreadLocalRandom;

/** One animation per tier. Tier index 0..4. */
public final class Animations {
    private static final int[] GOLD = {0xFFD700, 0xFFA500, 0xFFFFFF};
    private static final int[] GREEN = {0x3FD16B, 0xB6FF00, 0xFFFFFF};
    private static final int[] CYAN = {0x00E5FF, 0x2979FF, 0xFFFFFF};
    private static final int[] PINK = {0xFF4081, 0xE040FB, 0xFFFFFF};
    private static final int[] RAINBOW = {0xFF0000, 0xFF7F00, 0xFFFF00, 0x00FF00, 0x00B0FF, 0x8B00FF};

    private Animations() {}

    public static void play(int tier, double amount, String name) {
        String money = "$" + MoneyParser.format(amount);
        TrackerConfig cfg = TrackerConfig.get();
        switch (tier) {
            case 0 -> tier1();
            case 1 -> tier2();
            case 2 -> tier3();
            case 3 -> {
                tier4();
                if (cfg.flashEnabled) HudRenderer.triggerFlash(money, "from " + name, 0xFF66FF, 0);
            }
            case 4 -> {
                tier5();
                if (cfg.flashEnabled) HudRenderer.triggerFlash(money + " DONATION!", "from " + name, 0xFFD700, 90);
            }
            default -> blackHole(money, name);
        }
    }

    // Tier 1: one small gold firework above the player
    private static void tier1() {
        shot(0, GOLD, false, false, 0, 3, 0);
    }

    // Tier 2: three green stars, one after another
    private static void tier2() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 3; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            final double dx = Math.cos(a) * 3, dz = Math.sin(a) * 3, dy = 3 + i;
            AlertEffects.later(i * 6, () -> shot(2, GREEN, false, true, dx, dy, dz));
        }
    }

    // Tier 3: ring of six rainbow large balls + totem particles
    private static void tier3() {
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(i * 60);
            final double dx = Math.cos(a) * 4, dz = Math.sin(a) * 4;
            AlertEffects.later(i * 4, () -> shot(1, RAINBOW, true, false, dx, 4, dz));
        }
        AlertEffects.startBurst(40);
    }

    // Tier 4: rising spiral of bursts/stars, creeper face on top
    private static void tier4() {
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 40);
            final double dx = Math.cos(a) * 4, dz = Math.sin(a) * 4, dy = 2 + i * 0.7;
            final int type = (i % 2 == 0) ? 4 : 2;
            final int[] pal = (i % 2 == 0) ? PINK : CYAN;
            AlertEffects.later(i * 3, () -> shot(type, pal, true, true, dx, dy, dz));
        }
        AlertEffects.later(42, () -> shot(3, GOLD, true, true, 0, 11, 0));
        AlertEffects.startBurst(50);
    }

    // Tier 5: five random waves, then a giant 16-firework ring
    private static void tier5() {
        final int[] types = {1, 2, 3, 4};
        for (int w = 0; w < 5; w++) {
            AlertEffects.later(w * 18, () -> {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int k = 0; k < 8; k++) {
                    shot(types[r.nextInt(types.length)], RAINBOW, true, true,
                            (r.nextDouble() - 0.5) * 16, 2 + r.nextDouble() * 9, (r.nextDouble() - 0.5) * 16);
                }
            });
        }
        AlertEffects.later(95, () -> {
            for (int i = 0; i < 16; i++) {
                double a = Math.toRadians(i * 22.5);
                shot(4, RAINBOW, true, true, Math.cos(a) * 7, 6, Math.sin(a) * 7);
            }
        });
        AlertEffects.startBurst(100);
    }


    // Tier 6: a black hole opens in front of you, sucks in particles, then collapses in a firework explosion
    private static void blackHole(String money, String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        double yaw = Math.toRadians(mc.player.getYaw());
        double cx = mc.player.getX() - Math.sin(yaw) * 7;
        double cz = mc.player.getZ() + Math.cos(yaw) * 7;
        double cy = mc.player.getY() + 4;
        blackHoleTick(0, cx, cy, cz, money, name);
    }

    private static void blackHoleTick(int t, double cx, double cy, double cz, String money, String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        ThreadLocalRandom r = ThreadLocalRandom.current();
        TrackerConfig cfg = TrackerConfig.get();

        if (t < 90) {
            double grow = 0.4 + 0.6 * (t / 90.0);
            int n = (int) (18 * grow);
            for (int i = 0; i < n; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                double rad = 1.0 + r.nextDouble() * 5 * grow;
                // portal particles start at (pos + velocity) and fly into pos: they get sucked into the core
                AlertEffects.particle(ParticleTypes.PORTAL, cx, cy, cz,
                        Math.cos(a) * rad, (r.nextDouble() - 0.5) * 1.2, Math.sin(a) * rad);
                // swirling accretion disk
                double dr = 0.8 + r.nextDouble() * 3.2 * grow;
                AlertEffects.particle(ParticleTypes.DRAGON_BREATH,
                        cx + Math.cos(a) * dr, cy + (r.nextDouble() - 0.5) * 0.3, cz + Math.sin(a) * dr,
                        -Math.sin(a) * 0.2, 0.0, Math.cos(a) * 0.2);
            }
            // dark core
            for (int i = 0; i < 4; i++) {
                AlertEffects.particle(ParticleTypes.SQUID_INK,
                        cx + (r.nextDouble() - 0.5) * 0.8, cy + (r.nextDouble() - 0.5) * 0.8,
                        cz + (r.nextDouble() - 0.5) * 0.8, 0, 0, 0);
            }
            AlertEffects.later(1, () -> blackHoleTick(t + 1, cx, cy, cz, money, name));
            return;
        }

        // collapse
        AlertEffects.playSound("minecraft:entity.generic.explode");
        AlertEffects.particle(ParticleTypes.EXPLOSION_EMITTER, cx, cy, cz, 0, 0, 0);
        double ox = cx - mc.player.getX(), oy = cy - mc.player.getY(), oz = cz - mc.player.getZ();
        AlertEffects.firework(3, new int[]{0xB388FF, 0xFFFFFF}, new int[]{0x000000}, true, true, ox, oy, oz);
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30);
            final double fx = ox + Math.cos(a) * 3, fz = oz + Math.sin(a) * 3;
            AlertEffects.later(i, () -> AlertEffects.firework(4,
                    pick(PINK, 2), pick(CYAN, 1), true, true, fx, oy, fz));
        }
        AlertEffects.startBurst(100);
        if (cfg.flashEnabled) HudRenderer.triggerFlash(money + " BLACK HOLE", "from " + name, 0xB388FF, 110);
    }

    // ------------------------------------------------------------------
    private static void shot(int type, int[] palette, boolean flicker, boolean trail,
                             double dx, double dy, double dz) {
        AlertEffects.firework(type, pick(palette, 2), pick(palette, 1), flicker, trail, dx, dy, dz);
    }

    private static int[] pick(int[] palette, int n) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = palette[r.nextInt(palette.length)];
        return out;
    }
}
