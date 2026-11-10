package com.example.streamtracker.effects;

import com.example.streamtracker.config.TrackerConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Sounds, client-side fireworks, particle bursts and a tiny tick scheduler. */
public final class AlertEffects {
    private static final class Task {
        int ticks;
        final Runnable run;
        Task(int ticks, Runnable run) { this.ticks = ticks; this.run = run; }
    }

    private static final List<Task> tasks = new ArrayList<>();
    private static int burstTicks = 0;
    private static int burstTotal = 40;

    private AlertEffects() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AlertEffects::tick);
    }

    // ---------------- scheduler ----------------
    public static void later(int delayTicks, Runnable r) {
        if (delayTicks <= 0) { r.run(); return; }
        tasks.add(new Task(delayTicks, r));
    }

    // ---------------- sound ----------------
    public static void playSound(String id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Identifier ident = Identifier.tryParse(id);
        if (ident == null || mc.getSoundManager() == null) return;
        float volume = TrackerConfig.get().soundVolumePercent / 100f;
        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(ident), 1.0f, volume));
    }

    // ---------------- fireworks (purely client-side) ----------------
    /**
     * Type: 0 small ball, 1 large ball, 2 star, 3 creeper face, 4 burst.
     * Offsets are relative to the player.
     */
    public static void firework(int type, int[] colors, int[] fade, boolean flicker, boolean trail,
                                double dx, double dy, double dz) {
        if (!TrackerConfig.get().fireworksEnabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        NbtCompound explosion = new NbtCompound();
        explosion.putByte("Type", (byte) type);
        explosion.putIntArray("Colors", colors);
        explosion.putIntArray("FadeColors", fade);
        explosion.putBoolean("Flicker", flicker);
        explosion.putBoolean("Trail", trail);

        NbtList list = new NbtList();
        list.add(explosion);
        NbtCompound fireworks = new NbtCompound();
        fireworks.put("Explosions", list);

        mc.world.addFireworkParticle(
                mc.player.getX() + dx, mc.player.getY() + dy, mc.player.getZ() + dz,
                0.0, 0.0, 0.0, fireworks);
    }

    /** Spawn one particle at an absolute world position. */
    public static void particle(ParticleEffect effect, double x, double y, double z,
                                double vx, double vy, double vz) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        mc.world.addParticle(effect, x, y, z, vx, vy, vz);
    }

    // ---------------- particle ring ----------------
    public static void startBurst(int ticks) {
        if (!TrackerConfig.get().particlesEnabled) return;
        burstTotal = Math.max(1, ticks);
        burstTicks = burstTotal;
    }

    private static ParticleEffect resolveParticle() {
        Identifier id = Identifier.tryParse(TrackerConfig.get().particleId);
        if (id == null) return null;
        return Registries.PARTICLE_TYPE.getOrEmpty(id)
                .filter(t -> t instanceof ParticleEffect)
                .map(t -> (ParticleEffect) t)
                .orElse(null);
    }

    private static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) {
            tasks.clear();
            burstTicks = 0;
            return;
        }

        if (!tasks.isEmpty()) {
            List<Runnable> due = new ArrayList<>();
            Iterator<Task> it = tasks.iterator();
            while (it.hasNext()) {
                Task t = it.next();
                if (--t.ticks <= 0) { due.add(t.run); it.remove(); }
            }
            for (Runnable r : due) r.run();
        }

        if (burstTicks > 0) {
            burstTicks--;
            ParticleEffect effect = resolveParticle();
            if (effect == null) { burstTicks = 0; return; }

            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
            double rise = (burstTotal - burstTicks) * 0.05;

            for (int i = 0; i < 14; i++) {
                double a = rnd.nextDouble() * Math.PI * 2;
                double r = 1.2 + rnd.nextDouble() * 0.8;
                mc.world.addParticle(effect,
                        px + Math.cos(a) * r, py + 0.2 + rise, pz + Math.sin(a) * r,
                        Math.cos(a) * 0.05, 0.12 + rnd.nextDouble() * 0.1, Math.sin(a) * 0.05);
            }
        }
    }
}
