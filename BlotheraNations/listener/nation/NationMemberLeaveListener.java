package com.blothera.listener.nation;

import com.blothera.NationPlugin;
import com.blothera.event.nation.NationMemberLeaveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NationMemberLeaveListener implements Listener {

    private final NationPlugin plugin;

    public NationMemberLeaveListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMemberLeave(NationMemberLeaveEvent event) {
        String nationName = plugin.getDatabase().getNationDAO().getNationName(event.getNationUuid());
        plugin.getNationLogger().log("Player " + event.getPlayerUuid() + " left nation " + nationName);
        plugin.getLogger().info("Player " + event.getPlayerUuid() + " left nation " + nationName);
    }
}
