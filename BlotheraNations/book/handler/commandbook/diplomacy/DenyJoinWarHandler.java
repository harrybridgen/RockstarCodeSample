package com.blothera.book.handler.commandbook.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyRequestsDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.REQUEST_JOIN_WAR_DELIVERY_TYPE;

public class DenyJoinWarHandler extends CommandBookHandler {

    private final DiplomacyLecternDAO lecternDAO;
    private final NationDAO nationDAO;
    private final NationMemberDAO memberDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO claimDAO;
    private final DiplomacyRequestsDAO requestsDAO;

    public DenyJoinWarHandler(NationPlugin plugin) {
        super(plugin);
        this.lecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.memberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.claimDAO = plugin.getDatabase().getTownClaimDAO();
        this.requestsDAO = plugin.getDatabase().getDiplomacyRequestsDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of("Deny Join War");
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return lecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult validate(Player player, Block lecternBlock, BookMeta meta) {
        String uuid = player.getUniqueId().toString();
        String nationUuid = memberDAO.getNationUuid(uuid);
        if (nationUuid == null) {
            return sendErrorBook(lecternBlock, player, "You are not in a nation.");
        }

        if (!nationDAO.getLeaderUuid(nationUuid).equals(uuid)) {
            return sendErrorBook(lecternBlock, player, "Only your nation leader may deny join war requests.");
        }

        boolean validChunk = townDAO.getTownsByNationUuid(nationUuid)
                .stream()
                .anyMatch(town -> claimDAO.isLecternInTownChunk(town, lecternBlock));
        if (!validChunk) {
            return sendErrorBook(lecternBlock, player, "This lectern is not on your nation's land.");
        }

        return null;
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String selfNationUuid = memberDAO.getNationUuid(player.getUniqueId().toString());
        String sourceNationName = nationDAO.getNationName(selfNationUuid);

        String targetNationName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (targetNationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "You must specify a nation name.");
        }

        String targetNationUuid = nationDAO.getNationUUIDByName(targetNationName);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "No such nation exists.");
        }

        if (!requestsDAO.hasPendingRequestBetween(targetNationUuid, selfNationUuid, REQUEST_JOIN_WAR_DELIVERY_TYPE)) {
            return sendErrorBook(lecternBlock, player, "No join war request from " + targetNationName + " found.");
        }

        requestsDAO.deletePendingRequest(targetNationUuid, selfNationUuid, REQUEST_JOIN_WAR_DELIVERY_TYPE);

        return sendSuccessBook(lecternBlock, player,
                "Join War Denied",
                "§lRequest Denied§r\n" +
                        getDate() +
                        "\n\n" +
                        "The request from §o" + targetNationName + "§r to join your war was denied.\n\nYour strategy remains your own.",
                sourceNationName
        );
    }
}
