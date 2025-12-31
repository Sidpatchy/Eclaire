package com.sidpatchy.basebot.Data;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.smile.SmileFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MessageStore {
    private static final ObjectMapper MAPPER = new ObjectMapper(new SmileFactory());
    private final Path filePath;

    public MessageStore(Path filePath) {
        this.filePath = filePath;
    }

    // Write single message (appends to existing)
    public void writeMessage(EMessage message) throws IOException {
        List<EMessage> messages = readAllMessages();

        // Remove any existing message with the same ID
        messages.removeIf(msg -> msg.messageId().equals(message.messageId()));

        // Add the new/updated message
        messages.add(message);

        writeAllMessages(messages);
    }

    // Read all messages
    public List<EMessage> readAllMessages() throws IOException {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        try (InputStream in = Files.newInputStream(filePath)) {
            return MAPPER.readValue(in,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, EMessage.class));
        }
    }

    // Write all messages (overwrites)
    private void writeAllMessages(List<EMessage> messages) throws IOException {
        try (OutputStream out = Files.newOutputStream(filePath)) {
            MAPPER.writeValue(out, messages);
        }
    }

    // Filter messages
    public List<EMessage> filter(Predicate<EMessage> predicate) throws IOException {
        return readAllMessages().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    // Search by author username
    public List<EMessage> findByUsername(String username) throws IOException {
        return filter(msg -> msg.author().username().equals(username));
    }

    // Search by message ID
    public Optional<EMessage> findById(Long messageId) throws IOException {
        return readAllMessages().stream()
                .filter(msg -> msg.messageId().equals(messageId))
                .findFirst();
    }

    // Get the latest message (highest timestamp)
    public Optional<EMessage> getLatestMessage() throws IOException {
        return readAllMessages().stream()
                .max((m1, m2) -> Long.compare(m1.timestamp(), m2.timestamp()));
    }

    // Search by time range
    public List<EMessage> findByTimeRange(long startTimestamp, long endTimestamp) throws IOException {
        return filter(msg -> msg.timestamp() >= startTimestamp && msg.timestamp() <= endTimestamp);
    }

    // Search by content (contains)
    public List<EMessage> searchContent(String query) throws IOException {
        return filter(msg -> msg.content().toLowerCase().contains(query.toLowerCase()));
    }
}