package com.sidpatchy.basebot.Embed;

import com.sidpatchy.basebot.Data.EMessage;
import com.sidpatchy.basebot.Data.MessageStats;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MilestonesEmbed {
    private static final MessageStats stats = Main.getMessageStats();

    public static EmbedBuilder getMilestones(ShardManager shardManager, ZoneId zoneId) throws IOException {
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Main.getColor())
                .setTitle("E Milestones & Records")
                .setDescription("Major historical marks in E history!");

        List<EMessage> allMessages = Main.getMessageStore().readAllMessages();
        allMessages.sort(Comparator.comparingLong(EMessage::timestamp));

        if (allMessages.isEmpty()) {
            builder.setDescription("No 'E's recorded yet. The history is yours to write!");
            return builder;
        }

        // --- GLOBAL MILESTONES ---
        StringBuilder milestonesText = new StringBuilder();
        long count = 0;
        long nextMilestone = 100;
        for (EMessage msg : allMessages) {
            count++;
            if (count == nextMilestone) {
                milestonesText.append(String.format("`%dth E`: **%s** on `%s`\n",
                        count, getUserName(shardManager, msg.author().userId()), formatDate(msg.timestamp(), zoneId)));

                if (count < 1000) {
                    if (count == 100) nextMilestone = 500;
                    else if (count == 500) nextMilestone = 1000;
                } else if (count < 5000) {
                    nextMilestone += 1000;
                } else if (count < 50000) {
                    nextMilestone += 5000;
                } else {
                    nextMilestone += 10000;
                }
            }
        }
        
        if (!milestonesText.isEmpty()) {
            builder.addField("Historical Milestones", milestonesText.toString(), false);
        }

        // --- RECORDS ---
        StringBuilder recordsText = new StringBuilder();
        
        // Most Es in one day
        Map<java.time.LocalDate, Long> topDays = stats.getTopDays(null, 1, zoneId);
        if (!topDays.isEmpty()) {
            Map.Entry<java.time.LocalDate, Long> topDay = topDays.entrySet().iterator().next();
            recordsText.append(String.format("Most Es in a Day: **%d** (`%s`)\n", topDay.getValue(), topDay.getKey()));
        }

        // Most Es in one hour
        MessageStats.HourRecord topHour = stats.getTopHour(null, zoneId);
        if (topHour != null) {
            recordsText.append(String.format("Most Es in a Single Hour: **%d** (`%s` at `%02d:00`)\n", 
                    topHour.count(), topHour.date(), topHour.hour()));
        }

        // Longest Streak
        recordsText.append(String.format("Longest Server Streak: **%d** days\n", stats.getLongestStreak(null, zoneId)));

        if (!recordsText.isEmpty()) {
            builder.addField("Server Records", recordsText.toString(), false);
        }

        return builder;
    }

    private static String getUserName(ShardManager shardManager, long userId) {
        User user = shardManager.getUserById(userId);
        if (user != null) return user.getEffectiveName();
        try {
            return shardManager.retrieveUserById(userId).complete().getEffectiveName();
        } catch (Exception e) {
            return "Unknown User (" + userId + ")";
        }
    }

    private static String formatDate(long timestamp, ZoneId zoneId) {
        return Instant.ofEpochSecond(timestamp).atZone(zoneId).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
