package com.blothera.listener.town.command;

import com.blothera.NationPlugin;
import com.blothera.event.town.TownJoinRequestAcceptedEvent;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownJoinRequestAcceptedListener implements Listener {

    private final NationPlugin plugin;

    public TownJoinRequestAcceptedListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoinAccepted(TownJoinRequestAcceptedEvent event) {
        String townName = plugin.getDatabase().getTownDAO().getTownName(event.getTownUuid());
        plugin.getNationLogger().log("Player " + event.getPlayerUuid() + " accepted into town " + townName);
        plugin.getLogger().info("Player " + event.getPlayerUuid() + " accepted into town " + townName);
        Player player = plugin.getServer().getPlayer(event.getPlayerUuid());
        if (player != null && player.isOnline()) {
            String message = "§2You have been accepted into the town of §6" + townName;
            player.sendActionBar(message);
            Sound sound = Sound.ENTITY_VILLAGER_TRADE;
            World world = player.getWorld();
            world.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }
}
