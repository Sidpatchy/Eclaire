package com.sidpatchy.basebot.Listener;

import com.sidpatchy.Robin.Discord.ParseCommands;
import com.sidpatchy.basebot.EUtils;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.Logger;

public class MessageReceived extends ListenerAdapter {
    static ParseCommands parseCommands = new ParseCommands(Main.getCommandsFile());
    Logger logger = Main.getLogger();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();

        //logger.info("Received generic message event: " + event.getMessageId());
        //logger.info(message.getContentRaw());

        boolean isValid = EUtils.isValidEMessage(event.getChannel().getIdLong(), message);
        if (isValid) {
            EUtils.createEMessage(message);
            //logger.info("Valid E message received");
        }
        else {
            //message.delete().queue();
            //logger.info("Invalid E message received");
        }
    }

}
