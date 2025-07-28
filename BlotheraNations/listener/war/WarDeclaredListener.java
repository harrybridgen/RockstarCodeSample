package com.blothera.listener.war;

import com.blothera.NationPlugin;
import com.blothera.event.diplomacy.WarDeclaredEvent;
import com.blothera.war.BattleManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public class WarDeclaredListener implements Listener {
    private final NationPlugin plugin;
    private final BattleManager battleManager;

    public WarDeclaredListener(NationPlugin plugin, BattleManager battleManager) {
        this.plugin = plugin;
        this.battleManager = battleManager;
    }

    @EventHandler
    public void onWarDeclared(WarDeclaredEvent event) {
        plugin.getWarManager().declareWar(
                event.getSourceNationUuid(),
                event.getTargetNationUuid(),
                event.getLocation()
        );
    }
}

