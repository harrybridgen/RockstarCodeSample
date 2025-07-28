package com.blothera.listener.town.command;

import com.blothera.NationPlugin;
import com.blothera.event.nation.NationLeaderChangedEvent;
import com.blothera.event.nation.NationLeaderExiledEvent;
import com.blothera.event.town.TownMemberExileEvent;
import com.blothera.event.town.TownRemovedEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownMemberExileListener implements Listener {

    private final NationPlugin plugin;

    public TownMemberExileListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMemberExile(TownMemberExileEvent event) {
        String playerUuid = event.getPlayerUuid();
        String townUuid = event.getTownUuid();

        String townName = plugin.getDatabase().getTownDAO().getTownName(townUuid);
        plugin.getNationLogger().log("Player " + playerUuid + " exiled from town " + townName);
        plugin.getLogger().info("Player " + playerUuid + " exiled from town " + townName);

        var db = plugin.getDatabase();

        // --- TOWN LEADER REPLACEMENT ---
        String currentLeader = db.getTownDAO().getLeaderUuid(townUuid);
        if (playerUuid.equals(currentLeader)) {
            var members = db.getTownMemberDAO().getMembers(townUuid);
            members.remove(playerUuid);
            if (!members.isEmpty()) {
                String newLeader = members.getFirst();
                db.getTownDAO().changeLeader(townUuid, newLeader);
                plugin.getLogger().info("Transferred town leadership of " + townName + " to " + newLeader);
            } else {
                // No members left, delete the town and fire TownRemovedEvent
                String nationUuid = db.getTownDAO().getNationUuidFromTownUuid(townUuid);
                String townNameToRemove = db.getTownDAO().getTownName(townUuid);
                boolean wasCapital = db.getTownDAO().isCapital(townUuid);

                boolean removed = db.getTownDAO().removeTown(townUuid);
                if (removed) {
                    Bukkit.getPluginManager().callEvent(
                            new TownRemovedEvent(townUuid, townNameToRemove, nationUuid, wasCapital)
                    );
                    plugin.getLogger().info("Removed town " + townNameToRemove + " due to final exile.");
                    plugin.getNationLogger().log("Removed town " + townNameToRemove + " due to final exile.");
                } else {
                    plugin.getLogger().warning("Failed to remove empty town " + townNameToRemove + " after final exile.");
                    plugin.getNationLogger().log("Failed to remove empty town " + townNameToRemove + " after final exile.");
                }
            }
        }

        // --- NATION LEADER REPLACEMENT ---
        String nationUuid = db.getNationMemberDAO().getNationUuid(playerUuid);
        if (nationUuid != null) {
            String nationLeader = db.getNationDAO().getLeaderUuid(nationUuid);

            if (playerUuid.equals(nationLeader)) {
                // Get capital town
                String capitalTownUuid = db.getNationDAO().getCapitalTownUuid(nationUuid);
                if (capitalTownUuid == null) {
                    plugin.getLogger().warning("Nation " + nationUuid + " has no capital town set. Cannot transfer nation leadership.");
                    return;
                }

                String newNationLeader = db.getTownDAO().getLeaderUuid(capitalTownUuid);

                if (newNationLeader == null || newNationLeader.equals(playerUuid)) {
                    // Try to find another valid leader from any town in the nation
                    for (String townId : db.getTownDAO().getTownsByNationUuid(nationUuid)) {
                        String altLeader = db.getTownDAO().getLeaderUuid(townId);
                        if (altLeader != null && !altLeader.equals(playerUuid)) {
                            newNationLeader = altLeader;
                            plugin.getLogger().warning("Fallback nation leader selected: " + newNationLeader);
                            break;
                        }
                    }
                }

                // Still no valid leader found
                if (newNationLeader == null || newNationLeader.equals(playerUuid)) {
                    plugin.getLogger().warning("Failed to transfer nation leadership: no valid replacement found.");
                    return;
                }
                db.getNationDAO().changeLeader(nationUuid, newNationLeader);
                if ("SYSTEM_BANDIT_FLAG".equals(event.getExiledByUuid())) {
                    Bukkit.getPluginManager().callEvent(
                            new NationLeaderExiledEvent(nationUuid, playerUuid, newNationLeader)
                    );
                } else {
                    Bukkit.getPluginManager().callEvent(
                            new NationLeaderChangedEvent(nationUuid, playerUuid, newNationLeader)
                    );
                }
                plugin.getLogger().info("Transferred nation leadership of " + nationUuid + " to capital leader " + newNationLeader);

                // Final check to remove ex-leader from nation if they no longer belong
                boolean stillInNation = db.getTownDAO().getTownUuidsByPlayerUuid(playerUuid).stream()
                        .anyMatch(id -> nationUuid.equals(db.getTownDAO().getNationUuidFromTownUuid(id)));

                if (!stillInNation) {
                    db.getNationMemberDAO().removeMember(playerUuid);
                    plugin.getLogger().info("Removed ex-leader " + playerUuid + " from nation " + nationUuid + " after leadership transfer.");
                }
            }
        }

    }
}
