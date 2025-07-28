package com.blothera.listener.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.event.diplomacy.PeaceEstablishedEvent;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PeaceEstablishedListener implements Listener {

    private final NationPlugin plugin;

    public PeaceEstablishedListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPeaceEstablished(PeaceEstablishedEvent event) {
        String source = plugin.getDatabase().getNationDAO().getNationName(event.getSourceNationUuid());
        String target = plugin.getDatabase().getNationDAO().getNationName(event.getTargetNationUuid());

        plugin.getNationLogger().log("Peace established between " + source + " and " + target);
        plugin.getLogger().info("Peace established between " + source + " and " + target);

        Sound sound = Sound.ITEM_GOAT_HORN_SOUND_7;
        World world = event.getLocation().getWorld();
        world.playSound(event.getLocation(), sound, 1.0f, 0.7f);
    }
}
