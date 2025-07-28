package com.blothera.book.handler.deliverybook.create;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.deliverybook.DeliveryBookCreateHandler;
import com.blothera.database.DiplomacyDAOs.DiplomacyDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyRequestsDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.*;

public class RequestPeaceCreateHandler extends DeliveryBookCreateHandler {

    private final NationDAO nationDAO;
    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final DiplomacyDAO diplomacyDAO;
    private final DiplomacyRequestsDAO diplomacyRequestsDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;

    public RequestPeaceCreateHandler(NationPlugin plugin) {
        super(plugin);
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.diplomacyDAO = plugin.getDatabase().getDiplomacyDAO();
        this.diplomacyRequestsDAO = plugin.getDatabase().getDiplomacyRequestsDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(
                REQUEST_PEACE_COMMAND,
                PEACE_COMMAND,
                OFFER_PEACE_COMMAND
        );
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String playerUuid = player.getUniqueId().toString();
        String sourceNationUuid = nationMemberDAO.getNationUuid(playerUuid);

        if (sourceNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "You are not in a nation.");
        }

        String sourceNationName = nationDAO.getNationName(sourceNationUuid);
        if (sourceNationName == null) {
            return sendErrorBook(lecternBlock, player, "Your nation does not exist or has been deleted. Contact an admin.");
        }

        if (!playerUuid.equals(nationDAO.getLeaderUuid(sourceNationUuid))) {
            return sendErrorBook(lecternBlock, player, "Only the nation leader can request peace.");
        }

        // Validate that the lectern is in a valid town chunk of the player's nation
        boolean validChunk = townDAO
                .getTownsByNationUuid(sourceNationUuid)
                .stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));
        if (!validChunk) {
            return sendErrorBook(lecternBlock, player, "This lectern is not inside any of your nation's towns.");
        }

        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "Book must include target nation name. Write it on the first line of the book.");
        }

        String targetNationName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (targetNationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Target nation name cannot be empty. Write it on the first line of the book.");
        }

        String targetNationUuid = nationDAO.getNationUUIDByName(targetNationName);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "No nation by that name exists.");
        }

        targetNationName = nationDAO.getCanonicalName(targetNationName);
        if (targetNationName == null) {
            return sendErrorBook(lecternBlock, player, "Could not get canonical name for target nation. Please contact an admin.");
        }

        if (sourceNationUuid.equals(targetNationUuid)) {
            return sendErrorBook(lecternBlock, player, "You can’t request peace with yourself.");
        }

        if (diplomacyRequestsDAO.hasPendingRequestBetween(sourceNationUuid, targetNationUuid, PEACE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You already have a pending peace request to " + targetNationName + ".");
        }

        if (!diplomacyDAO.hasRelation(sourceNationUuid, targetNationUuid, WAR_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You are not at war with " + targetNationName + ".");
        }

        if (diplomacyDAO.hasRelation(sourceNationUuid, targetNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You are currently allied with " + targetNationName + ". You cannot request peace.");
        }

        // Check for mutual request
        if (diplomacyRequestsDAO.hasPendingRequestBetween(targetNationUuid, sourceNationUuid, PEACE_RELATION)) {
            diplomacyRequestsDAO.deletePendingRequest(targetNationUuid, sourceNationUuid, PEACE_RELATION);
            diplomacyDAO.removeRelation(sourceNationUuid, targetNationUuid, WAR_RELATION);
            return sendSuccessBook(lecternBlock, player,
                    "Peace Achieved",
                    "§lPeace Achieved§r" +
                            "\n" +
                            getDate() +
                            "\n\n" +
                            "Both sides have shown a willingness to end the conflict." +
                            "\n\n" +
                            "The war with §o" + targetNationName + "§r has ended.",
                    sourceNationName);
        }

        // Create Peace Book
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
        bookMeta.setTitle(PEACE_REQUEST_DELIVERY);
        bookMeta.setAuthor(player.getName());
        bookMeta.addPage("§lPeace Offer§r" +
                "\n" +
                getDate() +
                "\n\n" +
                "To the leaders of §o" + targetNationName + "§r," +
                "\n\n" +
                "We propose an end to the war between our nations." +
                "\n\n" +
                "Place this in their Diplomacy Lectern to initiate peace.");

        bookMeta.setLore(List.of("§7" + sourceNationName));

        plugin.getNationLogger().log(player.getName() + " made a request peace delivery book, targeting " + targetNationName + ".");

        setDeliveryKeys(bookMeta, sourceNationUuid, targetNationUuid, PEACE_REQUEST_DELIVERY_TYPE);
        book.setItemMeta(bookMeta);
        return sendSuccessBook(lecternBlock, player, book);
    }
}
