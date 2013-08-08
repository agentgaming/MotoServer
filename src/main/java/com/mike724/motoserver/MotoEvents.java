package com.mike724.motoserver;

import com.mike724.motoapi.push.MotoPushEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MotoEvents implements Listener {

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
