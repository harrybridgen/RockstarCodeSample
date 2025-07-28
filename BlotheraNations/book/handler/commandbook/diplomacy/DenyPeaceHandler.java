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
import com.blothera.event.diplomacy.PeaceRequestDeniedEvent;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.DENY_PEACE_COMMAND;
import static com.blothera.util.NationConstants.PEACE_RELATION;

public class DenyPeaceHandler extends CommandBookHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final NationDAO nationDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final DiplomacyRequestsDAO diplomacyRequestsDAO;

    public DenyPeaceHandler(NationPlugin plugin) {
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
        return List.of(DENY_PEACE_COMMAND);
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
        String sourceNationUuid = nationMemberDAO.getNationUuid(playerUuid);

        if (sourceNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "You are not a member of any nation.");
        }

        if (!nationDAO.getLeaderUuid(sourceNationUuid).equals(playerUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "Only the nation leader can deny peace requests.");
        }

        boolean isInOwnLectern = townDAO.getTownsByNationUuid(sourceNationUuid).stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));
        if (!isInOwnLectern) {
            return sendErrorBook(lecternBlock, player, "This diplomacy lectern is not inside your nation's town.");
        }

        return null;
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String sourceNationUuid = nationMemberDAO.getNationUuid(player.getUniqueId().toString());
        String sourceNationName = nationDAO.getNationName(sourceNationUuid);
        if (sourceNationName == null) {
            return sendErrorBook(lecternBlock, player, "Your nation does not exist. Contact an admin.");
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

        if (sourceNationUuid.equals(targetNationUuid)) {
            return sendErrorBook(lecternBlock, player, "You cannot deny a peace request from your own nation.");
        }

        if (!diplomacyRequestsDAO.hasPendingRequestBetween(targetNationUuid, sourceNationUuid, PEACE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "No pending peace request from " + targetNationName + " to deny.");
        }

        diplomacyRequestsDAO.deletePendingRequest(targetNationUuid, sourceNationUuid, PEACE_RELATION);
        Bukkit.getPluginManager().callEvent(new PeaceRequestDeniedEvent(sourceNationUuid, targetNationUuid));

        return sendSuccessBook(lecternBlock, player,
                "Peace Rejected",
                "§lPeace Rejected§r" +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "The proposal from §o" + targetNationName + "§r has been formally declined." +
                        "\n\n" +
                        "Let it be known that the conflict shall continue, and no truce will temper its course.",
                sourceNationName
        );

    }

}
