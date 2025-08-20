package net.kappasmp.kappaessentials.dailylogin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kappasmp.kappaessentials.KappaEssentials;
import net.kappasmp.kappaessentials.economy.BalanceManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DailyLoginManager {
    private static final String DAILY_LOGIN_FILE = "daily_logins.json";
    private static Map<UUID, String> lastLoginDates = new HashMap<>();
    private static File dataFile;
    private static final Random random = new Random();
    private static final Gson gson = new Gson();

    public static void init(File configDir) {
        dataFile = new File(configDir, DAILY_LOGIN_FILE);
        loadDailyLoginData();
    }

    public static void loadDailyLoginData() {
        if (!dataFile.exists()) {
            KappaEssentials.log("Daily login file doesn't exist, creating new one.");
            return;
        }

        try (FileReader reader = new FileReader(dataFile)) {
            JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();

            for (String uuidString : data.keySet()) {
                UUID uuid = UUID.fromString(uuidString);
                String lastLoginDate = data.get(uuidString).getAsString();
                lastLoginDates.put(uuid, lastLoginDate);
            }

            KappaEssentials.log("Loaded daily login data for " + lastLoginDates.size() + " players.");
        } catch (Exception e) {
            KappaEssentials.log("Error loading daily login data: " + e.getMessage());
        }
    }

    public static void saveDailyLoginData() {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }

            JsonObject data = new JsonObject();
            for (Map.Entry<UUID, String> entry : lastLoginDates.entrySet()) {
                data.addProperty(entry.getKey().toString(), entry.getValue());
            }

            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(data, writer);
            }

            KappaEssentials.log("Saved daily login data.");
        } catch (IOException e) {
            KappaEssentials.log("Error saving daily login data: " + e.getMessage());
        }
    }

    public static boolean canClaimDailyReward(UUID playerUUID) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String lastLoginDate = lastLoginDates.get(playerUUID);

        return lastLoginDate == null || !lastLoginDate.equals(today);
    }

    public static void claimDailyReward(ServerPlayerEntity player) {
        UUID playerUUID = player.getUuid();

        if (!canClaimDailyReward(playerUUID)) {
            player.sendMessage(Text.literal("§cYou have already claimed your daily reward today!"), false);
            return;
        }

        // Generate random reward between 1 and 10000
        int reward = random.nextInt(10000) + 1;

        // Add money to player's balance
        BalanceManager.addBalance(playerUUID, reward);

        // Update last login date
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        lastLoginDates.put(playerUUID, today);

        // Save the data
        saveDailyLoginData();

        // Send reward message with fancy formatting
        String rewardMessage = formatRewardMessage(reward);
        player.sendMessage(Text.literal(rewardMessage), false);

        KappaEssentials.log("Player " + player.getName().getString() + " claimed daily reward: $" + reward);
    }

    private static String formatRewardMessage(int reward) {
        StringBuilder message = new StringBuilder();
        message.append("§6§l★ DAILY LOGIN REWARD ★\n");
        message.append("§7You received: ");

        // Different colors based on reward amount for gacha excitement
        if (reward >= 8000) {
            message.append("§d§l$").append(reward).append(" §5(LEGENDARY!)");
        } else if (reward >= 5000) {
            message.append("§b§l$").append(reward).append(" §3(EPIC!)");
        } else if (reward >= 2000) {
            message.append("§a§l$").append(reward).append(" §2(RARE!)");
        } else if (reward >= 500) {
            message.append("§e§l$").append(reward).append(" §6(UNCOMMON)");
        } else {
            message.append("§f$").append(reward).append(" §7(COMMON)");
        }

        message.append("\n§7Come back tomorrow for another reward!");

        return message.toString();
    }

    public static String getLastLoginDate(UUID playerUUID) {
        return lastLoginDates.getOrDefault(playerUUID, "Never");
    }

    public static int getDaysUntilNextClaim(UUID playerUUID) {
        if (canClaimDailyReward(playerUUID)) {
            return 0;
        }
        return 1; // Next day
    }

    // Method to check and auto-claim when player joins (optional)
    public static void checkDailyLoginOnJoin(ServerPlayerEntity player) {
        UUID playerUUID = player.getUuid();

        if (canClaimDailyReward(playerUUID)) {
            // Send notification that they can claim
            player.sendMessage(Text.literal("§e§lDaily Login Available! §7Use §f/daily §7to claim your reward!"), false);
        }
    }
}