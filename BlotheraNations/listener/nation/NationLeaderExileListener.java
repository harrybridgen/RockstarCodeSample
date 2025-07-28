package com.blothera.listener.nation;

import com.blothera.NationPlugin;
import com.blothera.event.nation.NationLeaderExiledEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class NationLeaderExileListener implements Listener {

    private final NationPlugin plugin;

    public NationLeaderExileListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNationLeaderExiled(NationLeaderExiledEvent event) {
        String nationName = plugin.getDatabase().getNationDAO().getNationName(event.getNationUuid());

        OfflinePlayer oldLeader = Bukkit.getOfflinePlayer(UUID.fromString(event.getOldLeaderUuid()));
        OfflinePlayer newLeader = Bukkit.getOfflinePlayer(UUID.fromString(event.getNewLeaderUuid()));

        plugin.getNationLogger().log(
                nationName + " Nation leader " + oldLeader.getName() + " was exiled for banditry. " +
                        "Power transferred to " + newLeader.getName()
        );

        plugin.getLogger().info(
                nationName + " Nation leader " + oldLeader.getName() + " was exiled for banditry. " +
                        "Power transferred to " + newLeader.getName()
        );

        // Optional: broadcast to server
        Bukkit.broadcastMessage("§7§o" + oldLeader.getName() + " was exiled from the leadership of " +
                nationName + ". Power now rests with " + newLeader.getName() + ".");
    }
}
