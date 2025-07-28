package com.blothera.listener.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.event.diplomacy.AllianceFormedEvent;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AllianceFormedListener implements Listener {

    private final NationPlugin plugin;

    public AllianceFormedListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAllianceFormed(AllianceFormedEvent event) {
        String source = plugin.getDatabase().getNationDAO().getNationName(event.getSourceNationUuid());
        String target = plugin.getDatabase().getNationDAO().getNationName(event.getTargetNationUuid());

        plugin.getNationLogger().log("Alliance formed between " + source + " and " + target);
        plugin.getLogger().info("Alliance formed between " + source + " and " + target);

        Sound sound = Sound.ITEM_GOAT_HORN_SOUND_4;
        World world = event.getLocation().getWorld();
        world.playSound(event.getLocation(), sound, 1.0f, 0.7f);
    }
}
