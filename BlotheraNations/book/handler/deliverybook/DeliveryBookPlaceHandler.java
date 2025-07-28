package com.blothera.book.handler.deliverybook;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.blothera.util.NationConstants.*;

/**
 * Base handler for delivery book placement. These handlers handle the logic
 * when a delivery book is placed on a lectern, executing the request contained
 * within the book.
 */
public abstract class DeliveryBookPlaceHandler extends CommandBookHandler {

    protected DeliveryBookPlaceHandler(NationPlugin plugin) {
        super(plugin);
    }

    /**
     * Checks if the book is valid for this handler.
     * Validates the book title, checks if it matches the accepted titles,
     * and ensures the lectern is of the correct type.
     *
     * @param meta         book metadata
     * @param lecternBlock lectern block where the book is placed
     * @return true if the book should be handled by this handler
     */
    @Override
    public boolean shouldHandleBook(BookMeta meta, Block lecternBlock) {
        if (!super.shouldHandleBook(meta, lecternBlock)) {
            return false;
        }

        return meta.getPersistentDataContainer().has(DELIVERY_TYPE_KEY, PersistentDataType.STRING);
    }

    /**
     * @return titles that trigger this delivery book handler.
     */
    protected abstract List<String> getAcceptedTitles();

    /**
     * Checks that the lectern is of the proper type.
     * This is used to ensure that the book is placed on the lectern.
     */
    protected abstract boolean isCorrectLectern(Block lecternBlock);

    /**
     * Execute the delivery request when validation succeeds.
     */
    protected abstract BookResult execute(Player player, Block lecternBlock, BookMeta meta);

    /**
     * Retrieves the requesting entity from the book metadata.
     * Typically used to identify which nation or town made the request contained in the book.
     *
     * @param meta the book metadata
     * @return the UUID of the requesting entity, or null if not set
     */
    @Nullable
    protected String getRequestingEntityUuid(BookMeta meta) {
        return meta.getPersistentDataContainer().get(
                REQUESTING_ENTITY_KEY,
                PersistentDataType.STRING
        );
    }

    /**
     * Retrieves the target entity from the book metadata.
     * This is used to identify the entity that the delivery request is directed towards.
     *
     * @param meta the book metadata
     * @return the UUID of the target entity, or null if not set
     */
    @Nullable
    protected String getTargetEntityUuid(BookMeta meta) {
        return meta.getPersistentDataContainer().get(
                TARGET_ENTITY_KEY,
                PersistentDataType.STRING
        );
    }

    /**
     * Retrieves the request type from the book metadata.
     * This is used to determine what kind of delivery request is being made.
     *
     * @param meta the book metadata
     * @return the request type as a string, or null if not set
     */
    @Nullable
    protected String getRequestType(BookMeta meta) {
        return meta.getPersistentDataContainer().get(
                DELIVERY_TYPE_KEY,
                PersistentDataType.STRING
        );
    }
}
