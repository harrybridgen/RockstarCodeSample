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
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import static com.blothera.util.ParticleSpawner.spawnParticles;

public class LecternPlaceListener implements Listener {

    private final NationLecternDAO nationLecternDAO;
    private final TownLecternDAO townLecternDAO;
    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO claimDAO;

    public LecternPlaceListener(NationPlugin plugin) {
        this.nationLecternDAO = plugin.getDatabase().getNationLecternDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.claimDAO = plugin.getDatabase().getTownClaimDAO();
    }

    /**
     * Handles the placement of lecterns and saves their locations to the database.
     * This method checks if the placed item is a lectern and saves its location accordingly.
     *
     * @param event The BlockPlaceEvent triggered when a block is placed.
     */
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) return;

        boolean isNation = NationLecternItem.isNationLectern(item);
        boolean isTown = TownLecternItem.isTownLectern(item);
        boolean isDiplomacy = DiplomacyLecternItem.isDiplomacyLectern(item);

        if (!(isNation || isTown || isDiplomacy)) return;

        var loc = event.getBlock().getLocation();
        World world = loc.getWorld();

        if (world.getEnvironment() != World.Environment.NORMAL) {
            spawnParticles(loc, world);
            event.setCancelled(true);
            return;
        }

        String worldName = world.getName();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        if (isNation) {
            nationLecternDAO.saveLectern(worldName, x, y, z);
        }
        if (isTown) {
            townLecternDAO.saveLectern(worldName, x, y, z);
        }
        if (isDiplomacy) {
            diplomacyLecternDAO.saveLectern(worldName, x, y, z);
        }

        String townId = claimDAO.getTownIdAt(worldName, x >> 4, z >> 4);
        if (townId != null) {
            townDAO.refreshDormancy(townId);
        }
    }
}


