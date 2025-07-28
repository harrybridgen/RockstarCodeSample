package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.*;
import com.blothera.event.town.TownJoinRequestAcceptedEvent;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import java.util.List;

import static com.blothera.util.NationConstants.ACCEPT_JOIN_COMMAND;
import static com.blothera.util.NationConstants.ACCEPT_REQUEST_COMMAND;

public class AcceptJoinRequestHandler extends CommandBookHandler {

    private final NationDAO nationDAO;
    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownJoinRequestDAO joinRequestDAO;
    private final TownMemberDAO townMemberDAO;
    private final TownLecternDAO townLecternDAO;

    public AcceptJoinRequestHandler(NationPlugin plugin) {
        super(plugin);
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.joinRequestDAO = plugin.getDatabase().getJoinRequestDAO();
        this.townMemberDAO = plugin.getDatabase().getTownMemberDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(ACCEPT_REQUEST_COMMAND, ACCEPT_JOIN_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "Book must contain the player name.");
        }
        String targetPlayerName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (targetPlayerName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Book must contain the player name.");
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            return sendErrorBook(lecternBlock, player, "That player does not exist.");
        }

        String targetPlayerUuid = targetPlayer.getUniqueId().toString();
        String townUuid = townClaimDAO.getTownUuidAtLectern(lecternBlock);
        if (townUuid == null || !joinRequestDAO.hasRequestToSpecificTown(targetPlayerUuid, townUuid)) {
            return sendErrorBook(lecternBlock, player, "No join request found for this player to your town.");
        }

        boolean isInTown = townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock);
        if (!isInTown) {
            return sendErrorBook(lecternBlock, player, "This lectern is not within the town's borders.");
        }

        String leaderUuid = townDAO.getLeaderUuid(townUuid);
        if (!player.getUniqueId().toString().equals(leaderUuid)) {
            return sendErrorBook(lecternBlock, player, "Only the town leader may accept citizenship requests.");
        }

        if (nationDAO.getNationUuidByTownUuid(townUuid) == null) {
            return sendErrorBook(lecternBlock, player, "Town is not part of a nation. Cannot accept request. Contact an admin.");
        }

        if (townDAO.isDormant(townUuid)) {
            return sendErrorBook(lecternBlock, player, "This town is dormant and cannot be managed.");
        }

        String townName = townDAO.getTownName(townUuid);
        String playerNationUuid = nationMemberDAO.getNationUuid(targetPlayerUuid);

        if (playerNationUuid == null) {
            joinRequestDAO.deleteAllRequests(targetPlayerUuid);
        } else {
            joinRequestDAO.deleteRequest(targetPlayerUuid, townUuid);
        }

        String nationUuid = nationDAO.getNationUuidByTownUuid(townUuid);

        nationMemberDAO.addMember(targetPlayerUuid, nationUuid);
        townMemberDAO.addMember(targetPlayerUuid, townUuid);
        Bukkit.getPluginManager().callEvent(new TownJoinRequestAcceptedEvent(townUuid, targetPlayerUuid, player.getUniqueId().toString()));
        return sendSuccessBook(lecternBlock, player,
                "Citizenship Granted",
                "§lNew Citizen§r" +
                        "\n" +
                        targetPlayerName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "By the will of its leadership, §o" + townName + "§r welcomes §o" + targetPlayerName + "§r as a citizen." +
                        "\n\n" +
                        "Let their name be etched into the ledgers of the town.",
                townName
        );

    }
}
