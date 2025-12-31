package com.sidpatchy.basebot;

import com.sidpatchy.Robin.Discord.CommandFactory;
import com.sidpatchy.Robin.Exception.InvalidConfigurationException;
import com.sidpatchy.Robin.File.ResourceLoader;
import com.sidpatchy.Robin.File.RobinConfiguration;
import com.sidpatchy.basebot.Data.MessageStats;
import com.sidpatchy.basebot.Data.MessageStore;
import com.sidpatchy.basebot.Listener.MessageReceived;
import com.sidpatchy.basebot.Listener.SlashCommandCreate;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Éclaire
 * Copyright (C) 2025  Sidpatchy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * @since December 2025
 * @version 1.0.0
 * @author Sidpatchy
 */
public class Main {

    // Discord API (JDA)
    private static ShardManager shardManager;
    private static Integer currentShardCfg;
    private static Integer totalShardsCfg;
    private static String applicationId;

    private static final long startMillis = System.currentTimeMillis();

    // Various parameters extracted from config files
    private static String botName;
    private static String color;
    private static String errorColor;
    private static String guildID;

    // Commands
    private static final Logger logger = LogManager.getLogger(Main.class);

    // Related to configuration files
    private static final String configFile = "config.yml";
    private static final String commandsFile = "commands.yml";
    private static RobinConfiguration config;
    private static Commands commands;

    private static final MessageStore store = new MessageStore(Path.of("config/messages.smile"));
    private static final MessageStats stats = new MessageStats(store);

    public static void main(String[] args) throws InvalidConfigurationException {
        logger.info("Starting...");

        // Make sure required resources are loaded
        ResourceLoader loader = new ResourceLoader();
        loader.saveResource(configFile, false);
        loader.saveResource(commandsFile, false);

        // Init config handlers
        config = new RobinConfiguration("config/" + configFile);
        loadCommandDefs();

        config.load();

        // Read data from config file
        String token = config.getString("token");
        Integer current_shard = config.getInt("current_shard");
        Integer total_shards = config.getInt("total_shards");
        String video_url = config.getString("video_url");

        extractParametersFromConfig(true);

        currentShardCfg = current_shard;
        totalShardsCfg = total_shards;
        shardManager = DiscordLogin(token, current_shard, total_shards);

        if (shardManager == null) {
            System.exit(2);
        }
        else {
            logger.info("Successfully connected to Discord on shard " + current_shard + " with a total shard count of " + total_shards);
        }

        // Set the bot's activity (streaming if URL provided)
        if (video_url != null && !video_url.isEmpty()) {
            shardManager.setActivity(Activity.streaming("Éclaire v1.0.0", video_url));
        } else {
            shardManager.setActivity(Activity.playing("Éclaire v1.0.0"));
        }

        // Register slash commands
        registerSlashCommands();

        // Register Command-related listeners
        shardManager.addEventListener(new SlashCommandCreate());
        shardManager.addEventListener(new MessageReceived());

        logger.info("Done loading! (" + (System.currentTimeMillis() - startMillis) + "ms)");
    }

    // Connect to Discord and create a ShardManager
    private static ShardManager DiscordLogin(String token, Integer current_shard, Integer total_shards) {
        if (token == null || token.isEmpty()) {
            logger.fatal("Token can't be null or empty. Check your config file!");
            System.exit(1);
        }
        else if (current_shard == null || total_shards == null) {
            logger.fatal("Shard config is empty, check your config file!");
            System.exit(3);
        }

        try {
            // Connect to Discord
            logger.info("Attempting discord login");
            DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
            builder.enableIntents(java.util.EnumSet.allOf(GatewayIntent.class));
            builder.setShardsTotal(total_shards);
            builder.setShards(current_shard);
            ShardManager sm = builder.build();
            // Cache application/bot id for error codes
            applicationId = sm.getShards().isEmpty() ? "0" : String.valueOf(sm.getShards().get(0).getSelfUser().getIdLong());
            return sm;
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.fatal(e.toString());
            logger.fatal("Unable to log in to Discord. Aborting startup!");
        }
        return null;
    }

    // Extract parameters from the config.yml file, update the config if applicable.
    @SuppressWarnings("unchecked")
    public static void extractParametersFromConfig(boolean updateOutdatedConfigs) {
        logger.info("Loading configuration files...");

        try {
            botName = config.getString("botName");
            color = config.getString("color");
            errorColor = config.getString("errorColor");
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error("There was an error while extracting parameters from the config. This isn't fatal but there's a good chance things will be very broken.");
        }

    }

    public static void loadCommandDefs() {
        try {
            commands = CommandFactory.loadConfig("config/" + commandsFile, Commands.class);
            logger.warn(commands.getHelp().getName());
            logger.warn(commands.getHelp().getHelp());
        } catch (IOException e) {
            logger.fatal("There was a fatal error while registering slash commands", e);
            throw new RuntimeException(e);
        }
    }

    // Handle the registry of slash commands and any errors associated.
    public static void registerSlashCommands() {
        try {
            RegisterSlashCommands.RegisterSlashCommand();
            logger.info("Slash commands registered successfully!");
        }
        catch (NullPointerException e) {
            logger.fatal("There was an error while registering slash commands. There's a pretty good chance it's related to an uncaught issue with the commands.yml file.", e);
            logger.fatal("Check your commands.yml file!");
            System.exit(4);
        }
        catch (Exception e) {
            logger.fatal("There was a fatal error while registering slash commands.", e);
            System.exit(5);
        }
    }

    // Getters
    public static Color getColor() { return Color.decode(color); }

    public static Color getErrorColor() { return Color.decode(errorColor); }

    public static String getConfigFile() { return configFile; }

    public static String getCommandsFile() { return "config/" + commandsFile; }

    public static Logger getLogger() { return logger; }

    public static String getErrorCode(String descriptor) {
        return descriptor + ":" + String.valueOf(currentShardCfg) + ":" + String.valueOf(totalShardsCfg) + ":" + applicationId + ":" + System.currentTimeMillis() / 1000L;
    }

    public static ShardManager getShardManager() { return shardManager; }

    public static long getStartMillis() { return startMillis; }

    public static Commands getCommands() {
        return commands;
    }

    public static MessageStore getMessageStore() { return store; }

    public static MessageStats getMessageStats() { return stats; }
}
