package com.mike724.motoserver;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.mike724.motoapi.push.MotoPush;
import com.mike724.motoapi.push.MotoPushEvent;
import com.mike724.motoapi.storage.Storage;
import com.mike724.motoapi.storage.defaults.NetworkPlayer;
import com.mike724.motoapi.storage.defaults.NetworkRank;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.kitteh.tag.PlayerReceiveNameTagEvent;

@SuppressWarnings("unused")
public class MotoEvents implements Listener, PluginMessageListener {

    @EventHandler
    public void onNameTag(PlayerReceiveNameTagEvent event) {
        String dispName = event.getNamedPlayer().getDisplayName();
        NetworkPlayer np = MotoServer.getInstance().getStorage().getObject(event.getNamedPlayer().getName(), NetworkPlayer.class);
        if (np.getRank() == NetworkRank.OWNER) {
            event.setTag(ChatColor.GOLD + dispName);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String playerName = event.getPlayer().getName();
        String playerDispName = event.getPlayer().getDisplayName();

        //np should never be null because we cache it in onPlayerLogin no matter what
        NetworkPlayer np = MotoServer.getInstance().getStorage().getObject(playerName, NetworkPlayer.class);
        NetworkRank rank = np.getRank();

        //yellow is just a default, used if the rank is not accounted for yet
        ChatColor baseColor = ChatColor.YELLOW;
        switch (rank) {
            case OWNER:
                baseColor = ChatColor.GOLD;
                break;
            case ADMIN:
                baseColor = ChatColor.BLUE;
                break;
            case MOD:
                baseColor = ChatColor.GREEN;
                break;
            case BUILDER:
                baseColor = ChatColor.WHITE;
                break;
            case USER:
                baseColor = ChatColor.GRAY;
                break;
        }

        String formattedRank = rank.name().toLowerCase();
        formattedRank = Character.toUpperCase(formattedRank.charAt(0)) + formattedRank.substring(1);
        event.setFormat(baseColor + "[" + formattedRank + "] %s: %s");
    }

    //Sets player to online
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        //We aren't connected to MotoPush so we cannot accept new connections
        if (!MotoServer.getInstance().getMotoPush().isConnected()) {
            e.setResult(PlayerLoginEvent.Result.KICK_FULL);
            e.setKickMessage("This server is unable to connect to the network!");
            return;
        }

        String playerName = e.getPlayer().getName();
        Storage storage = MotoServer.getInstance().getStorage();
        MotoPush mp = MotoServer.getInstance().getMotoPush();

        JSONObject json = null;
        try {
            json = new JSONObject(mp.apiMethod("isplayeronline", e.getPlayer().getName()));
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        Boolean isOnline;
        try {
            isOnline = json.getBoolean("isOnline");
        } catch (JSONException e1) {
            e1.printStackTrace();
            isOnline = false;
        }

        if (isOnline) {
            e.setResult(PlayerLoginEvent.Result.KICK_FULL);
            e.setKickMessage("You are already logged in to another server!");
        } else {
            if (storage.cacheContains(playerName, NetworkPlayer.class))
                storage.removeFromCache(playerName, NetworkPlayer.class);
            NetworkPlayer np = storage.getObject(playerName, NetworkPlayer.class);
            if (np == null) {
                storage.cacheObject(playerName, new NetworkPlayer(playerName));
                np = storage.getObject(playerName, NetworkPlayer.class);
            }

            if (np.isBanned()) {
                e.setResult(PlayerLoginEvent.Result.KICK_BANNED);
                e.setKickMessage("You are banned from the network!");
            } else {
                mp.cmd("pc", e.getPlayer().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        //Set player to offline and remove them from the networkPlayers list
        String playerName = e.getPlayer().getName();
        Storage storage = MotoServer.getInstance().getStorage();
        MotoPush mp = MotoServer.getInstance().getMotoPush();

        //TODO: Replace this cacheContains if statement to a use a "better" boolean expression
        if (storage.cacheContains(playerName, NetworkPlayer.class)) {
            storage.saveAllObjectsForPlayer(playerName, false);
        }

        //Run this command no matter what in order to avoid undesired already logged in
        mp.cmd("pd", e.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKicked(PlayerKickEvent e) {
        //The player isn't going to be kicked, ignore it
        if (e.isCancelled()) {
            return;
        }
        MotoServer.getInstance().getLogger().info("Kick event called");

        //Set player to offline and remove them from the networkPlayers list
        String playerName = e.getPlayer().getName();
        Storage storage = MotoServer.getInstance().getStorage();
        MotoPush mp = MotoServer.getInstance().getMotoPush();

        //TODO: Replace this cacheContains if statement to a use a "better" boolean expression
        if (storage.cacheContains(playerName, NetworkPlayer.class)) {
            storage.saveAllObjectsForPlayer(playerName, false);
        }

        //Run this command no matter what in order to avoid undesired already logged in
        mp.cmd("pd", e.getPlayer().getName());
    }

    //Parse network events
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMotoPush(final MotoPushEvent e) {
        switch (e.getPushData().getCommand()) {
            case "kick":
                kick(e);
                break;
            default:
                break;
        }

    }

    private void kick(MotoPushEvent e) {
        String name = e.getPushData().getData().get("name");
        if (name == null || name.equals("")) return;

        Player p = MotoServer.getInstance().getServer().getPlayerExact(name);
        if (p != null) {
            String message = e.getPushData().getData().get("message");
            if (message == null) {
                p.kickPlayer("Kicked by network!");
            } else {
                p.kickPlayer(message);
            }
        }
    }

}
