package com.sidpatchy.basebot.Listener;

import com.sidpatchy.basebot.EUtils;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.IOException;

public class MessageUpdateDelete extends ListenerAdapter {

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!EUtils.isValidEMessage(event.getChannel().getIdLong(), event.getMessage())) {
            try {
                Main.getMessageStore().deleteMessage(event.getMessageIdLong());
            } catch (IOException e) {
                Main.getLogger().error("Error while deleting edited message from store", e);
            }
        }

        // Nuke the message if it's not a valid E. We're doing this after we update the DB in case of race conditions.
        EUtils.deleteInvalidEMessages(event.getChannel().getIdLong(), event.getMessage());
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        try {
            Main.getMessageStore().deleteMessage(event.getMessageIdLong());
        } catch (IOException e) {
            Main.getLogger().error("Error while deleting message from store", e);
        }
    }
}
