package com.sidpatchy.basebot.Listener;

import com.sidpatchy.Robin.Discord.ParseCommands;
import com.sidpatchy.basebot.Data.ChartType;
import com.sidpatchy.basebot.Embed.BuildCacheEmbed;
import com.sidpatchy.basebot.Embed.HelpEmbed;
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
    static ParseCommands parseCommands = new ParseCommands(Main.getCommandsFile());
    Logger logger = Main.getLogger();

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        User author = event.getUser();
        User user = event.getOption("user") != null ? event.getOption("user").getAsUser() : author;

        if (commandName.equalsIgnoreCase(parseCommands.getCommandName("help"))) {
            OptionMapping opt = event.getOption("command-name");
            String command = opt != null ? opt.getAsString() : "help";

            try {
                event.replyEmbeds(HelpEmbed.getHelp(command, event.getUser().getId()).build()).queue();
            } catch (FileNotFoundException e) {
                Main.getLogger().error(e);
                Main.getLogger().error("There was an issue locating the commands file at some point in the chain while the help command was running, good luck!");
            }
        }
        else if (commandName.equalsIgnoreCase(parseCommands.getCommandName("stats"))) {
            OptionMapping cumUserOption = event.getOption("user");
            String chartTypeStr = event.getOption("chart-type").getAsString();
            ChartType chartType = ChartType.valueOf(chartTypeStr);

            OptionMapping timezoneOption = event.getOption("timezone");
            ZoneId zoneId = ZoneId.systemDefault();
            if (timezoneOption != null) {
                try {
                    zoneId = ZoneId.of(timezoneOption.getAsString());
                } catch (DateTimeException e) {
                    // Fallback to system default or notify user?
                    // For now, let's just log and use system default.
                    logger.warn("Invalid timezone provided: " + timezoneOption.getAsString() + ". Using system default.");
                }
            }

            User cumUser = null;
            FileUpload fileUpload = null;
            if (cumUserOption != null) {
                cumUser = cumUserOption.getAsUser();
                try {
                    fileUpload = StatsEmbed.generateChart(cumUser.getIdLong(), chartType, zoneId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else {
                try {
                    fileUpload = StatsEmbed.generateChart(null, chartType, zoneId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            try {
                MessageCreateData messageData = new MessageCreateBuilder()
                        .addEmbeds(StatsEmbed.getStats(cumUser).build())
                        .addFiles(fileUpload)
                        .build();
                event.reply(messageData).queue();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if (commandName.equalsIgnoreCase(parseCommands.getCommandName("buildcache"))) {
            event.deferReply().queue();

            MessageChannel channel = Objects.requireNonNull(event.getOption("channel")).getAsChannel().asTextChannel();

            event.getHook().editOriginalEmbeds(BuildCacheEmbed.getBuildCacheEmbed(author.getId(), channel).build()).queue();
        }
    }
}
