package com.blothera.book.handler.deliverybook.place;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.deliverybook.DeliveryBookPlaceHandler;
import com.blothera.database.DiplomacyDAOs.DiplomacyDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.event.diplomacy.AllianceBrokenEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;
import java.util.UUID;

import static com.blothera.util.NationConstants.*;

public class BreakAlliancePlaceHandler extends DeliveryBookPlaceHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final NationDAO nationDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final DiplomacyDAO diplomacyDAO;

    public BreakAlliancePlaceHandler(NationPlugin plugin) {
        super(plugin);
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.diplomacyDAO = plugin.getDatabase().getDiplomacyDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(BREAK_ALLIANCE_DELIVERY);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {

        String requestType = getRequestType(meta);
        if (requestType == null || !requestType.equalsIgnoreCase(BREAK_ALLIANCE_DELIVERY_TYPE)) {
            return sendErrorBook(lecternBlock, player, "Invalid break alliance book. Contact an admin.");
        }

        String requestingNationUuid = getRequestingEntityUuid(meta);
        if (requestingNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "Missing book metadata: requesting nation. Contact an admin.");
        }

        String targetNationUuid = getTargetEntityUuid(meta);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "Missing book metadata: target nation. Contact an admin.");
        }

        // Validate that player is from the requesting nation
        String playerUuid = player.getUniqueId().toString();
        String playerNationUuid = nationMemberDAO.getNationUuid(playerUuid);
        if (playerNationUuid == null || !playerNationUuid.equals(requestingNationUuid)) {
            return sendErrorBook(lecternBlock, player, "Only members of the requesting nation can break alliances.");
        }

        String targetNationName = nationDAO.getNationName(targetNationUuid);
        if (targetNationName == null) {
            return sendErrorBook(lecternBlock, player, "The target nation has faded into history. It no longer exists.");
        }

        String requestingNationName = nationDAO.getNationName(requestingNationUuid);
        if (requestingNationName == null) {
            return sendErrorBook(lecternBlock, player, "The source nation does not exist or has been deleted. Contact an admin.");
        }

        String leaderUuid = nationDAO.getLeaderUuid(requestingNationUuid);
        if (leaderUuid == null) {
            return sendErrorBook(lecternBlock, player, "Could not determine the current nation leader. Contact an admin.");
        }

        // Check that the author is the current nation leader
        String currentLeaderUuidStr = nationDAO.getLeaderUuid(requestingNationUuid);
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

        // Ensure lectern is inside the target nation
        String world = lecternBlock.getWorld().getName();
        int chunkX = lecternBlock.getChunk().getX();
        int chunkZ = lecternBlock.getChunk().getZ();
        String townUuid = townClaimDAO.getTownIdAt(world, chunkX, chunkZ);
        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player, "This lectern is not in any claimed town.");
        }

        String lecternNationUuid = townDAO.getNationUuidFromTownUuid(townUuid);
        if (!targetNationUuid.equals(lecternNationUuid)) {
            return sendErrorBook(lecternBlock, player, "This lectern is not in the territory of " + targetNationName + ".");
        }

        if (requestingNationUuid.equals(targetNationUuid)) {
            return sendErrorBook(lecternBlock, player, "You cannot break alliance with your own nation.");
        }

        if (!diplomacyDAO.hasRelation(requestingNationUuid, targetNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "Your nations are not currently allied.");
        }

        if (!diplomacyDAO.removeRelation(requestingNationUuid, targetNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "Failed to break alliance with " + targetNationName + ". Contact an admin.");
        }
        int warId = plugin.getDatabase().getWarDAO().getOngoingWarIdByNation(requestingNationUuid);
        if (warId != -1) {
            var war = plugin.getDatabase().getWarDAO().getWarById(warId);
            if (war != null) {
                boolean isTargetLeader = targetNationUuid.equals(war.attackerNationUuid()) || targetNationUuid.equals(war.defenderNationUuid());
                boolean isRequestingNotLeader = !requestingNationUuid.equals(war.attackerNationUuid()) && !requestingNationUuid.equals(war.defenderNationUuid());

                if (isTargetLeader && isRequestingNotLeader) {
                    plugin.getDatabase().getWarAlliesDAO().removeNationFromWar(warId, requestingNationUuid);
                }
            }
        }
        Bukkit.getPluginManager().callEvent(new AllianceBrokenEvent(requestingNationUuid, targetNationUuid, lecternBlock.getLocation()));

        return sendSuccessBook(lecternBlock, player,
                "Alliance Broken",
                "§lAlliance Dissolved§r" +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "The pact between your nation and §o" + targetNationName + "§r has been officially nullified." +
                        "\n\n" +
                        "The banners that once flew side by side now drift apart on separate winds.",
                requestingNationName
        );
    }
}
