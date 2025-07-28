package com.blothera.book.handler.deliverybook;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

import static com.blothera.util.NationConstants.*;

/**
 * Base handler for delivery book creation. These handlers turn a written
 * book into a delivery book that can later be placed on the target lectern.
 */
public abstract class DeliveryBookCreateHandler extends CommandBookHandler {

    protected DeliveryBookCreateHandler(NationPlugin plugin) {
        super(plugin);
    }

    /**
     * Checks if the book is valid for this handler.
     * Validates the book title and checks if it matches the accepted titles.
     * Also checks if the lectern is of the correct type.
     *
     * @param meta         book metadata
     * @param lecternBlock lectern block where the book is placed
     * @return true if the book should be handled by this handler
     */
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
     * @return titles that trigger this creation handler.
     */
    protected abstract List<String> getAcceptedTitles();

    /**
     * Checks if the lectern is correct for creating this delivery book.
     */
    protected abstract boolean isCorrectLectern(Block lecternBlock);

    /**
     * Perform the creation logic when validation passes.
     */
    protected abstract BookResult execute(Player player, Block lecternBlock, BookMeta meta);


    /**
     * Sets the delivery keys in the book metadata.
     * This method is used to set the requesting entity UUID, target entity UUID,
     * and the type of request (delivery type).
     *
     * @param meta                 the book metadata
     * @param requestingEntityUuid the UUID of the entity making the request
     * @param targetEntityUuid     the UUID of the entity receiving the request
     * @param requestType          the type of delivery request
     */
    protected void setDeliveryKeys(BookMeta meta, String requestingEntityUuid, String targetEntityUuid, String requestType) {
        setRequestingEntityUuid(meta, requestingEntityUuid);
        setTargetEntityUuid(meta, targetEntityUuid);
        setDeliveryType(meta, requestType);
    }

    private void setRequestingEntityUuid(BookMeta meta, String requestingEntityUuid) {
        meta.getPersistentDataContainer().set(
                REQUESTING_ENTITY_KEY,
                PersistentDataType.STRING,
                requestingEntityUuid
        );
    }


    private void setTargetEntityUuid(BookMeta meta, String targetEntityUuid) {
        meta.getPersistentDataContainer().set(
                TARGET_ENTITY_KEY,
                PersistentDataType.STRING,
                targetEntityUuid
        );
    }

    private void setDeliveryType(BookMeta meta, String requestType) {
        meta.getPersistentDataContainer().set(
                DELIVERY_TYPE_KEY,
                PersistentDataType.STRING,
                requestType
        );
    }
}
