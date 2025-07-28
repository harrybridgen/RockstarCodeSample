package com.blothera.book.handler.commandbook.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyRequestsDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.event.diplomacy.AllianceRequestDeniedEvent;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.ALLIANCE_RELATION;
import static com.blothera.util.NationConstants.DENY_ALLY_COMMAND;

public class DenyAllyHandler extends CommandBookHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final NationDAO nationDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final DiplomacyRequestsDAO diplomacyRequestsDAO;

    public DenyAllyHandler(NationPlugin plugin) {
        super(plugin);
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.diplomacyRequestsDAO = plugin.getDatabase().getDiplomacyRequestsDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(DENY_ALLY_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult validate(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "Book must contain the name of the nation to deny.");
        }

        String playerUuid = player.getUniqueId().toString();
        String playerNationUuid = nationMemberDAO.getNationUuid(playerUuid);

        if (playerNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "You are not a member of any nation.");
        }

        if (!nationDAO.getLeaderUuid(playerNationUuid).equals(playerUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "Only the nation leader can deny alliance requests.");
        }

        boolean isInOwnLectern = townDAO
                .getTownsByNationUuid(playerNationUuid)
                .stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));

        if (!isInOwnLectern) {
            return sendErrorBook(lecternBlock, player, "This diplomacy lectern is not inside your nation's town.");
        }

        return null;
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String playerNationUuid = nationMemberDAO.getNationUuid(player.getUniqueId().toString());
        String sourceNationName = nationDAO.getNationName(playerNationUuid);
        if (sourceNationName == null) {
            return sendErrorBook(lecternBlock, player, "Your nation does not exist or has been deleted. Contact an admin.");
        }

        String targetNationName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (targetNationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Target nation name cannot be empty.");
        }

        String targetNationUuid = nationDAO.getNationUUIDByName(targetNationName);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "No nation by that name exists.");
        }

        targetNationName = nationDAO.getNationName(targetNationUuid);
        if (targetNationName == null) {
            return sendErrorBook(lecternBlock, player, "No nation by that name exists.");
        }


        if (playerNationUuid.equals(targetNationUuid)) {
            return sendErrorBook(lecternBlock, player, "You cannot deny a request from your own nation.");
        }

        if (!diplomacyRequestsDAO.hasPendingRequestBetween(targetNationUuid, playerNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "No pending alliance request from " + targetNationName + " to deny.");
        }

        diplomacyRequestsDAO.deletePendingRequest(targetNationUuid, playerNationUuid, ALLIANCE_RELATION);
        Bukkit.getPluginManager().callEvent(new AllianceRequestDeniedEvent(playerNationUuid, targetNationUuid));

        return sendSuccessBook(lecternBlock, player,
                "Alliance Denied",
                "§lAlliance Declined§r" +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "The proposal from §o" + targetNationName + "§r has been reviewed and respectfully denied." +
                        "\n\n" +
                        "Let this be known as a sovereign decision, diplomacy may resume another day.",
                sourceNationName
        );
    }
}
