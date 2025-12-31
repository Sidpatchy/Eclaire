package com.sidpatchy.basebot.Embed;

import com.sidpatchy.basebot.EUtils;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.pagination.MessagePaginationAction;

public class BuildCacheEmbed {
    public static EmbedBuilder getBuildCacheEmbed(String authorID, MessageChannel channel) {
        if (!authorID.equalsIgnoreCase("264601404562210828")) return ErrorEmbed.getError(Main.getErrorCode("noPerms"), "You do not have permission to use this command!");

        MessagePaginationAction paginationAction = channel.getIterableHistory();

        for (Message message : paginationAction) {
            if (EUtils.isValidEMessage(channel.getIdLong(), message)) {
                EUtils.createEMessage(message);
            }
            else {
                Main.getLogger().fatal("Invalid E message. Content: {} Author: {}", message.getContentRaw(), message.getAuthor().getEffectiveName());
            }
        }

        return new EmbedBuilder()
                .setColor(Main.getColor())
                .setTitle("Cache Built Successfully!");
    }
}
