package com.blothera.listener.town.command;

import com.blothera.NationPlugin;
import com.blothera.event.town.TownFormationEvent;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownFormationListener implements Listener {

    private final NationPlugin plugin;

    public TownFormationListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTownFormation(TownFormationEvent event) {

        World world = event.getLocation().getWorld();
        Location location = event.getLocation();
        Sound sound = Sound.ITEM_GOAT_HORN_SOUND_0;
        world.playSound(location, sound, 1.0f, 0.7f);

        String nationName = plugin.getDatabase().getNationDAO().getNationName(event.getNationUuid());
        plugin.getNationLogger().log("Town " + event.getTownName() + " founded in " + nationName);
        plugin.getLogger().info("Town " + event.getTownName() + " founded in " + nationName);
    }
}
