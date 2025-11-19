package org.adam.dispenserVillagerTrading;

import org.bukkit.plugin.java.JavaPlugin;

public final class DispenserVillagerTrading extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new DispernserVillagerListener(this), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
