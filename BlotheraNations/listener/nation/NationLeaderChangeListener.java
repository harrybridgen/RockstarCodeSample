package com.blothera.listener.nation;

import com.blothera.NationPlugin;
import com.blothera.event.nation.NationLeaderChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class NationLeaderChangeListener implements Listener {

    NationPlugin plugin;

    public NationLeaderChangeListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the NationLeaderChangedEvent to log the change of nation leadership.
     *
     * @param event The NationLeaderChangedEvent containing details about the leadership change.
     */
    @EventHandler
    public void onNationLeaderChanged(NationLeaderChangedEvent event) {
        String nationName = plugin.getDatabase().getNationDAO().getNationName(event.getNationUuid());
        OfflinePlayer newLeader = Bukkit.getOfflinePlayer(UUID.fromString(event.getNewLeaderUuid()));
        plugin.getNationLogger().log(nationName + " Nation " + event.getNationUuid() + " leadership transferred to " + newLeader.getName());
        plugin.getLogger().info(nationName + " Nation " + event.getNationUuid() + " leadership transferred to " + newLeader.getName());
        Bukkit.broadcastMessage("§7§o" + nationName + " now recognizes " + newLeader.getName() + " as its new leader.");
    }
}
