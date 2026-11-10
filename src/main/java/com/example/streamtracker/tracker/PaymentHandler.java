package com.example.streamtracker.tracker;

import com.example.streamtracker.config.TrackerConfig;
import com.example.streamtracker.effects.AlertEffects;
import com.example.streamtracker.effects.Animations;

public final class PaymentHandler {
    private PaymentHandler() {}

    /** -1 if below tier 1, else 0..4 */
    public static int tierFor(double amount) {
        double[] th = TrackerConfig.get().tierThresholds;
        int tier = -1;
        for (int i = 0; i < th.length; i++) if (amount >= th[i]) tier = i;
        return tier;
    }

    public static void onPayment(String name, double amount) {
        TrackerConfig cfg = TrackerConfig.get();
        SessionTracker.add(name, amount);

        int tier = tierFor(amount);
        if (tier < 0) return;

        AlertEffects.playSound(cfg.tierSounds[tier]);
        Animations.play(tier, amount, name);
    }
}
