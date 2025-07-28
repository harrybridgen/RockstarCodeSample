package com.blothera.listener.lectern;

import com.blothera.NationPlugin;
import com.blothera.book.BookRegistry;
import com.blothera.book.BookResult;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.NationDAOs.NationLecternDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class LecternInsertListener implements Listener {

    private final NationLecternDAO nationLecternDAO;
    private final TownLecternDAO townLecternDAO;
    private final DiplomacyLecternDAO diplomacyLecternDAO;

    public LecternInsertListener(NationPlugin plugin) {
        this.nationLecternDAO = plugin.getDatabase().getNationLecternDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
    }

    /**
     * Handles the insertion of a book into a lectern.
     * Checks if the book is a written book and if it belongs to a nation, town, or diplomacy lectern.
     * If so, it processes the book using the BookRegistry.
     *
     * @param event The PlayerInsertLecternBookEvent containing the book and lectern information.
     */
    @EventHandler
    public void onBookInsert(PlayerInsertLecternBookEvent event) {
        ItemStack book = event.getBook();
        if (book.getType() != Material.WRITTEN_BOOK) return;

        if (!(book.getItemMeta() instanceof BookMeta meta)) return;

        Block lecternBlock = event.getBlock();
        Player player = event.getPlayer();

        boolean isNationLectern = nationLecternDAO.isNationLectern(lecternBlock);
        boolean isTownLectern = townLecternDAO.isTownLectern(lecternBlock);
        boolean isDiplomatLectern = diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);

        if (!isNationLectern && !isTownLectern && !isDiplomatLectern) {
            return;
        }

        BookResult result = BookRegistry.handle(player, lecternBlock, meta);
        
        if (result == BookResult.HANDLED_BOOK) {
            World world = lecternBlock.getWorld();
            Location location = lecternBlock.getLocation();
            Sound writeBookSound = Sound.ENTITY_VILLAGER_WORK_CARTOGRAPHER;
            world.playSound(location, writeBookSound, 1.0f, 1.0f);
        }

        if (result == BookResult.ERROR_BOOK) {
            World world = lecternBlock.getWorld();
            Location location = lecternBlock.getLocation();
            Sound errorSound = Sound.ENTITY_VILLAGER_NO;
            world.playSound(location, errorSound, 1.0f, 1.0f);
        }
    }
}
