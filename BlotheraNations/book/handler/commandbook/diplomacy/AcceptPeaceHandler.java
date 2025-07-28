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
import com.blothera.event.diplomacy.PeaceEstablishedEvent;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.*;

public class AcceptPeaceHandler extends CommandBookHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final NationDAO nationDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final DiplomacyRequestsDAO diplomacyRequestsDAO;
    private final DiplomacyDAO diplomacyDAO;

    public AcceptPeaceHandler(NationPlugin plugin) {
        super(plugin);
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.diplomacyRequestsDAO = plugin.getDatabase().getDiplomacyRequestsDAO();
        this.diplomacyDAO = plugin.getDatabase().getDiplomacyDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(ACCEPT_PEACE_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult validate(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "Book must contain the name of the nation you're accepting peace with.");
        }

        String playerUuid = player.getUniqueId().toString();
        String sourceNationUuid = nationMemberDAO.getNationUuid(playerUuid);
        if (sourceNationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "You are not a member of any nation.\n\nJoin a nation to partake in diplomacy.");
        }

        boolean isOwnLectern = townDAO.getTownsByNationUuid(sourceNationUuid).stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));
        if (!isOwnLectern) {
            return sendErrorBook(lecternBlock, player,
                    "This diplomacy lectern is not inside your nation's borders.\n\nYou can only accept peace requests from your own nation.");
        }

        if (!nationDAO.getLeaderUuid(sourceNationUuid).equals(playerUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "Only the nation leader can accept peace requests.");
        }

        return null;
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String sourceNationUuid = nationMemberDAO.getNationUuid(player.getUniqueId().toString());
        String sourceNationName = nationDAO.getNationName(sourceNationUuid);

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
            return sendErrorBook(lecternBlock, player, "You cannot accept a peace request from your own nation.");
        }

        if (!diplomacyDAO.hasRelation(sourceNationUuid, targetNationUuid, WAR_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You are not at war with " + targetNationName + ".");
        }

        if (!diplomacyRequestsDAO.hasPendingRequestBetween(targetNationUuid, sourceNationUuid, PEACE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "No pending peace request from " + targetNationName + " to accept.");
        }

        diplomacyRequestsDAO.deletePendingRequest(targetNationUuid, sourceNationUuid, PEACE_RELATION);
        diplomacyDAO.removeRelation(sourceNationUuid, targetNationUuid, WAR_RELATION);
        Bukkit.getPluginManager().callEvent(new PeaceEstablishedEvent(sourceNationUuid, targetNationUuid, lecternBlock.getLocation()));

        return sendSuccessBook(lecternBlock, player,
                "Peace Declared",
                "§lPeace Restored§r" +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "§o" + targetNationName + "§r has accepted your gesture of reconciliation." +
                        "\n\n" +
                        "The war ends not with banners raised, but with understanding forged in its ashes.",
                sourceNationName
        );
    }
}
