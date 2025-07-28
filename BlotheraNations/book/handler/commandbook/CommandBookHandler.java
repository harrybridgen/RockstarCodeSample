package com.blothera.book.handler.commandbook;

import com.blothera.NationPlugin;
import com.blothera.book.BookHandler;
import com.blothera.book.BookResult;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

/**
 * Base handler for command books which execute an action immediately
 * when placed on a lectern.
 */
public abstract class CommandBookHandler extends BookHandler {

    protected CommandBookHandler(NationPlugin plugin) {
        super(plugin);
    }

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

    /**
     * @return the list of book titles this handler responds to.
     */
    protected abstract List<String> getAcceptedTitles();

    /**
     * Checks if the given lectern is valid for the command.
     */
    protected abstract boolean isCorrectLectern(Block lecternBlock);

    /**
     * Execute the command associated with the book.
     * This method should contain the logic for what happens
     * when the book is placed on the lectern.
     *
     * @param player       the player who placed the book
     * @param lecternBlock the lectern block where the book was placed
     * @param meta         the book metadata
     * @return a BookResult indicating the outcome of the command execution
     */
    protected abstract BookResult execute(Player player, Block lecternBlock, BookMeta meta);

    /**
     * Handles the command book when placed on a lectern.
     * Validates the book and executes the command if valid.
     *
     * @param player       the player who placed the book
     * @param lecternBlock the lectern block where the book was placed
     * @param meta         the book metadata
     * @return a BookResult indicating the outcome of the command execution
     */
    /**
     * Validate the command book before executing it. Subclasses can override
     * this method to perform pre-checks that do not require parsing any data
     * later used in {@link #execute}.
     *
     * @return {@code null} if validation passes, otherwise a {@link BookResult}
     * describing the failure.
     */
    protected BookResult validate(Player player, Block lecternBlock, BookMeta meta) {
        return null;
    }

    @Override
    public BookResult handleBook(Player player, Block lecternBlock, BookMeta meta) {
        
        BookResult validation = validate(player, lecternBlock, meta);

        if (validation != null) {
            return validation;
        }

        return execute(player, lecternBlock, meta);
    }

}
