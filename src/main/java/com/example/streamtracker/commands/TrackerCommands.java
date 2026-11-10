package com.example.streamtracker.commands;

import com.example.streamtracker.chat.ChatListener;
import com.example.streamtracker.chat.MoneyParser;
import com.example.streamtracker.config.TrackerConfig;
import com.example.streamtracker.tracker.PaymentHandler;
import com.example.streamtracker.tracker.SessionTracker;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 *  /streamtracker test <name> <amount>   preview an animation, e.g. /streamtracker test Steve 1.5B
 *  /streamtracker lines                  show the last chat lines the mod saw (to copy the real /pay format)
 *  /streamtracker addregex <regex>       add a payment regex (needs (?<name>..) and (?<amount>..))
 *  /streamtracker reset                  reset the session total
 */
public final class TrackerCommands {
    private TrackerCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("streamtracker")
                        .then(literal("reset").executes(ctx -> {
                            SessionTracker.reset();
                            ctx.getSource().sendFeedback(Text.literal("[StreamTracker] Session reset."));
                            return 1;
                        }))
                        .then(literal("lines").executes(ctx -> {
                            List<String> lines = ChatListener.recentLines();
                            ctx.getSource().sendFeedback(Text.literal("[StreamTracker] Last " + lines.size() + " lines seen (newest first):"));
                            for (String l : lines) ctx.getSource().sendFeedback(Text.literal(" > " + l));
                            return 1;
                        }))
                        .then(literal("addregex")
                                .then(argument("regex", StringArgumentType.greedyString()).executes(ctx -> {
                                    String rx = StringArgumentType.getString(ctx, "regex");
                                    try {
                                        Pattern.compile(rx);
                                    } catch (PatternSyntaxException e) {
                                        ctx.getSource().sendFeedback(Text.literal("[StreamTracker] Invalid regex."));
                                        return 0;
                                    }
                                    TrackerConfig cfg = TrackerConfig.get();
                                    cfg.paymentRegexes.add(0, rx);
                                    cfg.save();
                                    ctx.getSource().sendFeedback(Text.literal("[StreamTracker] Regex added (checked first)."));
                                    return 1;
                                })))
                        .then(literal("test")
                                .then(argument("name", StringArgumentType.word())
                                        .then(argument("amount", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name");
                                                    double amt = MoneyParser.parse(StringArgumentType.getString(ctx, "amount"));
                                                    if (amt <= 0) {
                                                        ctx.getSource().sendFeedback(Text.literal("[StreamTracker] Bad amount."));
                                                        return 0;
                                                    }
                                                    PaymentHandler.onPayment(name, amt);
                                                    return 1;
                                                }))))));
    }
}
