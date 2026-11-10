package com.example.streamtracker.chat;

import com.example.streamtracker.config.TrackerConfig;
import com.example.streamtracker.tracker.PaymentHandler;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.util.Formatting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Listens to incoming chat purely on the client and feeds matches into the tracker.
 * Most servers send payment notices as system ("game") messages.
 */
public final class ChatListener {
    private static final int RECENT_MAX = 15;
    private static final Deque<String> RECENT = new ArrayDeque<>();

    private ChatListener() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) handle(message.getString(), "game");
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> {
            if (TrackerConfig.get().listenToPlayerChat) handle(message.getString(), "chat");
        });
    }

    /** Last few raw lines seen, so you can copy the real /pay format into the regex list. */
    public static synchronized List<String> recentLines() {
        return new ArrayList<>(RECENT);
    }

    private static synchronized void remember(String line) {
        RECENT.addFirst(line);
        while (RECENT.size() > RECENT_MAX) RECENT.removeLast();
    }

    public static boolean handle(String raw) { return handle(raw, "test"); }

    /** Returns true if the line was recognised as a payment. */
    public static boolean handle(String raw, String source) {
        if (raw == null) return false;
        String line = Formatting.strip(raw);
        if (line == null) return false;
        line = line.trim();
        if (line.isEmpty()) return false;
        remember(line);

        for (Pattern p : TrackerConfig.get().getPatterns()) {
            Matcher m = p.matcher(line);
            if (!m.find()) continue;

            String name;
            String amountText;
            try {
                name = m.group("name");
                amountText = m.group("amount");
            } catch (IllegalArgumentException noNamedGroups) {
                if (m.groupCount() < 2) continue;
                name = m.group(1);
                amountText = m.group(2);
            }
            if (name == null || amountText == null) continue;

            double amount = MoneyParser.parse(amountText);
            if (amount <= 0) continue;

            PaymentHandler.onPayment(name, amount);
            return true;
        }
        return false;
    }
}
