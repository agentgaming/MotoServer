package com.mike724.motoserver.debug;

import com.mike724.motoapi.storage.defaults.NetworkPlayer;
import com.mike724.motoapi.storage.defaults.NetworkRank;
import com.mike724.motoserver.MotoServer;
import net.minecraft.server.v1_6_R2.RecipesFurnace;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

public class DebugInterfaceEvents implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent e) {
        if (DebugInterfaces.isRottenPotato(e.getCurrentItem())) {
            e.setCancelled(true);
            return;
        }

        if (e.getWhoClicked() instanceof Player) {
            Player p = (Player) e.getWhoClicked();

            if (e.getSlot() == 8 && MotoServer.getInstance().getStorage().getObject(p.getName(), NetworkPlayer.class).getRank().equals(NetworkRank.OWNER)) {
                p.getInventory().setItem(8, DebugInterfaces.getRottenPotato());
                e.setCancelled(true);
                return;
            }

            if (MotoServer.getInstance().getStorage().getObject(p.getName(), NetworkPlayer.class).getRank().equals(NetworkRank.OWNER)) {
                DebugInterface di = DebugInterfaces.getPlayerInterface(p);
                if (e.getInventory().getName() == di.getDebugInventory().getName() && e.getRawSlot() < 36) {
                    di.handleClick(e.getRawSlot());
                    e.setCancelled(true);
                    return;
                } else {
                    if (di.isModEnabled(31)) {
                        //TODO: VERSION DEPENDENT
                        try {
                            net.minecraft.server.v1_6_R2.ItemStack i = RecipesFurnace.getInstance().getResult(e.getCurrentItem().getTypeId());
                            e.setCurrentItem(new ItemStack(i.id, e.getCurrentItem().getAmount(), (short) i.getData()));
                        } catch (Exception ex) {
                        }
                    }
                    if (di.isModEnabled(13)) e.setCancelled(false);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerOpenInventory(InventoryOpenEvent e) {
        if (MotoServer.getInstance().getStorage().getObject(((Player) e.getPlayer()).getName(), NetworkPlayer.class).getRank().equals(NetworkRank.OWNER)) {
            DebugInterface di = DebugInterfaces.getPlayerInterface((Player) e.getPlayer());
            if (e.getInventory().getName() == di.getDebugInventory().getName() || di.isModEnabled(13)) {
                e.setCancelled(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (MotoServer.getInstance().getStorage().getObject(p.getName(), NetworkPlayer.class).getRank().equals(NetworkRank.OWNER)) {
                DebugInterface di = DebugInterfaces.getPlayerInterface(p);
                if (di.isModEnabled(1)) e.setCancelled(true);
                else if (di.isModEnabled(2)) e.setDamage(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if(e.getAction() == Action.PHYSICAL) return;
        Player p = e.getPlayer();

        if (DebugInterfaces.isRottenPotato(p.getItemInHand())) {
            DebugInterface di = DebugInterfaces.getPlayerInterface(p);
            if (MotoServer.getInstance().getStorage().getObject(p.getName(), NetworkPlayer.class).getRank().equals(NetworkRank.OWNER)) {
                p.openInventory(di.getDebugInventory());
                return;
            } else {
                p.sendMessage(ChatColor.YELLOW + "Nothing interesting happens...");
                p.getInventory().remove(p.getItemInHand());
                return;
            }
        }

        if (MotoServer.getInstance().getStorage().getObject(p.getName(), NetworkPlayer.class).getRank().equals(NetworkRank.OWNER)) {
            DebugInterface di = DebugInterfaces.getPlayerInterface(p);

            if (e.getAction() == Action.LEFT_CLICK_AIR && di.isModEnabled(4)) {
                Block target = p.getTargetBlock(null, 64);
                if (target.getType() != Material.AIR) {
                    Block closestAir = null;
                    for (BlockFace f : BlockFace.values()) {
                        Block rel = target.getRelative(f);
                        if (rel.getType() == Material.AIR) {
                            if (closestAir == null) {
                                closestAir = rel;
                            } else if (p.getLocation().distance(rel.getLocation()) < p.getLocation().distance(closestAir.getLocation())) {
                                closestAir = rel;
                            }
                        }
                    }
                    if (closestAir != null) {
                        closestAir.getLocation().setPitch(p.getLocation().getPitch());
                        closestAir.getLocation().setYaw(p.getLocation().getYaw());
                        p.teleport(closestAir.getLocation());
                    }
                } else {
                    target.getLocation().setPitch(p.getLocation().getPitch());
                    target.getLocation().setYaw(p.getLocation().getYaw());
                    p.teleport(target.getLocation());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();

        if (MotoServer.getInstance().getStorage().getObject(p.getName(), NetworkPlayer.class).getRank().equals(NetworkRank.OWNER)) {
            DebugInterface di = DebugInterfaces.getPlayerInterface(p);
            if (di.isModEnabled(14)) e.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();

        if (MotoServer.getInstance().getStorage().getObject(p.getName(), NetworkPlayer.class).getRank().equals(NetworkRank.OWNER)) {
            DebugInterface di = DebugInterfaces.getPlayerInterface(p);
            if (di.isModEnabled(14)) e.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockDamage(BlockDamageEvent e) {
        Player p = e.getPlayer();
        if (MotoServer.getInstance().getStorage().getObject(p.getName(), NetworkPlayer.class).getRank().equals(NetworkRank.OWNER)) {
            DebugInterface di = DebugInterfaces.getPlayerInterface(p);
            if (di.isModEnabled(14)) e.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (DebugInterfaces.isRottenPotato(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        for(Iterator<ItemStack> i = e.getDrops().iterator(); i.hasNext();) {
            if (DebugInterfaces.isRottenPotato(i.next())) {
                i.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if (MotoServer.getInstance().getStorage().getObject(p.getName(), NetworkPlayer.class).getRank().equals(NetworkRank.OWNER)) {
            DebugInterfaces.createPlayerInterface(p);
            p.getInventory().setItem(8, DebugInterfaces.getRottenPotato());
            p.sendMessage(ChatColor.YELLOW + "Your Rotten Potato has been enabled!");
        }
    }
}