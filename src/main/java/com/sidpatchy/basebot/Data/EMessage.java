package com.sidpatchy.basebot.Data;

public record EMessage(
        long timestamp,           // UNIX timestamp in seconds
        String content,           // Message contents
        Author author,            // Author information
        Long messageId            // Message ID
) {

    // Nested record for author information
    public record Author(
            long userId,
            String username,
            String nickname
    ) {}

    public EMessage {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be null or blank");
        }
        if (author == null) {
            throw new IllegalArgumentException("Author cannot be null");
        }
        if (messageId == null) {
            throw new IllegalArgumentException("Message ID cannot be null");
        }
    }
}
