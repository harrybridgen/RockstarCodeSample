package com.blothera.listener.lectern;

import com.blothera.NationPlugin;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.NationDAOs.NationLecternDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.item.DiplomacyLecternItem;
import com.blothera.item.NationLecternItem;
import com.blothera.item.TownLecternItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class LecternBreakListener implements Listener {

    private final NationLecternDAO nationLecternDAO;
    private final TownLecternDAO townLecternDAO;
    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO claimDAO;

    public LecternBreakListener(NationPlugin plugin) {
        this.nationLecternDAO = plugin.getDatabase().getNationLecternDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.claimDAO = plugin.getDatabase().getTownClaimDAO();
    }

    /**
     * Handles the breaking of lecterns, dropping custom items based on the type of lectern.
     *
     * @param event The BlockBreakEvent triggered when a lectern is broken.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onLecternBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.LECTERN) {
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        Location loc = block.getLocation();
        String world = loc.getWorld().getName();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        boolean modified = false;
        if (nationLecternDAO.isNationLectern(block)) {
            handleLecternBreak(event, loc, NationLecternItem.create());
            nationLecternDAO.deleteLectern(world, x, y, z);
            modified = true;
        }

        if (townLecternDAO.isTownLectern(block)) {
            handleLecternBreak(event, loc, TownLecternItem.create());
            townLecternDAO.deleteLectern(world, x, y, z);
            modified = true;
        }

        if (diplomacyLecternDAO.isDiplomacyLectern(block)) {
            handleLecternBreak(event, loc, DiplomacyLecternItem.create());
            diplomacyLecternDAO.deleteLectern(world, x, y, z);
            modified = true;
        }

        if (modified) {
            String townId = claimDAO.getTownIdAt(world, x >> 4, z >> 4);
            if (townId != null) {
                townDAO.refreshDormancy(townId);
            }
        }
    }

    private void handleLecternBreak(BlockBreakEvent event, Location loc, org.bukkit.inventory.ItemStack customItem) {
        event.setDropItems(false);
        loc.getWorld().dropItemNaturally(loc, customItem);
    }

}


