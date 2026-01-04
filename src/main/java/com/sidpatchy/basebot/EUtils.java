package com.sidpatchy.basebot;

import com.sidpatchy.basebot.Data.EMessage;
import com.sidpatchy.basebot.Data.MessageStore;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EUtils {

    public static final long E_CHANNEL_ID = 519539412644790287L;

    public static boolean isValidEMessage(long channelID, Message message) {
        if (channelID == E_CHANNEL_ID) {
            return message.getContentRaw().equals("E");
        }
        return false;
    }

    public static void deleteInvalidEMessages(long channelID, Message message) {
        if (channelID == E_CHANNEL_ID && !isValidEMessage(channelID, message)) {
            message.delete().queue();
        }
    }

    public static void fetchMissedMessages(MessageChannel channel) {
        Main.getLogger().info("Rechecking for missed messages in channel: " + channel.getName());
        try {
            Optional<EMessage> latestMessage = Main.getMessageStore().getLatestMessage();
            if (latestMessage.isPresent()) {
                long lastMessageId = latestMessage.get().messageId();
                Main.getLogger().info("Fetching messages after message ID: " + lastMessageId);
                fetchMessagesAfter(channel, lastMessageId);
            } else {
                Main.getLogger().info("No previous messages found in store. Fetching recent history.");
                processMessages(channel, channel.getIterableHistory());
            }
        } catch (IOException e) {
            Main.getLogger().error("Error while fetching latest message from store", e);
            // Fallback to iterable history if store fails
            processMessages(channel, channel.getIterableHistory());
        }
    }

    private static void fetchMessagesAfter(MessageChannel channel, long lastMessageId) {
        channel.getHistoryAfter(lastMessageId, 100).queue(history -> {
            List<Message> messages = history.getRetrievedHistory();
            if (!messages.isEmpty()) {
                Main.getLogger().info("Found " + messages.size() + " missed messages.");
                processMessages(channel, messages);

                // If we got 100 messages, there might be more
                if (messages.size() == 100) {
                    // JDA history is ordered from newest to oldest by default in getRetrievedHistory()?
                    // Actually MessageHistory.getRetrievedHistory() is usually chronological if it's history after.
                    // Let's check the last message in the list to continue fetching.
                    long newLastMessageId = messages.get(0).getIdLong(); // HistoryAfter should be oldest first.
                    // Wait, let's verify the order. HistoryAfter(id, limit) returns messages AFTER 'id'.
                    // Usually it's oldest to newest.
                    for (Message m : messages) {
                        if (m.getIdLong() > newLastMessageId) {
                            newLastMessageId = m.getIdLong();
                        }
                    }
                    fetchMessagesAfter(channel, newLastMessageId);
                }
            } else {
                Main.getLogger().info("No more missed messages found.");
            }
        });
    }

    public static void processMessages(MessageChannel channel, Iterable<Message> messages) {
        List<EMessage> eMessages = new ArrayList<>();
        for (Message message : messages) {
            if (isValidEMessage(channel.getIdLong(), message)) {
                eMessages.add(toEMessage(message));
            } else {
                Main.getLogger().warn("Invalid E message. Content: {} Author: {}, Time: {}",
                        message.getContentRaw(), message.getAuthor().getEffectiveName(), message.getTimeCreated());
            }
        }

        if (!eMessages.isEmpty()) {
            try {
                Main.getMessageStore().writeMessages(eMessages);
            } catch (IOException e) {
                Main.getLogger().error("Error while writing batch E messages", e);
            }
        }
    }

    public static EMessage toEMessage(Message message) {
        User author = message.getAuthor();
        EMessage.Author eAuthor = new EMessage.Author(author.getIdLong(), author.getName(), author.getEffectiveName());
        return new EMessage(message.getTimeCreated().toEpochSecond(), message.getContentRaw(), eAuthor, message.getIdLong());
    }

    public static EMessage createEMessage(Message message) {
        EMessage eMessage = toEMessage(message);
        try {
            Main.getMessageStore().writeMessage(eMessage);
        } catch (IOException e) {
            Main.getLogger().error("There was an error while writing an E message to disk");
            throw new RuntimeException(e);
        }
        return eMessage;
    }
}
