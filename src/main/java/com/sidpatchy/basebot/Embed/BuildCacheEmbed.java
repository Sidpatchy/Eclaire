package com.sidpatchy.basebot.Embed;

import com.sidpatchy.basebot.EUtils;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class BuildCacheEmbed {
    public static EmbedBuilder getBuildCacheEmbed(String authorID, MessageChannel channel) {
        if (!authorID.equalsIgnoreCase("264601404562210828")) return ErrorEmbed.getError(Main.getErrorCode("noPerms"), "You do not have permission to use this command!");

        EUtils.processMessages(channel, channel.getIterableHistory());

        return new EmbedBuilder()
                .setColor(Main.getColor())
                .setTitle("Cache Built Successfully!");
    }
}
