package com.sidpatchy.basebot.Embed;

import com.sidpatchy.basebot.Data.MessageStats;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.io.IOException;
import java.util.Map;

public class LeaderboardEmbed {
    private static final MessageStats stats = Main.getMessageStats();

    public static EmbedBuilder getLeaderboard(ShardManager shardManager) throws IOException {
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Main.getColor())
                .setTitle("🏆 E Leaderboard 🏆")
                .setDescription("The most active E-ers of all time!");

        Map<Long, Long> topUsers = stats.getTopUsers(10);
        int rank = 1;

        StringBuilder leaderboardText = new StringBuilder();
        for (Map.Entry<Long, Long> entry : topUsers.entrySet()) {
            User user = shardManager.getUserById(entry.getKey());
            String name;
            if (user != null) {
                name = user.getEffectiveName();
            } else {
                try {
                    name = shardManager.retrieveUserById(entry.getKey()).complete().getEffectiveName();
                } catch (Exception e) {
                    name = "Unknown User (" + entry.getKey() + ")";
                }
            }
            
            leaderboardText.append(String.format("`%d.` **%s** — %d E's\n", rank++, name, entry.getValue()));
        }

        if (leaderboardText.isEmpty()) {
            leaderboardText.append("No data available yet.");
        }

        builder.addField("", leaderboardText.toString(), false);

        return builder;
    }
}
