package com.mike724.motoserver;

import com.mike724.motoapi.push.MotoPush;
import com.mike724.motoapi.storage.DataStorage;
import com.mike724.motoapi.storage.defaults.NetworkPlayer;
import org.apache.commons.io.IOUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

public class MotoServer extends JavaPlugin {

    private static MotoServer instance;

    private MotoPush motoPush;

    private HashMap<String,NetworkPlayer> networkPlayers = new HashMap<>();

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

        //Setup MotoPush
        try {
            motoPush = new MotoPush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Setup the event listener
        getServer().getPluginManager().registerEvents(new MotoEvents(), this);

        //Setup the network command listener
        getCommand("net").setExecutor(new MotoCommands(this));

        this.getLogger().info("MotoServer Enabled");
	}
	
	@Override
	public void onDisable() {
        //Set all players to offline when the server goes down
        HashMap<Object,String> playerObjects = new HashMap<>();
        for(NetworkPlayer np : networkPlayers.values()) {
            np.setOnline(false);
            playerObjects.put((Object) np, np.getPlayer());
        }
        this.getDataStorage().writeObjects(playerObjects);

        //Uh, oh
        this.getLogger().info("MotoServer Disabled");

        //If we go down, the server does too!!
        this.getServer().shutdown();
	}

    public MotoPush getMotoPush() {
        return motoPush;
    }

    public DataStorage getDataStorage() {
        return null;
    }

    protected void addNetworkPlayer(NetworkPlayer np) {
        networkPlayers.put(np.getPlayer(),np);
    }

    protected void removeNetworkPlayer(String p) {
        networkPlayers.remove(p);
    }

    public HashMap<String,NetworkPlayer> getNetworkPlayers() {
        return networkPlayers;
    }

    public String getExternalIP() {
        return externalIP;
    }

    public static MotoServer getInstance() {
        return instance;
    }
}