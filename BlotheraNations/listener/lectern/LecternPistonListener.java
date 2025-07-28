package com.blothera.listener.lectern;

import com.blothera.NationPlugin;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.NationDAOs.NationLecternDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public class LecternPistonListener implements Listener {

    private final NationLecternDAO nationLecternDAO;
    private final TownLecternDAO townLecternDAO;
    private final DiplomacyLecternDAO diplomacyLecternDAO;

    public LecternPistonListener(NationPlugin plugin) {
        this.nationLecternDAO = plugin.getDatabase().getNationLecternDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
    }

    /**
     * Checks if the given block is a protected lectern.
     * A lectern is considered protected if it belongs to a nation, town, or diplomacy.
     *
     * @param block The block to check.
     * @return true if the block is a protected lectern, false otherwise.
     */
    private boolean isProtectedLectern(Block block) {
        if (block.getType() != Material.LECTERN) return false;
        return nationLecternDAO.isNationLectern(block) ||
                townLecternDAO.isTownLectern(block) ||
                diplomacyLecternDAO.isDiplomacyLectern(block);
    }

    /**
     * Handles the piston extend event.
     * If any of the blocks being pushed are protected lecterns, the event is cancelled.
     *
     * @param event The piston extend event.
     */
    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (isProtectedLectern(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Handles the piston retract event.
     * If any of the blocks being pulled are protected lecterns, the event is cancelled.
     *
     * @param event The piston retract event.
     */
    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (isProtectedLectern(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
