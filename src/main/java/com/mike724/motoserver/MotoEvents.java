package com.mike724.motoserver;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.mike724.motoapi.push.MotoPush;
import com.mike724.motoapi.push.MotoPushEvent;
import com.mike724.motoapi.storage.Storage;
import com.mike724.motoapi.storage.defaults.NetworkPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@SuppressWarnings("unused")
public class MotoEvents implements Listener {

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

        JSONObject json = mp.apiMethod("isplayeronline", e.getPlayer().getName());

        Boolean isOnline;
        try {
            isOnline = json.getBoolean("isOnline");
        } catch (JSONException e1) {
            isOnline = false;
        }

        if (isOnline) {
            e.setResult(PlayerLoginEvent.Result.KICK_FULL);
            e.setKickMessage("You are already logged in to another server!");
        } else {
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        //Set player to offline and remove them from the networkPlayers list
        String playerName = e.getPlayer().getName();
        Storage storage = MotoServer.getInstance().getStorage();
        MotoPush mp = MotoServer.getInstance().getMotoPush();

        //TODO: Replace this cacheContains if statement to a use a "better" boolean expression
        if (storage.cacheContains(playerName, NetworkPlayer.class)) {
            mp.cmd("pd", e.getPlayer().getName());
            storage.saveAllObjectsForPlayer(playerName, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKicked(PlayerKickEvent e) {
        //Set player to offline and remove them from the networkPlayers list
        String playerName = e.getPlayer().getName();
        Storage storage = MotoServer.getInstance().getStorage();
        MotoPush mp = MotoServer.getInstance().getMotoPush();

        //TODO: Replace this cacheContains if statement to a use a "better" boolean expression
        if (storage.cacheContains(playerName, NetworkPlayer.class)) {
            mp.cmd("pd", e.getPlayer().getName());
            storage.saveAllObjectsForPlayer(playerName, false);
        }
    }

    //Parse network events
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMotoPush(MotoPushEvent e) {

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
