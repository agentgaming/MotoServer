package com.mike724.motoserver;

import com.mike724.motoapi.push.MotoPush;
import com.mike724.motoapi.storage.Storage;
import org.apache.commons.io.IOUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class MotoServer extends JavaPlugin {

    private static MotoServer instance;
    private MotoPush motoPush;
    private Storage storage;
    private String externalIP;

    @Override
    public void onEnable() {
        //Set our instance
        instance = this;

        //Get this servers external ip
        InputStream is = null;
        try {
            is = new URL("http://checkip.amazonaws.com/").openStream();
            this.externalIP = IOUtils.toString(is) + ":" + this.getServer().getPort();
        } catch (IOException e) {
            System.out.println("Couldn't get external IP.. stopping");
            this.getServer().shutdown();
        } finally {
            IOUtils.closeQuietly(is);
        }

        //Setup Storage
        try {
            storage = new Storage("jxBkqvpe0seZhgfavRqB", "RXaCcuuQcIUFZuVZik9K", "nXWvOgfgRJKBbbzowle1");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Couldn't setup Storage... stopping");
            this.getServer().shutdown();
        }

        //Setup MotoPush
        try {
            motoPush = new MotoPush();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Couldn't setup MotoPush... stopping");
            this.getServer().shutdown();
        }

        //Setup the event listener
        getServer().getPluginManager().registerEvents(new MotoEvents(), this);

        //Setup the command listeners
        getCommand("net").setExecutor(new MotoCommands(this));
        getCommand("addfriend").setExecutor(new MotoCommands(this));
        getCommand("delfriend").setExecutor(new MotoCommands(this));
        getCommand("setrank").setExecutor(new MotoCommands(this));

        this.getLogger().info("MotoServer Enabled");
    }

    @Override
    public void onDisable() {
        //Uh, oh
        this.getLogger().info("MotoServer Disabled");

        //If we go down, the server does too!!
        this.getServer().shutdown();
    }

    public MotoPush getMotoPush() {
        return motoPush;
    }

    public Storage getStorage() {
        return storage;
    }

    public String getExternalIP() {
        return externalIP;
    }

    public Boolean isPlayerOnServer(String name) {
        for(Player p : this.getServer().getOnlinePlayers())
            if(p.getName().equalsIgnoreCase(name)) return true;
        return false;
    }

    public static MotoServer getInstance() {
        return instance;
    }
}