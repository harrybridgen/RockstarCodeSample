package com.blothera.listener.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.event.diplomacy.PeaceRequestDeniedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PeaceRequestDeniedListener implements Listener {

    private final NationPlugin plugin;

    public PeaceRequestDeniedListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPeaceDenied(PeaceRequestDeniedEvent event) {
        String source = plugin.getDatabase().getNationDAO().getNationName(event.getSourceNationUuid());
        String target = plugin.getDatabase().getNationDAO().getNationName(event.getTargetNationUuid());
        plugin.getNationLogger().log("Peace request from " + target + " to " + source + " denied");
        plugin.getLogger().info("Peace request from " + target + " to " + source + " denied");
    }
}
