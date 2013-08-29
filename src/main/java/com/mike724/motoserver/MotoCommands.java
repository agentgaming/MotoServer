package com.mike724.motoserver;

import com.mike724.motoapi.push.MotoPushData;
import com.mike724.motoapi.storage.defaults.NetworkPlayer;
import com.mike724.motoapi.storage.defaults.NetworkRank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MotoCommands implements CommandExecutor {

    @SuppressWarnings("FieldCanBeLocal")
    private MotoServer plugin;

    public MotoCommands(MotoServer plugin) {
        this.plugin = plugin;
    }

    private final String BAD_PERMS = ChatColor.RED + "You have insufficient permissions to run this command.";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(!(sender instanceof Player)) {
            if(cmd.getName().equalsIgnoreCase("cmdauth")) {
                if(args.length < 3) return false;

                String player = args[0];
                Integer perms = Integer.parseInt(args[1]);
                String command = "";

                for(int i = 2; i < args.length; i++) {
                    command += args[i];
                }

                NetworkPlayer np = MotoServer.getInstance().getStorage().getObject(player, NetworkPlayer.class);
                if(np.getRank().getPermission() >= perms) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    Bukkit.getLogger().info("cmdauth ran '" +  command +"'");
                }

                return true;
            }
            return false;
        }

        Player p = (Player) sender;
        NetworkPlayer np = MotoServer.getInstance().getStorage().getObject(p.getName(),NetworkPlayer.class);

        switch(cmd.getName()) {
            case "net":
                if(np.getRank().getPermission() >= 200) {
                    if (args.length > 0) {
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
                }
                return true;
            case "addfriend":
                if(!validateArgs(args,1)) return false;
                np.addFriend(args[0]);
                p.sendMessage(ChatColor.GREEN + "'" + args[0] + "' added as friend.");
                MotoServer.getInstance().getStorage().cacheObject(p.getName(),np);
                MotoServer.getInstance().getStorage().saveObject(p.getName(),NetworkPlayer.class);
                return true;
            case "delfriend":
                if(!validateArgs(args,1)) return false;
                if(np.getFriends().contains(args[0])) {
                    np.removeFriend(args[0]);
                    p.sendMessage(ChatColor.GREEN + "'" + args[0] + "' has been removed from your friends.");
                    MotoServer.getInstance().getStorage().cacheObject(p.getName(),np);
                    MotoServer.getInstance().getStorage().saveObject(p.getName(),NetworkPlayer.class);
                } else {
                    p.sendMessage(ChatColor.RED + "You can't delete a friend you don't have!");
                }
                return true;
            case "setrank":
                if(!validateArgs(args,2)) return false;
                if(np.getRank().getPermission() >= 1000) {
                    NetworkRank rank = null;
                    String ranksList = "";
                    for (NetworkRank nr : NetworkRank.values()) {
                        ranksList += nr.name() + ", ";
                        if (nr.name().equalsIgnoreCase(args[1])){
                            rank = nr;
                        }
                    }

                    if(rank == null) {
                        p.sendMessage(ChatColor.RED + "Rank '" + args[1] + "' does not exist! Must be one of: " + ranksList);
                    } else {
                        NetworkPlayer target = MotoServer.getInstance().getStorage().getObject(args[0],NetworkPlayer.class);
                        target.setRank(NetworkRank.valueOf(args[1]));

                        MotoServer.getInstance().getStorage().cacheObject(args[0],np);
                        MotoServer.getInstance().getStorage().saveObject(args[0],NetworkPlayer.class);
                        if(!MotoServer.getInstance().isPlayerOnServer(args[0])) MotoServer.getInstance().getStorage().removeFromCache(args[0],NetworkPlayer.class);

                        p.sendMessage(ChatColor.RED + "'" + args[0] + "'s rank was set to " + NetworkRank.valueOf(args[1]).name());
                        MotoPushData md = new MotoPushData("kick");
                        md.addData("name", args[0]);
                        md.addData("message","Your rank has been changed, this requires you to relog.");
                        MotoServer.getInstance().getMotoPush().push(md);
                    }
                } else {
                    p.sendMessage(BAD_PERMS);
                }
                return true;
        }
        return false;
    }

    private boolean validateArgs(String[] args, Integer len) {
        return args.length >= len;
    }
}
