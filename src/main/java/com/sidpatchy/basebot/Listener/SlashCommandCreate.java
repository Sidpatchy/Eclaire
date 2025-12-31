package com.sidpatchy.basebot.Listener;

import com.sidpatchy.Robin.Discord.ParseCommands;
import com.sidpatchy.basebot.Embed.HelpEmbed;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;

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
    }
}
