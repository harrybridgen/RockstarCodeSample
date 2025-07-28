package com.blothera.tax;

import com.blothera.NationPlugin;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import com.blothera.event.town.TownRemovedEvent;
import org.bukkit.Bukkit;

import java.util.List;

import static com.blothera.util.NationConstants.*;

public class TownTaxTask {
    private final NationPlugin plugin;
    private final TownDAO townDAO;
    private final TownClaimDAO claimDAO;
    private final TownLecternDAO lecternDAO;


    public TownTaxTask(NationPlugin plugin) {
        this.plugin = plugin;
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.claimDAO = plugin.getDatabase().getTownClaimDAO();
        this.lecternDAO = plugin.getDatabase().getTownLecternDAO();
        scheduleTaxCheck();
    }

    /**
     * Schedules a repeating task to check towns for tax payments and dormancy.
     * If a town has not paid taxes and does not have a lectern, it is marked as dormant.
     * If a town has been dormant for a specified number of days, it is removed.
     */
    private void scheduleTaxCheck() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            plugin.getLogger().info("Checking towns for tax payments and dormancy...");
            List<String> towns = townDAO.getAllTownUuids();
            java.time.LocalDate today = java.time.LocalDate.now();
            for (String townUuid : towns) {
                String paidUntil = townDAO.getTaxPaidUntil(townUuid);
                if (paidUntil != null) {
                    try {
                        java.time.LocalDate date = java.time.LocalDate.parse(paidUntil);

                        if (date.plusDays(DORMANT_DAYS_TO_DELETE_TOWN).isBefore(today)) {
                            String name = townDAO.getTownName(townUuid);
                            String nationId = townDAO.getNationUuidFromTownUuid(townUuid);
                            boolean wasCapital = townDAO.isCapital(townUuid);
                            boolean didRemoveTown = townDAO.removeTown(townUuid);
                            if (didRemoveTown) {
                                Bukkit.getPluginManager().callEvent(new TownRemovedEvent(townUuid, name, nationId, wasCapital));
                                plugin.getLogger().info("Removed dormant town " + name + " after " + DORMANT_DAYS_TO_DELETE_TOWN + " days of inactivity.");
                                plugin.getNationLogger().log("Removed dormant town " + name + " after " + DORMANT_DAYS_TO_DELETE_TOWN + " days of inactivity.");
                            }
                            continue;
                        }
                    } catch (Exception ignored) {
                        plugin.getLogger().warning("Failed to parse tax paid date for town " + townUuid + ": " + paidUntil);
                    }
                }

                townDAO.refreshDormancy(townUuid);
            }
        }, TASK_INIT_DELAY_TICKS, TAX_TASK_INTERVAL_TICKS);
    }
}
