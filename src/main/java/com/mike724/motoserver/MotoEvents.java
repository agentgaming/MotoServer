package com.mike724.motoserver;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.google.gson.JsonArray;
import com.mike724.motoapi.push.MotoPush;
import com.mike724.motoapi.push.MotoPushEvent;
import com.mike724.motoapi.storage.DataStorage;
import com.mike724.motoapi.storage.Storage;
import com.mike724.motoapi.storage.defaults.NetworkPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class MotoEvents implements Listener {

    /** Sets player to online */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        String playerName = e.getPlayer().getName();
        Storage storage = MotoServer.getInstance().getStorage();
        MotoPush mp = MotoServer.getInstance().getMotoPush();

        JSONObject json = mp.apiMethod("isplayeronline", e.getPlayer().getName());

        Boolean isOnline = false;
        try {
            isOnline = json.getBoolean("isOnline");
        } catch (JSONException e1) {
            isOnline = false;
        }

        if(isOnline) {
            e.setResult(PlayerLoginEvent.Result.KICK_FULL);
            e.setKickMessage("You are already logged in to another server!");
        } else {
            NetworkPlayer np = (NetworkPlayer)storage.getObject(playerName, NetworkPlayer.class);
            if(np==null) {
                storage.cacheObject(playerName, new NetworkPlayer(playerName));
            }

            if(np.isBanned()) {
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

        mp.cmd("pd", e.getPlayer().getName());
        //Saves object AND removes from cache (false boolean)
        storage.saveObject(playerName, NetworkPlayer.class, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerKicked(PlayerKickEvent e) {
        //Set player to offline and remove them from the networkPlayers list if they are on it
        //TODO: Do we really need to check if the player is on the list?

        String playerName = e.getPlayer().getName();
        Storage storage = MotoServer.getInstance().getStorage();
        MotoPush mp = MotoServer.getInstance().getMotoPush();

        mp.cmd("pd", e.getPlayer().getName());
        storage.saveObject(playerName, NetworkPlayer.class, false);
    }


    //Parse network events
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMotoPush(MotoPushEvent e) {

        switch(e.getPushData().getCommand()) {
            case "kick":
                kick(e);
                break;
            default:
                break;
        }

    }

    private void kick(MotoPushEvent e) {
        String name = e.getPushData().getData().get("name");
        if(name == null || name == "") return;

        Player p = MotoServer.getInstance().getServer().getPlayerExact(name);
        if(p != null) {
            String message = e.getPushData().getData().get("message");
            if(message == null) {
                p.kickPlayer("Kicked by network!");
            } else {
                p.kickPlayer(message);
            }
        }
    }

}
