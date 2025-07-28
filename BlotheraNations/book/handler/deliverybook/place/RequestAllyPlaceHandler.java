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

public class RequestAllyPlaceHandler extends DeliveryBookPlaceHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final NationDAO nationDAO;
    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final DiplomacyRequestsDAO diplomacyRequestsDAO;
    private final DiplomacyDAO diplomacyDAO;
    private final NationMemberDAO nationMemberDAO;

    public RequestAllyPlaceHandler(NationPlugin plugin) {
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
        return List.of(ALLIANCE_REQUEST_DELIVERY);
    }


    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {

        String requestType = getRequestType(meta);
        if (requestType == null || !requestType.equals(ALLIANCE_REQUEST_DELIVERY_TYPE)) {
            return sendErrorBook(lecternBlock, player, "Invalid alliance book format.");
        }

        String targetNationUuid = getTargetEntityUuid(meta);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "Target nation no longer exists.");
        }

        String targetNationName = nationDAO.getNationName(targetNationUuid);
        if (targetNationName == null) {
            return sendErrorBook(lecternBlock, player, "Target nation name could not be determined.");
        }

        boolean lecternValid = townDAO
                .getTownsByNationUuid(targetNationUuid)
                .stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));

        if (!lecternValid) {
            return sendErrorBook(lecternBlock, player, "This lectern is not in the territory of " + targetNationName + ".");
        }

        String senderUuid = player.getUniqueId().toString();
        String senderNationUuid = nationMemberDAO.getNationUuid(senderUuid);
        if (senderNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "You are not part of a nation.");
        }

        String storedSourceUuid = getRequestingEntityUuid(meta);
        if (storedSourceUuid == null) {
            return sendErrorBook(lecternBlock, player, "Alliance book missing source nation tag. Please report this.");
        }

        // Check that the author is the current nation leader
        String currentLeaderUuidStr = nationDAO.getLeaderUuid(storedSourceUuid);
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

        if (diplomacyDAO.hasRelation(senderNationUuid, targetNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You are already allied with " + targetNationName + ".");
        }

        if (!storedSourceUuid.equals(senderNationUuid)) {
            return sendErrorBook(lecternBlock, player, "This alliance book does not belong to your nation.");
        }

        if (senderNationUuid.equals(targetNationUuid)) {
            return sendErrorBook(lecternBlock, player, "You cannot send a request to your own nation.");
        }

        if (diplomacyDAO.hasRelation(senderNationUuid, targetNationUuid, WAR_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You are currently at war with " + targetNationName + ". End the war before forming an alliance.");
        }

        int senderWarId = plugin.getDatabase().getWarDAO().getOngoingWarIdByNation(senderNationUuid);
        int targetWarId = plugin.getDatabase().getWarDAO().getOngoingWarIdByNation(targetNationUuid);
        if (senderWarId != -1 && senderWarId == targetWarId) {
            var war = plugin.getDatabase().getWarDAO().getWarById(senderWarId);
            if (war != null) {
                String enemyLeaderUuid = targetNationUuid.equals(war.attackerNationUuid())
                        ? war.defenderNationUuid()
                        : war.attackerNationUuid();

                if (enemyLeaderUuid.equals(senderNationUuid)) {
                    return sendErrorBook(lecternBlock, player,
                            "You are part of a war against " + targetNationName + ". An alliance cannot be formed.");
                }
            }
        }
        String sourceNationName = nationDAO.getNationName(senderNationUuid);

        boolean reciprocalExists = diplomacyRequestsDAO.hasPendingRequestBetween(targetNationUuid, senderNationUuid, ALLIANCE_RELATION);
        if (reciprocalExists) {
            diplomacyDAO.createRelation(senderNationUuid, targetNationUuid, ALLIANCE_RELATION);
            return sendSuccessBook(lecternBlock, player,
                    "Alliance Formed",
                    "§lAlliance Forged§r" +
                            "\n" +
                            getDate() +
                            "\n\n" +
                            "An existing request from §o" + targetNationName + "§r was discovered in the records." +
                            "\n\n" +
                            "With quills laid down and intentions clear, your nations are now bound in mutual alliance.",
                    sourceNationName
            );
        }


        diplomacyRequestsDAO.createRequest(senderNationUuid, targetNationUuid, ALLIANCE_RELATION);

        plugin.getNationLogger().log(player.getName() + " from " + sourceNationName + " has sent an alliance request to " + targetNationName);

        return sendSuccessBook(lecternBlock, player,
                "Alliance Request Sent",
                "§lAlliance Proposal§r" +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "An official request for alliance has been dispatched to §o" + targetNationName + "§r." +
                        "\n\n" +
                        "Their leader must review the proposal and decide whether to unite in friendship.",
                sourceNationName
        );
    }
}
