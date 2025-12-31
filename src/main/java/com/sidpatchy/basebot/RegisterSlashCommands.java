package com.sidpatchy.basebot;

import com.sidpatchy.basebot.Data.ChartType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegisterSlashCommands {
    private static final com.sidpatchy.basebot.Commands commands = Main.getCommands();

    public static void DeleteSlashCommands() {
        JDA jda = getPrimaryJDA();
        if (jda != null) {
            jda.updateCommands().addCommands().queue();
        }
    }

    /**
     * Register slash commands while feeling like you're doing it wrong no matter how you do it!
     * <p>
     * Only called on startup.
     */
    public static void RegisterSlashCommand() {
        JDA jda = getPrimaryJDA();
        if (jda == null) throw new NullPointerException("No available JDA shard to register commands");

        // Create the command list in the help command without repeating the same thing 50 million times.
        List<Command.Choice> helpCommandChoices = new ArrayList<>();
        for (com.sidpatchy.Robin.Discord.Command command : Main.getCommands().getAllCommands()) {
            helpCommandChoices.add(new Command.Choice(command.getName(), command.getName()));
        }

        OptionData helpOption = new OptionData(OptionType.STRING, "command-name", "Command to get more info on", false)
                .addChoices(helpCommandChoices);

        CommandData help = Commands.slash(commands.getHelp().getName(), commands.getHelp().getHelp()).addOptions(helpOption);
        CommandData stats = Commands.slash(commands.getStats().getName(), commands.getStats().getHelp())
                .addOptions(new OptionData(OptionType.USER, "user", "User to get stats for", false))
                .addOptions(new OptionData(OptionType.STRING, "chart-type", "Type of chart to generate", false)
                        .addChoices(Arrays.stream(ChartType.values())
                                .map(type -> new Command.Choice(
                                        type.name().charAt(0) + type.name().substring(1).toLowerCase(), // "HOURLY" -> "Hourly"
                                        type.name()
                                ))
                                .toArray(Command.Choice[]::new)))
                .addOptions(new OptionData(OptionType.STRING, "timezone", "Timezone to use for the chart (e.g. UTC, EST, Europe/London)", false));
        CommandData buildcache = Commands.slash(commands.getBuildcache().getName(), commands.getBuildcache().getHelp())
                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Channel to build cache with", true));

        jda.updateCommands().addCommands(help, stats, buildcache).queue();
    }

    private static JDA getPrimaryJDA() {
        if (Main.getShardManager() == null) return null;
        if (Main.getShardManager().getShards().isEmpty()) return null;
        return Main.getShardManager().getShards().get(0);
    }
}
