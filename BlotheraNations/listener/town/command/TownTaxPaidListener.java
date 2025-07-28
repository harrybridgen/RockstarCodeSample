package com.blothera.listener.town.command;

import com.blothera.NationPlugin;
import com.blothera.event.town.TownTaxPaidEvent;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownTaxPaidListener implements Listener {

    private final NationPlugin plugin;

    public TownTaxPaidListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTaxPaid(TownTaxPaidEvent event) {
        String townName = plugin.getDatabase().getTownDAO().getTownName(event.getTownUuid());

        plugin.getNationLogger().log("Town " + townName + " paid tax of " + event.getAmount() + " emeralds");
        plugin.getLogger().info("Town " + townName + " paid tax of " + event.getAmount() + " emeralds");

        Sound sound = Sound.BLOCK_AMETHYST_BLOCK_RESONATE;
        World world = event.getLocation().getWorld();
        world.playSound(event.getLocation(), sound, 1.4f, 0.7f);
    }
}
