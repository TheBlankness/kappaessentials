package net.kappasmp.kappaessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.kappasmp.kappaessentials.dailylogin.DailyLoginManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class DailyCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("daily")
                .requires(source -> source.isExecutedByPlayer())
                .executes(DailyCommand::executeDailyReward));
    }

    private static int executeDailyReward(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();

        if (player == null) {
            context.getSource().sendError(Text.literal("This command can only be used by players!"));
            return 0;
        }

        try {
            DailyLoginManager.claimDailyReward(player);
            return 1;
        } catch (Exception e) {
            player.sendMessage(Text.literal("§cError claiming daily reward: " + e.getMessage()), false);
            return 0;
        }
    }
}