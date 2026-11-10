package com.example.streamtracker.tracker;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class SessionTracker {
    public record Supporter(String name, double amount, long timeMillis) {}

    private static final int MAX_STORED = 50;
    private static double total = 0;
    private static int count = 0;
    private static final Deque<Supporter> feed = new ArrayDeque<>();

    private SessionTracker() {}

    public static synchronized void add(String name, double amount) {
        total += amount;
        count++;
        feed.addFirst(new Supporter(name, amount, System.currentTimeMillis()));
        while (feed.size() > MAX_STORED) feed.removeLast();
    }

    public static synchronized void reset() {
        total = 0;
        count = 0;
        feed.clear();
    }

    public static synchronized double total() { return total; }
    public static synchronized int count() { return count; }

    public static synchronized List<Supporter> recent(int n) {
        List<Supporter> out = new ArrayList<>(n);
        int i = 0;
        for (Supporter s : feed) {
            if (i++ >= n) break;
            out.add(s);
        }
        return out;
    }
}
