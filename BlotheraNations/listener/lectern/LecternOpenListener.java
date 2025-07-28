package com.blothera.listener.lectern;

import com.blothera.NationPlugin;
import com.blothera.book.BookRegistry;
import com.blothera.book.BookResult;
import com.blothera.database.TownDAOs.TownClaimDAO;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LecternInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import static com.blothera.util.NationConstants.ERROR_BOOK_KEY;

public class LecternOpenListener implements Listener {

    private final NationPlugin plugin;
    TownClaimDAO townClaimDAO;

    public LecternOpenListener(NationPlugin plugin) {
        this.plugin = plugin;
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
    }

    /**
     * Handles the opening of a lectern by a player.
     * If the lectern contains a book with a specific error tag, it opens the book for the player.
     * If the book is handled by the BookRegistry, it cancels the event to prevent default behavior.
     *
     * @param event The PlayerInteractEvent triggered when a player interacts with a block.
     */
    @EventHandler
    public void onLecternOpen(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LECTERN) return;

        Lectern lectern = (Lectern) block.getState();
        ItemStack lecternBook = ((LecternInventory) lectern.getInventory()).getBook();
        Player player = event.getPlayer();

        boolean isNationLectern = plugin.getDatabase().getNationLecternDAO().isNationLectern(block);
        boolean isTownLectern = plugin.getDatabase().getTownLecternDAO().isTownLectern(block);
        boolean isDiplomatLectern = plugin.getDatabase().getDiplomacyLecternDAO().isDiplomacyLectern(block);

        if (!isNationLectern && !isTownLectern && !isDiplomatLectern) {
            return;
        }

        String world = block.getWorld().getName();
        int x = block.getX() >> 4;
        int z = block.getZ() >> 4;

        // Do not allow interactions with town lecterns that are no longer in claimed chunks.
        // Prevents processing/rewriting of historical books in fallen towns or nations.
        if (!townClaimDAO.isChunkClaimed(world, x, z)) {
            return;
        }


        if (lecternBook != null && lecternBook.getType() == Material.WRITTEN_BOOK) {
            BookMeta meta = (BookMeta) lecternBook.getItemMeta();
            if (meta != null) {
                boolean isError = meta.getPersistentDataContainer().has(
                        ERROR_BOOK_KEY,
                        PersistentDataType.INTEGER
                );

                if (isError) {
                    LecternInventory inv = (LecternInventory) lectern.getInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.openBook(lecternBook);
                        inv.setItem(0, lecternBook);
                    }, 2L); // 2 ticks = ~0.1 seconds

                    event.setCancelled(true);
                    return;
                }


                BookResult result = BookRegistry.handle(player, block, meta);
                if (result != BookResult.NOT_HANDLED) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
