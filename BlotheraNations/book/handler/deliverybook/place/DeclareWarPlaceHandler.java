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
import com.blothera.event.diplomacy.WarDeclaredEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;
import java.util.UUID;

import static com.blothera.util.NationConstants.*;

public class DeclareWarPlaceHandler extends DeliveryBookPlaceHandler {

    private final NationDAO nationDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final DiplomacyDAO diplomacyDAO;
    private final DiplomacyRequestsDAO diplomacyRequestsDAO;

    public DeclareWarPlaceHandler(NationPlugin plugin) {
        super(plugin);
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.diplomacyDAO = plugin.getDatabase().getDiplomacyDAO();
        this.diplomacyRequestsDAO = plugin.getDatabase().getDiplomacyRequestsDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(WAR_DECLARATION_DELIVERY);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {

        String requestType = getRequestType(meta);
        if (requestType == null || !requestType.equals(DECLARE_WAR_DELIVERY_TYPE)) {
            return sendErrorBook(lecternBlock, player, "Invalid war declaration book.");
        }

        String sourceNationUuid = getRequestingEntityUuid(meta);
        if (sourceNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "Missing declaring nation metadata.");
        }

        String sourceNationName = nationDAO.getNationName(sourceNationUuid);
        if (sourceNationName == null) {
            return sendErrorBook(lecternBlock, player, "Your nation does not exist or has been deleted. Contact an admin.");
        }

        String targetNationUuid = getTargetEntityUuid(meta);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "Target nation no longer exists.");
        }

        String targetNationName = nationDAO.getNationName(targetNationUuid);
        if (targetNationName == null) {
            return sendErrorBook(lecternBlock, player, "Target nation name could not be retrieved.");
        }

        String playerUuid = player.getUniqueId().toString();
        String playerNationUuid = nationMemberDAO.getNationUuid(playerUuid);
        if (playerNationUuid == null || !playerNationUuid.equals(sourceNationUuid)) {
            return sendErrorBook(lecternBlock, player, "Only members of the declaring nation may place this book.");
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

        if (diplomacyDAO.hasRelation(sourceNationUuid, targetNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You are currently allied with " + targetNationName + ". You must break the alliance first.");
        }

        if (diplomacyDAO.hasRelation(sourceNationUuid, targetNationUuid, WAR_RELATION)) {
            return sendErrorBook(lecternBlock, player, "War is already declared between your nations.");
        }

        // Check if the lectern is inside the target nation's claimed territory
        boolean isTargetLectern = townDAO
                .getTownsByNationUuid(targetNationUuid)
                .stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));

        if (!isTargetLectern) {
            return sendErrorBook(lecternBlock, player, "This lectern is not inside " + targetNationName + "'s claimed territory.");
        }

        if (sourceNationUuid.equals(targetNationUuid)) {
            return sendErrorBook(lecternBlock, player, "You cannot declare war on your own nation.");
        }
        int sourceWarId = plugin.getDatabase().getWarDAO().getOngoingWarIdByNation(sourceNationUuid);
        int targetWarId = plugin.getDatabase().getWarDAO().getOngoingWarIdByNation(targetNationUuid);
        if (sourceWarId != -1 || targetWarId != -1) {
            return sendErrorBook(lecternBlock, player,
                    "Either your nation or " + targetNationName + " is already involved in another war.\n" +
                            "Only one war at a time is allowed per nation.");
        }

        diplomacyRequestsDAO.deleteAllRequests(sourceNationUuid, targetNationUuid);
        diplomacyDAO.createRelation(sourceNationUuid, targetNationUuid, WAR_RELATION);
        Bukkit.getPluginManager().callEvent(new WarDeclaredEvent(sourceNationUuid, targetNationUuid, lecternBlock.getLocation()));

        return sendSuccessBook(lecternBlock, player,
                "War Declared",
                "§lWar Declared§r" +
                        "\n" +
                        targetNationName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "A formal declaration has been made against §o" + targetNationName + "§r." +
                        "\n\n" +
                        "From this day forth, your nations are at war.",
                sourceNationName
        );
    }
}
