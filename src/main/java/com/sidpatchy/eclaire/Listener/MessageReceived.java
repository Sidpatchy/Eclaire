package com.sidpatchy.eclaire.Listener;

import com.sidpatchy.eclaire.EUtils;
import com.sidpatchy.eclaire.Main;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.Logger;

public class MessageReceived extends ListenerAdapter {
    Logger logger = Main.getLogger();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();

        boolean isValid = EUtils.isValidEMessage(event.getChannel().getIdLong(), message);
        if (isValid) {
            EUtils.createEMessage(message);
        }

        // Nuke the message if it's not a valid E.
        EUtils.deleteInvalidEMessages(event.getChannel().getIdLong(), event.getMessage());
    }

}
