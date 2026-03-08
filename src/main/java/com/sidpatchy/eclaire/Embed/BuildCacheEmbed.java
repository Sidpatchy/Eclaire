package com.sidpatchy.eclaire.Embed;

import com.sidpatchy.eclaire.EUtils;
import com.sidpatchy.eclaire.Main;
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
