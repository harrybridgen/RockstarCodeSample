package com.blothera.listener.lectern;

import com.blothera.NationPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LecternInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;

import static com.blothera.util.NationConstants.ERROR_BOOK_KEY;

public class LecternPunchListener implements Listener {
    private final NationPlugin plugin;

    public LecternPunchListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the event when a player punches a lectern.
     * If the lectern contains a written book, it will drop the book and clear the lectern's inventory.
     * If the book is an error book, it will not drop it, but will still clear the lectern's inventory.
     *
     * @param event The PlayerInteractEvent triggered by the player punching the lectern.
     */
    @EventHandler
    public void onLecternPunch(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.LECTERN) return;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        // Prevent double-tap abuse using metadata
        if (block.hasMetadata("blothera_lectern_locked")) return;
        block.setMetadata("blothera_lectern_locked", new FixedMetadataValue(plugin, true));

        Lectern lectern = (Lectern) block.getState();
        LecternInventory inventory = (LecternInventory) lectern.getInventory();
        ItemStack heldBook = inventory.getBook();

        if (heldBook == null || heldBook.getType() != Material.WRITTEN_BOOK) {
            block.removeMetadata("blothera_lectern_locked", plugin);
            return;
        }

        if (!(heldBook.getItemMeta() instanceof BookMeta meta)) {
            block.removeMetadata("blothera_lectern_locked", plugin);
            return;
        }

        boolean isErrorBook = meta.getPersistentDataContainer().has(ERROR_BOOK_KEY, PersistentDataType.INTEGER);

        // Schedule the action to occur shortly after (as before)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            inventory.clear();

            Location dropLoc = block.getLocation().add(0.5, 1.1, 0.5);
            World world = block.getWorld();
            Sound pageTurnSound = Sound.ITEM_BOOK_PAGE_TURN;

            if (!isErrorBook) {
                world.dropItemNaturally(dropLoc, heldBook.clone());
            } else {
                world.dropItemNaturally(dropLoc, new ItemStack(Material.WRITABLE_BOOK));
            }

            world.playSound(dropLoc, pageTurnSound, 1.0f, 1.0f);
            block.removeMetadata("blothera_lectern_locked", plugin);

        }, 2L);

        player.swingMainHand();
        event.setCancelled(false);
    }

}
