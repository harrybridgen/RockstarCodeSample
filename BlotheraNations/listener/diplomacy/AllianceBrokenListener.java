package com.blothera.listener.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.event.diplomacy.AllianceBrokenEvent;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AllianceBrokenListener implements Listener {

    private final NationPlugin plugin;

    public AllianceBrokenListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAllianceBroken(AllianceBrokenEvent event) {
        String source = plugin.getDatabase().getNationDAO().getNationName(event.getSourceNationUuid());
        String target = plugin.getDatabase().getNationDAO().getNationName(event.getTargetNationUuid());

        plugin.getNationLogger().log("Alliance between " + source + " and " + target + " broken");
        plugin.getLogger().info("Alliance between " + source + " and " + target + " broken");

        Sound sound = Sound.BLOCK_GLASS_BREAK;
        World world = event.getLocation().getWorld();
        world.playSound(event.getLocation(), sound, 1.6f, 0.7f);
    }
}
