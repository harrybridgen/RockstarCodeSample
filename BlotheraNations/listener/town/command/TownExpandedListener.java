package com.blothera.listener.town.command;

import com.blothera.NationPlugin;
import com.blothera.event.town.TownExpandedEvent;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownExpandedListener implements Listener {

    private final NationPlugin plugin;

    public TownExpandedListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTownExpanded(TownExpandedEvent event) {
        String townName = plugin.getDatabase().getTownDAO().getTownName(event.getTownUuid());

        plugin.getNationLogger().log("Town " + townName + " expanded by " + event.getChunksAdded() + " chunks");
        plugin.getLogger().info("Town " + townName + " expanded by " + event.getChunksAdded() + " chunks");

        Sound sound = Sound.BLOCK_AMETHYST_BLOCK_CHIME;
        World world = event.getLocation().getWorld();
        world.playSound(event.getLocation(), sound, 1.4f, 0.7f);
    }
}
