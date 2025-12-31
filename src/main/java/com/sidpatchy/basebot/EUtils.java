package com.sidpatchy.basebot;

import com.sidpatchy.basebot.Data.EMessage;
import com.sidpatchy.basebot.Data.MessageStore;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import java.io.IOException;

public class EUtils {

    public static boolean isValidEMessage(long channelID, Message message) {
        if (channelID == 519539412644790287L) {
            return message.getContentRaw().equals("E");
        }
        return false;
    }

    public static EMessage createEMessage(Message message) {
        MessageStore store = Main.getMessageStore();
        User author = message.getAuthor();

        EMessage.Author eAuthor = new EMessage.Author(author.getIdLong(), author.getName(), author.getEffectiveName());
        EMessage eMessage = new EMessage(message.getTimeCreated().toEpochSecond(), message.getContentRaw(), eAuthor, message.getIdLong());
        try {
            store.writeMessage(eMessage);
        } catch (IOException e) {
            Main.getLogger().error("There was an error while writing an E message to disk");
            throw new RuntimeException(e);
        }
        return eMessage;
    }
}
