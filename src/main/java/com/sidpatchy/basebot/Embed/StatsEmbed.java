package com.sidpatchy.basebot.Embed;

import com.sidpatchy.basebot.Data.ChartType;
import com.sidpatchy.basebot.Data.MessageStats;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;

public class StatsEmbed {
    private static final MessageStats stats = Main.getMessageStats();

    public static EmbedBuilder getStats(User user) throws IOException {

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Main.getColor())
                .setAuthor("E Stats");

        if (user != null) {
            builder.setFooter(user.getEffectiveName() + " (" + user.getId() + ")");

            builder.addField("Confirmed 'E's: " + stats.getTotalMessagesByUser(user.getIdLong()), "", true);
        }
        else {
            builder.addField("Total Confirmed 'E's: " + stats.getTotalMessages(), "", true);
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
