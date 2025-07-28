package com.blothera.listener.war;

import com.blothera.NationPlugin;
import com.blothera.war.ActiveBattle;
import com.blothera.war.BattleArenaUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Map;

public class WarBattleListener implements Listener {

    private final NationPlugin plugin;

    public WarBattleListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        String killerId = killer.getUniqueId().toString();
        String victimId = victim.getUniqueId().toString();

        Location deathLoc = victim.getLocation();

        for (Map.Entry<Integer, ActiveBattle> entry : plugin.getBattleManager().getActiveBattles().entrySet()) {
            Location center = entry.getValue().getArenaCenter();
            if (BattleArenaUtil.isInsideArenaChunk(center, deathLoc)) {
                plugin.getBattleManager().recordKill(entry.getKey(), killerId, victimId);
                break;
            }
        }
    }
}
