package com.mike724.motoserver;

import com.amazonaws.util.json.JSONException;
import com.amazonaws.util.json.JSONObject;
import com.google.gson.JsonArray;
import com.mike724.motoapi.push.MotoPush;
import com.mike724.motoapi.push.MotoPushEvent;
import com.mike724.motoapi.storage.DataStorage;
import com.mike724.motoapi.storage.defaults.NetworkPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class MotoEvents implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        //Set player to online and add them to the networkPlayers list
        String playerName = e.getPlayer().getName();
        DataStorage ds = MotoServer.getInstance().getDataStorage();
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
            //Add the network player object to the pool
            NetworkPlayer np = (NetworkPlayer) ds.getObject(NetworkPlayer.class, playerName);
            if(np == null)  np = new NetworkPlayer(playerName);

            if(np.isBanned()) {
                e.setResult(PlayerLoginEvent.Result.KICK_BANNED);
                e.setKickMessage("You are banned from the network!");
            } else {
                MotoServer.getInstance().addNetworkPlayer(np);
                mp.cmd("pc", e.getPlayer().getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        //Set player to offline and remove them from the networkPlayers list
        String playerName = e.getPlayer().getName();
        DataStorage ds = MotoServer.getInstance().getDataStorage();
        MotoPush mp = MotoServer.getInstance().getMotoPush();

        mp.cmd("pd", e.getPlayer().getName());

        NetworkPlayer np = MotoServer.getInstance().getNetworkPlayers().get(playerName);
        ds.writeObject(np,playerName);
        MotoServer.getInstance().removeNetworkPlayer(np.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerKickEvent e) {
        //Set player to offline and remove them from the networkPlayers list if they are on it
        String playerName = e.getPlayer().getName();

        if(MotoServer.getInstance().getNetworkPlayers().containsKey(playerName)) {
            DataStorage ds = MotoServer.getInstance().getDataStorage();
            NetworkPlayer np = MotoServer.getInstance().getNetworkPlayers().get(playerName);
            ds.writeObject(np,playerName);
            MotoServer.getInstance().removeNetworkPlayer(np.getPlayer());

            MotoPush mp = MotoServer.getInstance().getMotoPush();
            mp.cmd("pd", e.getPlayer().getName());
        }
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
