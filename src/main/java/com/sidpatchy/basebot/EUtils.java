package com.sidpatchy.basebot;

import com.sidpatchy.basebot.Data.EMessage;
import com.sidpatchy.basebot.Data.MessageStore;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.apache.pdfbox.cos.COSName.T;

public class EUtils {

    public static final long E_CHANNEL_ID = 519539412644790287L;
    public static final long E_FAILURE_SHAMING_CHANNEL_ID = 1457239118982418574L;

    public static boolean isValidEMessage(long channelID, Message message) {
        if (channelID == E_CHANNEL_ID) {
            return message.getContentRaw().equals("E");
        }
        return false;
    }

    public static void deleteInvalidEMessages(long channelID, Message message) {
        if (channelID == E_CHANNEL_ID && !isValidEMessage(channelID, message)) {
            message.delete().queue();

            JDA jda = message.getJDA();
            MessageChannel shameChannel = jda.getChannelById(MessageChannel.class, E_FAILURE_SHAMING_CHANNEL_ID);

            String shameMessage = buildShameMessage(message);

            assert shameChannel != null;
            shameChannel.sendMessage(shameMessage).queue();

        }
    }

    private static String buildShameMessage(Message message) {
        User author = message.getAuthor();
        String content = message.getContentRaw();
        String contentToShow = content.matches("^\\s*([A-Za-z])(?:\\s*\\1\\s*)*$") ? content : "";

        List<String> shameTemplates = List.of(
                "%s can't use their keyboard",
                "%s is a bozo",
                "%s is still trying to find E on their keyboard",
                "%s doesn't understand the assignment",
                "%s forgot what letter we're on",
                "%s is too cool for E apparently",
                "%s has rejected the E",
                "%s is a heretic",
                "%s has strayed from the path of E",
                "%s refuses to embrace the E",
                "%s is not one of us",
                "%s has forsaken the sacred letter",
                "%s will not ascend with the rest of us",
                "%s can't use their keyboard, innit",
                "%s is an absolute muppet",
                "%s is still trying to find E on their keyboard, the melt",
                "%s doesn't understand the assignment, does they",
                "%s forgot what letter we're on, bloody hell",
                "%s thinks they're too posh for E",
                "%s is having a proper mare with their keyboard",
                "%s's gone and mucked it up",
                "%s is taking the piss with these non-E letters",
                "%s needs a loicense for that keyboard behaviour",
                "%s is being a right knobhead about E",
                "%s's keyboard went to the chippy without them",
                "%s can't use their keyboard, arr",
                "%s is a scurvy dog",
                "%s is still trying to find E on their keyboard, ye barnacle",
                "%s doesn't understand the assignment, me hearty",
                "%s forgot what letter we be pressin'",
                "%s thinks they're too fancy fer E",
                "%s has mutinied against the letter E",
                "%s's keyboard walked the plank",
                "%s be sailin' against the E winds",
                "%s needs a letter of marque fer that behaviour",
                "%s's fingers be three sheets to the wind",
                "%s abandoned ship on the E",
                "%s be keelhauled fer this treachery against E"
        );
        String template = shameTemplates.get(new Random().nextInt(shameTemplates.size()));

        if (!contentToShow.isEmpty()) {
            template = template + ": %s";
        }
        else {
            template = template + ".";
        }

        return String.format(template, author.getAsMention(), contentToShow);
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
