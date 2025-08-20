package net.kappasmp.kappaessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.kappasmp.kappaessentials.economy.BalanceManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

public class BetCommand {
    private static final Random RANDOM = new Random();
    private static final String[] CHOICES = {"rock", "paper", "scissors"};
    private static final double WIN_MULTIPLIER = 1.05; // 5% extra on win

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bet")
                .requires(source -> source.hasPermissionLevel(0))
                .then(CommandManager.argument("choice", StringArgumentType.word())
                        .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0.01))
                                .executes(BetCommand::execute))));
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFeedback(() -> Text.literal("This command can only be used by players!").formatted(Formatting.RED), false);
            return 0;
        }

        String playerChoice = StringArgumentType.getString(context, "choice").toLowerCase();
        double betAmount = DoubleArgumentType.getDouble(context, "amount");

        // Validate choice
        if (!isValidChoice(playerChoice)) {
            player.sendMessage(Text.literal("Invalid choice! Use: rock, paper, or scissors").formatted(Formatting.RED), false);
            return 0;
        }

        // Check if player has enough balance
        double currentBalance = BalanceManager.getBalance(player.getUuid());
        if (currentBalance < betAmount) {
            player.sendMessage(Text.literal("Insufficient funds! You have $" + formatBalance(currentBalance) +
                    " but need $" + formatBalance(betAmount)).formatted(Formatting.RED), false);
            return 0;
        }

        // Generate server choice
        String serverChoice = CHOICES[RANDOM.nextInt(CHOICES.length)];

        // Determine game result
        GameResult result = determineWinner(playerChoice, serverChoice);

        // Process the bet
        processBet(player, betAmount, result);

        // Send game results
        sendGameResults(player, playerChoice, serverChoice, betAmount, result);

        return 1;
    }

    private static boolean isValidChoice(String choice) {
        for (String validChoice : CHOICES) {
            if (validChoice.equals(choice)) {
                return true;
            }
        }
        return false;
    }

    private static GameResult determineWinner(String playerChoice, String serverChoice) {
        if (playerChoice.equals(serverChoice)) {
            return GameResult.TIE;
        }

        switch (playerChoice) {
            case "rock":
                return serverChoice.equals("scissors") ? GameResult.WIN : GameResult.LOSE;
            case "paper":
                return serverChoice.equals("rock") ? GameResult.WIN : GameResult.LOSE;
            case "scissors":
                return serverChoice.equals("paper") ? GameResult.WIN : GameResult.LOSE;
            default:
                return GameResult.LOSE;
        }
    }

    private static void processBet(ServerPlayerEntity player, double betAmount, GameResult result) {
        switch (result) {
            case WIN:
                // Player keeps their money and gets 5% extra
                double winnings = betAmount * 0.05; // Only add the 5% profit
                BalanceManager.addBalance(player.getUuid(), (int) winnings);
                break;
            case LOSE:
                // Player loses their bet
                BalanceManager.withdrawBalance(player.getUuid(), (int) betAmount);
                break;
            case TIE:
                // Nothing happens, player keeps their money
                break;
        }
    }

    private static void sendGameResults(ServerPlayerEntity player, String playerChoice, String serverChoice,
                                        double betAmount, GameResult result) {
        // Send the choices
        player.sendMessage(Text.literal("🎲 ").formatted(Formatting.YELLOW)
                .append(Text.literal("You chose: ").formatted(Formatting.WHITE))
                .append(Text.literal(capitalize(playerChoice)).formatted(Formatting.AQUA))
                .append(Text.literal(" | Server chose: ").formatted(Formatting.WHITE))
                .append(Text.literal(capitalize(serverChoice)).formatted(Formatting.GOLD)), false);

        // Send the result
        switch (result) {
            case WIN:
                double profit = betAmount * 0.05;
                double totalReceived = betAmount + profit;
                player.sendMessage(Text.literal("🎉 ").formatted(Formatting.GREEN)
                        .append(Text.literal("You won! ").formatted(Formatting.GREEN))
                        .append(Text.literal("Received $" + formatBalance(profit) +
                                " profit (total bet + profit: $" + formatBalance(totalReceived) + ")").formatted(Formatting.YELLOW)), false);
                break;
            case LOSE:
                player.sendMessage(Text.literal("💸 ").formatted(Formatting.RED)
                        .append(Text.literal("You lost! ").formatted(Formatting.RED))
                        .append(Text.literal("Lost $" + formatBalance(betAmount)).formatted(Formatting.DARK_RED)), false);
                break;
            case TIE:
                player.sendMessage(Text.literal("🤝 ").formatted(Formatting.GRAY)
                        .append(Text.literal("It's a tie! ").formatted(Formatting.GRAY))
                        .append(Text.literal("Your money is safe.").formatted(Formatting.WHITE)), false);
                break;
        }

        // Show current balance
        double currentBalance = BalanceManager.getBalance(player.getUuid());
        player.sendMessage(Text.literal("💰 ").formatted(Formatting.GOLD)
                .append(Text.literal("Current balance: $" + formatBalance(currentBalance)).formatted(Formatting.GREEN)), false);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static String formatBalance(double balance) {
        if (balance >= 1_000_000_000) {
            return String.format("%.2fB", balance / 1_000_000_000.0);
        } else if (balance >= 1_000_000) {
            return String.format("%.2fM", balance / 1_000_000.0);
        } else if (balance >= 1_000) {
            return String.format("%.2fK", balance / 1_000.0);
        } else {
            return String.format("%.2f", balance);
        }
    }

    private enum GameResult {
        WIN, LOSE, TIE
    }
}