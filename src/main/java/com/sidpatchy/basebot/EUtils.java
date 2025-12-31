package com.sidpatchy.basebot;

import com.sidpatchy.basebot.Data.EMessage;
import com.sidpatchy.basebot.Data.MessageStore;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.io.IOException;
import java.util.Optional;

public class EUtils {

    public static final long E_CHANNEL_ID = 519539412644790287L;

    public static boolean isValidEMessage(long channelID, Message message) {
        if (channelID == E_CHANNEL_ID) {
            return message.getContentRaw().equals("E");
        }
        return false;
    }

    public static void fetchMissedMessages(MessageChannel channel) {
        MessageStore store = Main.getMessageStore();
        try {
            Optional<EMessage> latestMessage = store.getLatestMessage();
            if (latestMessage.isPresent()) {
                long lastId = latestMessage.get().messageId();
                Main.getLogger().info("Fetching missed messages after ID: " + lastId);
                fetchMessagesAfter(channel, lastId);
            } else {
                Main.getLogger().info("No previous messages found in store. Skipping missed message fetch.");
            }
        } catch (IOException e) {
            Main.getLogger().error("Error while fetching missed messages", e);
        }
    }

    private static void fetchMessagesAfter(MessageChannel channel, long lastId) {
        channel.getHistoryAfter(lastId, 100).queue(history -> {
            var messages = history.getRetrievedHistory();
            if (messages.isEmpty()) return;

            // JDA's MessageHistory for getHistoryAfter is sorted newest to oldest.
            // Index 0 is the NEWEST message.
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);
                if (isValidEMessage(channel.getIdLong(), message)) {
                    createEMessage(message);
                }
            }

            if (messages.size() == 100) {
                long newestId = messages.get(0).getIdLong();
                fetchMessagesAfter(channel, newestId);
            }
        });
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
