package net.kappasmp.kappaessentials.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public class PayCommand {

    private static final SuggestionProvider<ServerCommandSource> BALANCE_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            int balance = BalanceManager.getBalance(player.getUuid());

            // Add the current balance as a suggestion
            builder.suggest(String.valueOf(balance), Text.literal("Your balance: $" + balance));

            // Add some common amounts if they have enough balance
            if (balance >= 100) builder.suggest("100");
            if (balance >= 500) builder.suggest("500");
            if (balance >= 1000) builder.suggest("1000");
            if (balance >= 5000) builder.suggest("5000");

        } catch (CommandSyntaxException ignored) {
            // If not a player, don't suggest anything
        }

        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("pay")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                .suggests(BALANCE_SUGGESTIONS)
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    try {
                                        ServerPlayerEntity sender = source.getPlayerOrThrow();
                                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                                        int amount = IntegerArgumentType.getInteger(context, "amount");

                                        if (sender.getUuid().equals(target.getUuid())) {
                                            source.sendFeedback(() -> Text.literal("§cYou can't pay yourself!"), false);
                                            return 0;
                                        }

                                        int senderBalance = BalanceManager.getBalance(sender.getUuid());
                                        if (senderBalance < amount) {
                                            source.sendFeedback(() -> Text.literal("§cYou don't have enough money!"), false);
                                            return 0;
                                        }

                                        // Perform the transfer
                                        BalanceManager.addBalance(sender.getUuid(), -amount);
                                        BalanceManager.addBalance(target.getUuid(), amount);

                                        source.sendFeedback(() -> Text.literal("§aYou paid §e" + target.getName().getString() + " §a$" + amount), false);
                                        target.sendMessage(Text.literal("§aYou received §a$" + amount + " §afrom §e" + sender.getName().getString()));

                                        return 1;
                                    } catch (CommandSyntaxException e) {
                                        source.sendFeedback(() -> Text.literal("§cError executing command: " + e.getMessage()), false);
                                        return 0;
                                    }
                                }))));
    }
}