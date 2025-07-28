package com.blothera.book.handler.commandbook.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.DiplomacyDAOs.DiplomacyDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyRequestsDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.event.diplomacy.AllianceFormedEvent;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.ACCEPT_ALLY_COMMAND;
import static com.blothera.util.NationConstants.ALLIANCE_RELATION;

public class AcceptAllyHandler extends CommandBookHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final NationMemberDAO nationMemberDAO;
    private final NationDAO nationDAO;
    private final TownDAO townDAO;
    private final DiplomacyDAO diplomacyDAO;
    private final TownClaimDAO townClaimDAO;
    private final DiplomacyRequestsDAO diplomacyRequestsDAO;

    public AcceptAllyHandler(NationPlugin plugin) {
        super(plugin);
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();

        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.diplomacyDAO = plugin.getDatabase().getDiplomacyDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.diplomacyRequestsDAO = plugin.getDatabase().getDiplomacyRequestsDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(ACCEPT_ALLY_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult validate(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "The book must not be empty.\n\nWrite the name of the nation on the first line of the book.");
        }

        String playerUuid = player.getUniqueId().toString();
        String playerNationUuid = nationMemberDAO.getNationUuid(playerUuid);
        if (playerNationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "You are not a member of any nation.\n\nJoin a nation to partake in diplomacy.");
        }

        if (!nationDAO.getLeaderUuid(playerNationUuid).equals(playerUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "Only the nation leader can accept alliance requests.\n\nAsk your leader to accept the request.");
        }

        boolean isOwnLectern = townDAO.getTownsByNationUuid(playerNationUuid).stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));
        if (!isOwnLectern) {
            return sendErrorBook(lecternBlock, player,
                    "This diplomacy lectern is not inside your nation's town.\n\nYou can only accept alliance requests from your own nation's territory.");
        }

        return null;
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String playerNationUuid = nationMemberDAO.getNationUuid(player.getUniqueId().toString());

        String targetNationName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (targetNationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player,
                    "You must provide the name of the nation you want to ally with.\n\nWrite the nation's name on the first line of the book.");
        }

        String targetNationUuid = nationDAO.getNationUUIDByName(targetNationName);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "That nation is not known to the land.\n\nEnsure you are correct and try again.");
        }

        targetNationName = nationDAO.getNationName(targetNationUuid);
        if (targetNationName == null) {
            return sendErrorBook(lecternBlock, player,
                    "Could not retrieve the name of the nation with UUID: " + targetNationUuid + ".\n\nPlease contact an admin.");
        }

        if (playerNationUuid.equals(targetNationUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "You cannot accept a request from your own nation.\n\nThe land will not be fooled.");
        }

        if (diplomacyDAO.hasRelation(playerNationUuid, targetNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player,
                    "You are already allied with " + targetNationName + ".\n\nThere is no need to accept the request again.");
        }

        if (!diplomacyRequestsDAO.hasPendingRequestBetween(targetNationUuid, playerNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player,
                    "No pending alliance request from " + targetNationName + " to accept.\n\nEnsure the other nation has sent a request before accepting.");
        }

        diplomacyDAO.createRelation(playerNationUuid, targetNationUuid, ALLIANCE_RELATION);
        String sourceNationName = nationDAO.getNationName(playerNationUuid);
        Bukkit.getPluginManager().callEvent(new AllianceFormedEvent(playerNationUuid, targetNationUuid, lecternBlock.getLocation()));

        return sendSuccessBook(lecternBlock, player,
                "Alliance Formed",
                "§lAlliance Formed§r" +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "A pact of friendship has been sealed between §o" + sourceNationName + "§r and §o" + targetNationName + "§r." +
                        "\n\n" +
                        "May this bond bring strength, unity, and prosperity to your people.",
                targetNationName
        );
    }
}
