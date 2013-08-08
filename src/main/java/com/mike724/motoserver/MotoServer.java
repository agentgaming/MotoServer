package com.mike724.motoserver;

import com.mike724.motoapi.push.MotoPush;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class MotoServer extends JavaPlugin {

    private static MotoServer instance;

    private MotoPush motoPush;

	@Override
	public void onEnable() {
        instance = this;

        try {
            motoPush = new MotoPush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        getServer().getPluginManager().registerEvents(new MotoEvents(), this);
        getCommand("net").setExecutor(new MotoCommands(this));

        this.getLogger().info("MotoServer Enabled");
	}
	
	@Override
	public void onDisable() {
		this.getLogger().info("MotoServer Disabled");
	}

    public MotoPush getMotoPush() {
        return motoPush;
    }

    public static MotoServer getInstance() {
        return instance;
    }
}