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
import com.blothera.database.WarDAOs.WarDAO;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;
import java.util.UUID;

import static com.blothera.util.NationConstants.ALLIANCE_RELATION;
import static com.blothera.util.NationConstants.REQUEST_JOIN_WAR_DELIVERY_TYPE;

public class RequestJoinWarPlaceHandler extends DeliveryBookPlaceHandler {

    private final NationPlugin plugin;
    private final DiplomacyLecternDAO lecternDAO;
    private final NationDAO nationDAO;
    private final NationMemberDAO memberDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO claimDAO;
    private final DiplomacyDAO diplomacyDAO;
    private final DiplomacyRequestsDAO requestsDAO;
    private final WarDAO warDAO;

    public RequestJoinWarPlaceHandler(NationPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.lecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.memberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.claimDAO = plugin.getDatabase().getTownClaimDAO();
        this.diplomacyDAO = plugin.getDatabase().getDiplomacyDAO();
        this.requestsDAO = plugin.getDatabase().getDiplomacyRequestsDAO();
        this.warDAO = plugin.getDatabase().getWarDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of("Join War Request");
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return lecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String requestType = getRequestType(meta);
        if (!REQUEST_JOIN_WAR_DELIVERY_TYPE.equals(requestType)) {
            return sendErrorBook(lecternBlock, player, "Invalid join war request format.");
        }

        String senderNationUuid = getRequestingEntityUuid(meta);
        String targetNationUuid = getTargetEntityUuid(meta);

        if (senderNationUuid == null || targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "Missing nation information in this book.");
        }

        // Check that the author is the current nation leader
        String currentLeaderUuidStr = nationDAO.getLeaderUuid(senderNationUuid);
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

        String targetNationName = nationDAO.getNationName(targetNationUuid);
        if (targetNationName == null) {
            return sendErrorBook(lecternBlock, player, "Target nation no longer exists.");
        }

        boolean validLectern = townDAO.getTownsByNationUuid(targetNationUuid)
                .stream()
                .anyMatch(townUuid -> claimDAO.isLecternInTownChunk(townUuid, lecternBlock));

        if (!validLectern) {
            return sendErrorBook(lecternBlock, player, "This lectern is not in the territory of " + targetNationName + ".");
        }

        // Must be allies
        if (!diplomacyDAO.hasRelation(senderNationUuid, targetNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You must be allied with " + targetNationName + " to join their war.");
        }

        int warId = warDAO.getOngoingWarIdByNation(targetNationUuid);
        if (warId == -1) {
            return sendErrorBook(lecternBlock, player, targetNationName + " is not in a war.");
        }

        var war = warDAO.getWarById(warId);
        if (war == null) {
            return sendErrorBook(lecternBlock, player, "Could not retrieve war details.");
        }

        // Cannot be allied with the enemy
        String enemyUuid = targetNationUuid.equals(war.attackerNationUuid()) ?
                war.defenderNationUuid() : war.attackerNationUuid();

        if (diplomacyDAO.hasRelation(senderNationUuid, enemyUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You are allied with both sides. Choose a side before joining.");
        }

        if (warDAO.isNationInWar(warId, senderNationUuid)) {
            return sendErrorBook(lecternBlock, player, "Your nation is already in this war.");
        }

        if (requestsDAO.hasPendingRequestBetween(senderNationUuid, targetNationUuid, REQUEST_JOIN_WAR_DELIVERY_TYPE)) {
            return sendErrorBook(lecternBlock, player, "You already sent a join war request to " + targetNationName + ".");
        }

        int sourceWarId = warDAO.getOngoingWarIdByNation(senderNationUuid);
        if (sourceWarId != -1) {
            return sendErrorBook(lecternBlock, player,
                    "Your nation is already involved in another war.\n" +
                            "Only one war at a time is allowed per nation.");
        }

        int targetWarId = warDAO.getOngoingWarIdByNation(targetNationUuid);
        if (targetWarId == -1) {
            return sendErrorBook(lecternBlock, player,
                    "The nation you are trying to join arms with is not fighting in a war.");
        }

        requestsDAO.deletePendingRequest(senderNationUuid, enemyUuid, ALLIANCE_RELATION);
        requestsDAO.deletePendingRequest(enemyUuid, senderNationUuid, ALLIANCE_RELATION);
        requestsDAO.createRequest(senderNationUuid, targetNationUuid, REQUEST_JOIN_WAR_DELIVERY_TYPE);

        plugin.getNationLogger().log(player.getName() + " from " +
                nationDAO.getNationName(senderNationUuid) +
                " submitted a join war request to " + targetNationName);

        return sendSuccessBook(lecternBlock, player,
                "Join War Request Sent",
                "§lJoin War Request§r\n" +
                        getDate() + "\n\n" +
                        "Your nation seeks to join the war alongside §o" + targetNationName + "§r.\n\n" +
                        "The request has been recorded and will await their decision.",
                nationDAO.getNationName(senderNationUuid)
        );
    }
}
