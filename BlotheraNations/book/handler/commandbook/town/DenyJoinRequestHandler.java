package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownJoinRequestDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import com.blothera.event.town.TownJoinRequestDeniedEvent;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import java.util.List;

import static com.blothera.util.NationConstants.DENY_JOIN_COMMAND;
import static com.blothera.util.NationConstants.DENY_REQUEST_COMMAND;

public class DenyJoinRequestHandler extends CommandBookHandler {

    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final TownJoinRequestDAO joinRequestDAO;
    private final TownLecternDAO townLecternDAO;

    public DenyJoinRequestHandler(NationPlugin plugin) {
        super(plugin);
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.joinRequestDAO = plugin.getDatabase().getJoinRequestDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(DENY_REQUEST_COMMAND, DENY_JOIN_COMMAND);
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
        String targetName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (targetName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Book must contain the player name.");
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
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
            return sendErrorBook(lecternBlock, player, "You are not authorized to deny this request.");
        }

        if (townDAO.isDormant(townUuid)) {
            return sendErrorBook(lecternBlock, player, "This town is dormant and cannot be managed.");
        }
        String townName = townDAO.getTownName(townUuid);
        joinRequestDAO.deleteRequest(targetPlayerUuid, townUuid);
        Bukkit.getPluginManager().callEvent(new TownJoinRequestDeniedEvent(townUuid, targetPlayerUuid, player.getUniqueId().toString()));
        return sendSuccessBook(lecternBlock, player,
                "Citizenship Denied",
                "§lRequest Rejected§r" +
                        "\n" +
                        targetName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "After consideration, §o" + townName + "§r has declined the request for citizenship from §o" + targetName + "§r." +
                        "\n\n" +
                        "Let this decision be noted in the town’s record.",
                townName
        );

    }
}
