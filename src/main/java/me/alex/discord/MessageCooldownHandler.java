package me.alex.discord;

import me.alex.ConfigurationValues;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * A class that stores messages that users have sent before updating the database in memory. Implements a cooldown between messages.
 * @see ConfigurationValues
 */
public class MessageCooldownHandler extends ListenerAdapter {
    private final ConfigurationValues configurationValues;
    private final JDA jda;

    /**
     * @param configurationValues Instance of ConfigurationValues associated with this class.
     * @param jda JDA instance that all Discord api calls are done with.
     * @see JDA
     * @see ConfigurationValues
     */
    public MessageCooldownHandler(ConfigurationValues configurationValues, JDA jda) {
        this.configurationValues = configurationValues;
        this.jda = jda;
    }
    private final HashMap<User,Long> cooldowns = new HashMap<>();
    private final ArrayList<User> users = new ArrayList<>();
    private final ArrayList<Long> timeStamp = new ArrayList<>();

    /**
     * This method will update the messages lists in memory.
     * @param e The MessageReceivedEvent object received when a user sends a message.
     * @see MessageReceivedEvent
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (e.getChannelType() == ChannelType.PRIVATE) {
            return;
        }
        if (!e.getGuild().equals(jda.getGuildById(configurationValues.serverId))) {
            return;
        }
        if (Arrays.asList(configurationValues.ignoredChannels).contains(e.getChannel().getIdLong())) return;
        long time = System.currentTimeMillis();


        if (cooldowns.get(e.getAuthor()) == null || cooldowns.get(e.getAuthor()) <= System.currentTimeMillis()) {
            users.add(e.getAuthor());
            timeStamp.add(time);
            cooldowns.put(e.getAuthor(), time + configurationValues.messageCooldown);
        }
    }

    /**
     * @return An ArrayList of string that are SQL calls ready to be executed by MessageUpdater
     * It also wipes the message data in memory.
     * @see me.alex.sql.MessageUpdater
     */
    public ArrayList<String> generateSqlCalls() {
        ArrayList<String> sqlCalls = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            sqlCalls.add(String.format("INSERT INTO messages VALUES (%s, %s)", users.get(i).getIdLong(), timeStamp.get(i)));
        }
        users.clear();
        timeStamp.clear();
        return sqlCalls;
    }
}
