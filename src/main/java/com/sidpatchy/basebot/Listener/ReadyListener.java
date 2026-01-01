package com.sidpatchy.basebot.Listener;

import com.sidpatchy.basebot.EUtils;
import com.sidpatchy.basebot.Main;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ReadyListener extends ListenerAdapter {

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        Main.getLogger().info("Shard " + event.getJDA().getShardInfo().getShardId() + " is ready!");
        
        MessageChannel channel = event.getJDA().getTextChannelById(EUtils.E_CHANNEL_ID);
        if (channel != null) {
            EUtils.fetchMissedMessages(channel);
        } else {
            Main.getLogger().error("Could not find the configured channel with ID: " + EUtils.E_CHANNEL_ID);
        }
    }
}
