package com.sidpatchy.basebot.Embed;

import com.sidpatchy.basebot.Data.ChartType;
import com.sidpatchy.basebot.Data.MessageStats;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsEmbed {
    private static final MessageStats stats = Main.getMessageStats();

    public static EmbedBuilder getStats(User user, ZoneId zoneId) throws IOException {

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Main.getColor())
                .setAuthor("E Stats");

        String avgFirst;
        String avgLast;

        if (user != null) {
            builder.setFooter(user.getEffectiveName() + " (" + user.getId() + ")");

            builder.addField("Confirmed 'E's", String.valueOf(stats.getTotalMessagesByUser(user.getIdLong())), true);

            avgFirst = stats.getAverageFirstMessageTime(user.getIdLong(), MessageStats.Period.DAY, zoneId);
            avgLast = stats.getAverageLastMessageTime(user.getIdLong(), MessageStats.Period.DAY, zoneId);
        }
        else {
            builder.addField("Total Confirmed 'E's", String.valueOf(stats.getTotalMessages()), true);

            avgFirst = stats.getAverageFirstMessageTime(null, MessageStats.Period.DAY, zoneId);
            avgLast = stats.getAverageLastMessageTime(null, MessageStats.Period.DAY, zoneId);
        }

        builder.addField("Average First 'E'", avgFirst, true);
        builder.addField("Average Last 'E'", avgLast, true);

        Long uid = user == null ? null : user.getIdLong();
        builder.addField("Longest Streak", stats.getLongestStreak(uid, zoneId) + " days", true);
        builder.addField("Consistency (30d)", String.format("%.2f%%", stats.getConsistencyScore(uid, 30, zoneId)), true);
        builder.addField("Consistency (Total)", String.format("%.2f%%", stats.getConsistencyScore(uid, 0, zoneId)), true);

        Map<LocalDate, Long> topDays = stats.getTopDays(uid, 3, zoneId);
        String topDaysStr = topDays.entrySet().stream()
                .map(entry -> String.format("`%s`: **%d** E's", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));

        if (!topDaysStr.isEmpty()) {
            builder.addField("Most 'E's Day", topDaysStr, false);
        }

        return builder;
    }

    public static FileUpload generateChart(Long userID, ChartType chartType, ZoneId zoneId) throws IOException {
        File chartFile = null;

        switch (chartType) {
            case HOURLY -> chartFile = stats.generateHourlyChart(userID, zoneId);
            case DAILY -> chartFile = stats.generateDailyChart(userID, zoneId);
            case MONTHLY -> chartFile = stats.generateMonthlyChart(userID, zoneId);
            case YEARLY -> chartFile = stats.generateYearlyChart(userID, zoneId);
        }

        return FileUpload.fromData(chartFile, "Eclaire Stats.png");
    }
}
