package com.sidpatchy.basebot.Listener;

import com.sidpatchy.Robin.Discord.ParseCommands;
import com.sidpatchy.basebot.Data.ChartType;
import com.sidpatchy.basebot.Embed.BuildCacheEmbed;
import com.sidpatchy.basebot.Embed.HelpEmbed;
import com.sidpatchy.basebot.Embed.LeaderboardEmbed;
import com.sidpatchy.basebot.Embed.MilestonesEmbed;
import com.sidpatchy.basebot.Embed.StatsEmbed;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Objects;

public class SlashCommandCreate extends ListenerAdapter {
    private static final ParseCommands parseCommands = new ParseCommands(Main.getCommandsFile());
    private final Logger logger = Main.getLogger();

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        User author = event.getUser();

        // --- HELP COMMAND ---
        if (commandName.equalsIgnoreCase(parseCommands.getCommandName("help"))) {
            OptionMapping opt = event.getOption("command-name");
            String command = opt != null ? opt.getAsString() : "help";

            try {
                event.replyEmbeds(HelpEmbed.getHelp(command, author.getId()).build()).queue();
            } catch (FileNotFoundException e) {
                logger.error("Error finding commands file during help execution:", e);
                event.reply("An internal error occurred while fetching the help menu.").setEphemeral(true).queue();
            }
        }
        // --- STATS COMMAND ---
        else if (commandName.equalsIgnoreCase(parseCommands.getCommandName("stats"))) {
            // 1. Resolve User
            OptionMapping userOption = event.getOption("user");
            User targetUser = userOption != null ? userOption.getAsUser() : null;

            // 2. Resolve Chart Type
            ChartType chartType = null;
            OptionMapping chartOption = event.getOption("chart-type");
            if (chartOption != null) {
                try {
                    chartType = ChartType.valueOf(chartOption.getAsString());
                } catch (IllegalArgumentException e) {
                    chartType = null; // Invalid chart type, treat as no chart
                }
            }

            // 3. Resolve Timezone
            OptionMapping timezoneOption = event.getOption("timezone");
            ZoneId zoneId = ZoneId.systemDefault();
            if (timezoneOption != null) {
                try {
                    zoneId = ZoneId.of(timezoneOption.getAsString());
                } catch (DateTimeException e) {
                    logger.warn("Invalid timezone provided: {}. Using system default.", timezoneOption.getAsString());
                }
            }

            try {
                if (chartType != null) {
                    // Generate Chart and Embed
                    FileUpload fileUpload;
                    if (targetUser != null) {
                        fileUpload = StatsEmbed.generateChart(targetUser.getIdLong(), chartType, zoneId);
                    } else {
                        fileUpload = StatsEmbed.generateChart(null, chartType, zoneId);
                    }

                    MessageCreateData messageData = new MessageCreateBuilder()
                            .addEmbeds(StatsEmbed.getStats(targetUser, zoneId).build())
                            .addFiles(fileUpload)
                            .build();

                    event.reply(messageData).queue();
                } else {
                    // No Chart, Embed Only
                    // Fixed: Added .queue() and ensuring targetUser is passed correctly
                    event.replyEmbeds(StatsEmbed.getStats(targetUser, zoneId).build()).queue();
                }
            } catch (IOException e) {
                logger.error("Error processing stats command:", e);
                throw new RuntimeException(e);
            }
        }
        // --- BUILD CACHE COMMAND ---
        else if (commandName.equalsIgnoreCase(parseCommands.getCommandName("buildcache"))) {
            event.deferReply().queue();

            OptionMapping channelOption = event.getOption("channel");
            if (channelOption != null) {
                MessageChannel channel = channelOption.getAsChannel().asTextChannel();
                event.getHook().editOriginalEmbeds(
                        BuildCacheEmbed.getBuildCacheEmbed(author.getId(), channel).build()
                ).queue();
            }
        }
        // --- LEADERBOARD COMMAND ---
        else if (commandName.equalsIgnoreCase(parseCommands.getCommandName("leaderboard"))) {
            try {
                event.replyEmbeds(LeaderboardEmbed.getLeaderboard(event.getJDA().getShardManager()).build()).queue();
            } catch (IOException e) {
                logger.error("Error processing leaderboard command:", e);
                event.reply("An error occurred while fetching the leaderboard.").setEphemeral(true).queue();
            }
        }
        // --- MILESTONES COMMAND ---
        else if (commandName.equalsIgnoreCase(parseCommands.getCommandName("milestones"))) {
            // Resolve Timezone
            OptionMapping timezoneOption = event.getOption("timezone");
            ZoneId zoneId = ZoneId.systemDefault();
            if (timezoneOption != null) {
                try {
                    zoneId = ZoneId.of(timezoneOption.getAsString());
                } catch (DateTimeException e) {
                    logger.warn("Invalid timezone provided: {}. Using system default.", timezoneOption.getAsString());
                }
            }

            try {
                event.replyEmbeds(MilestonesEmbed.getMilestones(event.getJDA().getShardManager(), zoneId).build()).queue();
            } catch (IOException e) {
                logger.error("Error processing milestones command:", e);
                event.reply("An error occurred while fetching the milestones.").setEphemeral(true).queue();
            }
        }
    }
}
