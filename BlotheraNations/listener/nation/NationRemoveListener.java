package com.blothera.listener.nation;

import com.blothera.NationPlugin;
import com.blothera.event.nation.NationRemovedEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NationRemoveListener implements Listener {

    NationPlugin plugin;

    public NationRemoveListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the NationRemovedEvent to log the removal of a nation and broadcast a message.
     *
     * @param event The NationRemovedEvent containing details about the removed nation.
     */
    @EventHandler
    public void onNationRemoved(NationRemovedEvent event) {
        String nationName = event.getNationName();
        plugin.getNationLogger().log("Deleted nation " + event.getNationUuid());
        plugin.getLogger().info("Deleted nation " + event.getNationUuid());
        if (event.getLastTownName() != null) {
            Bukkit.getServer().broadcastMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Crisis in " + nationName + ". " +
                    "With the fall of " + event.getLastTownName() + ", the nation has faded into history.");
        } else {
            Bukkit.getServer().broadcastMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + nationName + " has faded into history.");
        }
    }
}
