package com.blothera.book.handler.deliverybook.place;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.deliverybook.DeliveryBookPlaceHandler;
import com.blothera.database.DiplomacyDAOs.DiplomacyDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyRequestsDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;
import java.util.UUID;

import static com.blothera.util.NationConstants.*;

public class RequestPeacePlaceHandler extends DeliveryBookPlaceHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final NationDAO nationDAO;
    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final DiplomacyRequestsDAO diplomacyRequestsDAO;
    private final DiplomacyDAO diplomacyDAO;
    private final NationMemberDAO nationMemberDAO;

    public RequestPeacePlaceHandler(NationPlugin plugin) {
        super(plugin);
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.diplomacyRequestsDAO = plugin.getDatabase().getDiplomacyRequestsDAO();
        this.diplomacyDAO = plugin.getDatabase().getDiplomacyDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(PEACE_REQUEST_DELIVERY);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {

        String requestType = getRequestType(meta);
        if (requestType == null || !requestType.equals(PEACE_REQUEST_DELIVERY_TYPE)) {
            return sendErrorBook(lecternBlock, player,
                    "Invalid metadata: delivery type. Contact an admin.");
        }

        String targetNationUuid = getTargetEntityUuid(meta);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "Invalid metadata: target nation. Contact an admin.");
        }

        String sourceNationUuid = getRequestingEntityUuid(meta);
        if (sourceNationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "Invalid metadata: source nation. Contact an admin.");
        }

        String expectedLeaderUuid = nationDAO.getLeaderUuid(sourceNationUuid);
        if (expectedLeaderUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "Could not verify requesting nation's leader. Contact an admin.");
        }

        String playerUuid = player.getUniqueId().toString();
        String playerNationUuid = nationMemberDAO.getNationUuid(playerUuid);
        if (playerNationUuid == null || !playerNationUuid.equals(sourceNationUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "You are not a member of the nation that created this book.");
        }

        String targetNationName = nationDAO.getNationName(targetNationUuid);
        if (targetNationName == null) {
            return sendErrorBook(lecternBlock, player,
                    "The target nation has faded into history.");
        }

        boolean lecternValid = townDAO.getTownsByNationUuid(targetNationUuid).stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));
        if (!lecternValid) {
            return sendErrorBook(lecternBlock, player,
                    "This lectern is not in " + targetNationName + "'s claimed territory.");
        }

        // Check that the author is the current nation leader
        String currentLeaderUuidStr = nationDAO.getLeaderUuid(sourceNationUuid);
        if (currentLeaderUuidStr == null) {
            return sendErrorBook(lecternBlock, player, "Could not determine the current nation leader.");
        }

        String bookAuthor = meta.getAuthor(); // name from the book
        if (bookAuthor == null) {
            return sendErrorBook(lecternBlock, player, "This book has no author.");
        }

        OfflinePlayer authorOffline = Bukkit.getOfflinePlayer(bookAuthor);
        String authorUUID = authorOffline.getUniqueId().toString();

        if (!authorUUID.equals(currentLeaderUuidStr)) {
            String leaderName = Bukkit.getOfflinePlayer(UUID.fromString(currentLeaderUuidStr)).getName();
            if (leaderName == null) leaderName = currentLeaderUuidStr; // fallback

            return sendErrorBook(lecternBlock, player, "This book must be signed by the current nation leader (" + leaderName + ").");
        }

        if (!diplomacyDAO.hasRelation(sourceNationUuid, targetNationUuid, WAR_RELATION)) {
            return sendErrorBook(lecternBlock, player,
                    "Your nations are not currently at war.");
        }

        String sourceNationName = nationDAO.getNationName(sourceNationUuid);
        if (sourceNationName == null) {
            return sendErrorBook(lecternBlock, player,
                    "The requesting nation has faded into history.");
        }

        // Handle mutual request
        boolean reciprocalExists = diplomacyRequestsDAO.hasPendingRequestBetween(targetNationUuid, sourceNationUuid, PEACE_RELATION);
        if (reciprocalExists) {
            diplomacyRequestsDAO.deletePendingRequest(targetNationUuid, sourceNationUuid, PEACE_RELATION);
            diplomacyDAO.removeRelation(sourceNationUuid, targetNationUuid, WAR_RELATION);

            plugin.getNationLogger().log(
                    player.getName() + " from " + sourceNationName + " " +
                            "has achieved peace with " + targetNationName + ".");

            return sendSuccessBook(lecternBlock, player,
                    "Peace Achieved",
                    "§lPeace Achieved§r" +
                            "\n" +
                            getDate() +
                            "\n\n" +
                            "A mutual desire for peace has been recognized." +
                            "\n\n" +
                            "The war with §o" + targetNationName + "§r has ended.",
                    sourceNationName);
        }

        diplomacyRequestsDAO.createRequest(sourceNationUuid, targetNationUuid, PEACE_RELATION);

        return sendSuccessBook(lecternBlock, player,
                "Peace Proposal Sent",
                "§lPeace Proposal§r" +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "An official request for peace has been delivered to §o" + targetNationName + "§r." +
                        "\n\n" +
                        "Their nation must now decide if war will end, or continue to rage.",
                sourceNationName);

    }
}
