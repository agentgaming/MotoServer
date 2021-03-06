package com.mike724.motoserver;

import com.mike724.motoapi.push.MotoPushData;
import com.mike724.motoapi.storage.Storage;
import com.mike724.motoapi.storage.defaults.NetworkPlayer;
import com.mike724.motoapi.storage.defaults.NetworkRank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class MotoCommands implements CommandExecutor {

    @SuppressWarnings("FieldCanBeLocal")
    private MotoServer plugin;

    public MotoCommands(MotoServer plugin) {
        this.plugin = plugin;
    }

    private final String BAD_PERMS = ChatColor.RED + "You have insufficient permissions to run this command.";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("hub")) {
            Player targ;
            if (args.length >= 1) {
                targ = MotoServer.getInstance().getServer().getPlayerExact(args[0]);
            } else {
                if (!(sender instanceof Player)) return true;
                targ = (Player) sender;
            }

            if (targ == null) {
                sender.sendMessage(ChatColor.RED + "Couldn't find player!");
                return true;
            }

            if (MotoServer.getInstance().getServer().getPluginManager().isPluginEnabled("MotoHub")) {
                MotoServer.getInstance().getServer().dispatchCommand(sender, "spawn " + targ.getName());
            } else {
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(b);

                try {
                    out.writeUTF("Connect");
                    out.writeUTF("hub");
                } catch (IOException e) {
                }

                Storage storage = MotoServer.getInstance().getStorage();

                if (storage.cacheContains(targ.getName(), NetworkPlayer.class)) {
                    storage.saveAllObjectsForPlayer(targ.getName(), false);
                }

                final String pName = new String(targ.getName());
                //TODO: May not be needed in a future version
                MotoServer.getInstance().getMotoPush().cmd("pd", pName);

                MotoServer.getInstance().getServer().getMessenger().registerOutgoingPluginChannel(MotoServer.getInstance(), "BungeeCord");
                targ.sendPluginMessage(MotoServer.getInstance(), "BungeeCord", b.toByteArray());
            }

            return true;
        }

        if (!(sender instanceof Player)) {
            if (cmd.getName().equalsIgnoreCase("cmdauth")) {
                if (args.length < 3) return false;

                String player = args[0];
                Integer perms = Integer.parseInt(args[1]);
                String command = "";

                for (int i = 2; i < args.length; i++) {
                    command += args[i] + (i == args.length - 1 ? "" : " ");
                }

                NetworkPlayer np = MotoServer.getInstance().getStorage().getObject(player, NetworkPlayer.class);
                if (np.getRank().getPermission() >= perms) {
                    Bukkit.dispatchCommand(sender, command);
                }

                return true;
            }
            return false;
        }

        Player p = (Player) sender;
        NetworkPlayer np = MotoServer.getInstance().getStorage().getObject(p.getName(), NetworkPlayer.class);

        switch (cmd.getName()) {
            case "net":
                if (np.getRank().getPermission() >= 200) {
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
                if (!validateArgs(args, 1)) return false;
                np.addFriend(args[0]);
                p.sendMessage(ChatColor.GREEN + "'" + args[0] + "' added as friend.");
                MotoServer.getInstance().updateNetworkPlayer(p.getName(), np);
                return true;
            case "delfriend":
                if (!validateArgs(args, 1)) return false;
                if (np.getFriends().contains(args[0])) {
                    np.removeFriend(args[0]);
                    p.sendMessage(ChatColor.GREEN + "'" + args[0] + "' has been removed from your friends.");
                    MotoServer.getInstance().updateNetworkPlayer(p.getName(), np);
                } else {
                    p.sendMessage(ChatColor.RED + "You can't delete a friend you don't have!");
                }
                return true;
            case "setrank":
                if (!validateArgs(args, 2)) return false;
                if (np.getRank().getPermission() >= 1000) {
                    NetworkRank rank = null;
                    String ranksList = "";
                    for (NetworkRank nr : NetworkRank.values()) {
                        ranksList += nr.name() + ", ";
                        if (nr.name().equalsIgnoreCase(args[1].toUpperCase())) {
                            rank = nr;
                        }
                    }

                    if (rank == null) {
                        p.sendMessage(ChatColor.RED + "Rank '" + args[1] + "' does not exist! Must be one of: " + ranksList);
                    } else {
                        NetworkPlayer target = MotoServer.getInstance().getStorage().getObject(args[0], NetworkPlayer.class);
                        target.setRank(NetworkRank.valueOf(args[1]));

                        MotoServer.getInstance().updateNetworkPlayer(args[0], target);
                        if (!MotoServer.getInstance().isPlayerOnServer(args[0]))
                            MotoServer.getInstance().getStorage().removeFromCache(args[0], NetworkPlayer.class);

                        p.sendMessage(ChatColor.RED + "'" + args[0] + "'s rank was set to " + NetworkRank.valueOf(args[1]).name());
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
