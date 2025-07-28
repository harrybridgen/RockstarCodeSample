package com.blothera.listener.nation;

import com.blothera.NationPlugin;
import com.blothera.event.nation.NationFormationEvent;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NationFormationListener implements Listener {
    NationPlugin plugin;

    public NationFormationListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNationFormation(NationFormationEvent event) {
        Sound sound = Sound.ITEM_GOAT_HORN_SOUND_1;
        World world = event.getLocation().getWorld();
        world.playSound(event.getLocation(), sound, 1.0f, 0.7f);
        plugin.getNationLogger().log("Nation formation: " + event.getLeaderName() + " has founded the nation " + event.getNationName());
        plugin.getLogger().info("Nation formation: " + event.getLeaderName() + " has founded the nation " + event.getNationName());
    }
}
