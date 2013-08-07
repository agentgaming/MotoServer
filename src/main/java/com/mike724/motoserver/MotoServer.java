package com.mike724.motoserver;

import org.bukkit.plugin.java.JavaPlugin;

public class MotoServer extends JavaPlugin {

	@Override
	public void onEnable() {
		this.getLogger().info("MotoServer Enabled");
	}
	
	@Override
	public void onDisable() {
		this.getLogger().info("MotoServer Disabled");
	}
}