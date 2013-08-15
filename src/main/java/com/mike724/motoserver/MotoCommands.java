package com.mike724.motoserver;

import com.mike724.motoapi.push.MotoPushData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MotoCommands implements CommandExecutor {

    private MotoServer plugin;

    public MotoCommands(MotoServer plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return false;

        if (cmd.getName().equalsIgnoreCase("net")) {
            if (args.length > 0) {
                //TODO: Implement network rank
                if (args.length >= 2 && args[0].equalsIgnoreCase("kick")) {
                    MotoPushData md = new MotoPushData("kick");
                    md.addData("name", args[1]);
                    if (args.length >= 3) {
                        String msg = "";
                        for (int i = 2; i < args.length; i++) msg += args[i] + " ";
                        md.addData("message", msg);
                    }
                    MotoServer.getInstance().getMotoPush().push(md);
                }

            }
            return true;
        }
        return false;
    }
}
