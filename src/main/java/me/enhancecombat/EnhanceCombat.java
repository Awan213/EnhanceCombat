
package me.enhancecombat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import me.enhancecombat.listeners.CombatListener;

public class EnhanceCombat extends JavaPlugin {
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new CombatListener(), this);
        getLogger().info("EnhanceCombat enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("EnhanceCombat disabled.");
    }
}
