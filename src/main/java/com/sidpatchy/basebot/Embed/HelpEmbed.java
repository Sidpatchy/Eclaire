package com.sidpatchy.basebot.Embed;

import com.sidpatchy.Robin.Discord.Command;
import com.sidpatchy.basebot.Commands;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.EmbedBuilder;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Locale;

public class HelpEmbed {

    private static final Commands commands = Main.getCommands();
    private static final String COMMANDS_LABEL = "Commands";
    private static final String USAGE_LABEL = "Usage";

    // Called by SlashCommandCreate
    public static EmbedBuilder getHelp(String commandName, String userID) throws FileNotFoundException {
        HashMap<String, Command> allCommands = new HashMap<>();
        HashMap<String, Command> regularCommands = new HashMap<>();

        for (Command command : commands.getAllCommands()) {
            if (command == null || command.getName() == null) continue;
            allCommands.put(command.getName().toLowerCase(Locale.ROOT), command);
            regularCommands.put(command.getName(), command);
        }

        if (commandName == null || commandName.equalsIgnoreCase("help")) {
            return buildHelpEmbed(regularCommands);
        } else {
            return buildCommandDetailEmbed(commandName, allCommands);
        }
    }

    private static EmbedBuilder buildHelpEmbed(HashMap<String, Command> regularCommands) {
        StringBuilder commandsList = new StringBuilder("```");

        for (String name : regularCommands.keySet()) {
            if (commandsList.length() > 3) {
                commandsList.append(", ");
            }
            commandsList.append(name);
        }

        commandsList.append("```");

        return new EmbedBuilder()
                .setColor(Main.getColor())
                .addField(COMMANDS_LABEL, commandsList.toString(), false);
    }

    private static EmbedBuilder buildCommandDetailEmbed(String commandName, HashMap<String, Command> allCommands) {
        Command command = allCommands.get(commandName.toLowerCase(Locale.ROOT));

        if (command == null) {
            String errorCode = Main.getErrorCode("help_command");
            Main.getLogger().error("Unknown command in help: " + commandName + " (" + errorCode + ")");
            return ErrorEmbed.getError(errorCode);
        } else {
            String usageBlock = USAGE_LABEL + "\n```" + command.getUsage() + "```";
            String description = (command.getOverview() == null || command.getOverview().isEmpty()) ? command.getHelp() : command.getOverview();
            return new EmbedBuilder()
                    .setColor(Main.getColor())
                    .setAuthor(command.getName().toUpperCase(Locale.ROOT))
                    .setDescription(description)
                    .addField(COMMANDS_LABEL, usageBlock, false);
        }
    }
}

