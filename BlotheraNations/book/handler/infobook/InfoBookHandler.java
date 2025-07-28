package com.blothera.book.handler.infobook;

import com.blothera.NationPlugin;
import com.blothera.book.BookHandler;
import com.blothera.book.BookResult;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

/**
 * Base handler for informational books. These books are updated when a
 * player places or interacts with them on the proper lectern.
 */
public abstract class InfoBookHandler extends BookHandler {

    protected InfoBookHandler(NationPlugin plugin) {
        super(plugin);
    }

    /**
     * Implement this method to specify which book titles this handler will react to.
     * This method should return a list of titles that the handler
     * will respond to when a player interacts with a book on a lectern.
     * <p>
     * You should use or add constants, found in NationConstants.java.
     *
     * @return a list of titles this handler will react to.
     */
    protected abstract List<String> getAcceptedTitles();

    /**
     * Implement this method to check if the lectern is of the correct type
     * <p>
     * You can use the TownLecternDAO, NationLecternDAO or DiplomacyLecternDAO to check, using
     * isTownLectern(), isNationLectern() or isDiplomacyLectern() methods.
     *
     * @param lecternBlock the block representing the lectern
     */
    protected abstract boolean isCorrectLectern(Block lecternBlock);

    @Override
    public boolean shouldHandleBook(BookMeta meta, Block lecternBlock) {
        if (meta.getTitle() == null || !meta.hasTitle()) {
            return false;
        }
        String title = meta.getTitle();
        boolean matches = getAcceptedTitles().stream()
                .anyMatch(t -> t.equalsIgnoreCase(title));
        return matches && isCorrectLectern(lecternBlock);
    }

    @Override
    public BookResult handleBook(Player player, Block lecternBlock, BookMeta meta) {
        return execute(player, lecternBlock, meta);
    }

    /**
     * Implement this method to execute the book's functionality.
     * You should verify the book's contents and context before sending the success book.
     * If there is an error, return a BookResult with the error message by using sendErrorBook()
     * If sucscessful, return a BookResult with the success book by using sendSuccessBook().
     */
    protected abstract BookResult execute(Player player, Block lecternBlock, BookMeta meta);

}
