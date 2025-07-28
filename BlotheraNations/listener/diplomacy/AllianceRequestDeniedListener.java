package com.blothera.listener.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.event.diplomacy.AllianceRequestDeniedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AllianceRequestDeniedListener implements Listener {

    private final NationPlugin plugin;

    public AllianceRequestDeniedListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAllianceDenied(AllianceRequestDeniedEvent event) {
        String source = plugin.getDatabase().getNationDAO().getNationName(event.getSourceNationUuid());
        String target = plugin.getDatabase().getNationDAO().getNationName(event.getTargetNationUuid());
        plugin.getNationLogger().log("Alliance request from " + target + " to " + source + " denied");
        plugin.getLogger().info("Alliance request from " + target + " to " + source + " denied");
    }
}
