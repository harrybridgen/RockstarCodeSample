package com.blothera.listener.town.command;

import com.blothera.NationPlugin;
import com.blothera.database.Database;
import com.blothera.event.town.TownRemovedEvent;
import com.blothera.event.nation.NationRemovedEvent;
import com.blothera.event.nation.NationLeaderChangedEvent;
import com.blothera.event.nation.NationCapitalTransferEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.*;

public class TownRemoveEventListener implements Listener {

    private final NationPlugin plugin;
    private final Database database;

    public TownRemoveEventListener(NationPlugin plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabase();
    }

    /**
     * Handles the {@link TownRemovedEvent}, which is triggered when a town is removed from the world.
     * <p>
     * This method performs cleanup and state management for both the town and its associated nation.
     * It removes all claims and members of the town, checks whether the nation still has any towns left,
     * and if not, removes the nation entirely. It also ensures nation leadership and capital status remain valid.
     * <p>
     * Events such as {@link NationRemovedEvent}, {@link NationLeaderChangedEvent}, and {@link NationCapitalTransferEvent}
     * may be triggered as a result of this logic.
     *
     * @param event The {@link TownRemovedEvent} containing information about the removed town.
     */
    @EventHandler
    public void onTownRemoved(TownRemovedEvent event) {
        String townUuid = event.getTownUuid();
        String oldTownName = event.getTownName();
        String nationUuid = event.getNationUuid();
        boolean wasCapital = event.wasCapital();

        database.getTownClaimDAO().removeClaimsForTown(townUuid);
        database.getTownMemberDAO().removeAllTownMembers(townUuid);

        String nationName = database.getNationDAO().getNationName(nationUuid);
        if (nationName == null) {
            plugin.getLogger().warning("Failed to fetch nation name for town " + townUuid + ". " +
                    "This may indicate a data inconsistency.");
            plugin.getNationLogger().log("Failed to fetch nation name for town " + townUuid + ". " +
                    "This may indicate a data inconsistency.");
            return;
        }

        List<String> remainingTowns = database.getTownDAO().getTownsByNationUuid(nationUuid);

        if (remainingTowns.isEmpty()) {
            boolean removed = database.getNationDAO().removeNation(nationUuid);
            if (removed) {
                Bukkit.getPluginManager().callEvent(new NationRemovedEvent(nationUuid, nationName, oldTownName));
            } else {
                plugin.getLogger().warning("Failed to remove nation " + nationName + " after last town " + oldTownName + " was removed.");
                plugin.getNationLogger().log("Failed to remove nation " + nationName + " after last town " + oldTownName + " was removed.");
            }
            return;
        }

        handlePowerTransfer(nationUuid, oldTownName, remainingTowns, wasCapital);

        cleanupDanglingNationMembers(nationUuid);
    }

    private void handlePowerTransfer(String nationUuid, String oldTownName, List<String> remainingTowns, boolean wasCapital) {
        String nationLeaderUuid = database.getNationDAO().getLeaderUuid(nationUuid);
        boolean leaderStillInNation = database.getTownDAO().getTownUuidsByPlayerUuid(nationLeaderUuid).stream()
                .anyMatch(townID -> nationUuid.equals(database.getTownDAO().getNationUuidFromTownUuid(townID)));

        boolean shouldTransferLeader = !leaderStillInNation;

        if (!shouldTransferLeader && !wasCapital) return;

        List<String> topTowns = getMostPopulousTowns(remainingTowns);
        if (topTowns.isEmpty()) return;

        String newCapital = topTowns.size() == 1 ? topTowns.getFirst() : topTowns.get(new Random().nextInt(topTowns.size()));
        String newLeader = database.getTownDAO().getLeaderUuid(newCapital);

        if (newLeader == null) {
            plugin.getLogger().severe("Cannot transfer nation leadership, new capital has no leader!");
            plugin.getNationLogger().log("Nation leadership or capital transfer failed for " + nationUuid +
                    ". New capital " + database.getTownDAO().getTownName(newCapital) + " has no leader.");
            return;
        }

        if (!database.getNationMemberDAO().isMemberOfNation(newLeader, nationUuid)) {
            plugin.getLogger().warning("New nation leader " + newLeader + " is not in the nation member list after transfer!");
            plugin.getNationLogger().log("Nation leadership or capital transfer failed for " + nationUuid +
                    ". New leader " + newLeader + " is not a member of the nation.");
            return;
        }

        if (shouldTransferLeader) {
            database.getNationDAO().changeLeader(nationUuid, newLeader);

            // Fire leadership changed event
            Bukkit.getPluginManager().callEvent(
                    new NationLeaderChangedEvent(nationUuid, nationLeaderUuid, newLeader)
            );

            plugin.getLogger().info("Transferred nation leadership of " + nationUuid + " to " + newLeader);

            if (!nationLeaderUuid.equals(newLeader)) {
                database.getNationMemberDAO().removeMember(nationLeaderUuid);
            }
        }

        if (wasCapital) {
            database.getTownDAO().setCapital(newCapital, nationUuid);
            String newCapitalName = database.getTownDAO().getTownName(newCapital);
            Bukkit.getPluginManager().callEvent(new NationCapitalTransferEvent(nationUuid, oldTownName, newCapitalName));
        }
    }

    private List<String> getMostPopulousTowns(List<String> towns) {
        List<String> topTowns = new ArrayList<>();
        int maxMembers = -1;

        for (String id : towns) {
            int count = database.getTownMemberDAO().countMembers(id);
            if (count < 1) {
                plugin.getLogger().warning("Town " + id + " has no members, skipping for capital transfer.");
                continue;
            }
            if (count > maxMembers) {
                maxMembers = count;
                topTowns.clear();
                topTowns.add(id);
            } else if (count == maxMembers) {
                topTowns.add(id);
            }
        }
        return topTowns;
    }

    private void cleanupDanglingNationMembers(String nationUuid) {
        for (String memberUuid : database.getNationMemberDAO().getMembers(nationUuid)) {
            boolean inNation = database.getTownDAO().getTownUuidsByPlayerUuid(memberUuid).stream()
                    .anyMatch(townId -> nationUuid.equals(database.getTownDAO().getNationUuidFromTownUuid(townId)));
            if (!inNation) {
                plugin.getLogger().info("Removing nation member " + memberUuid + " who no longer has a town in nation " + nationUuid);
                database.getNationMemberDAO().removeMember(memberUuid);
            }
        }
    }
}
