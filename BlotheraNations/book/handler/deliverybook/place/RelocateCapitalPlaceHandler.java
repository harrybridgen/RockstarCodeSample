package com.blothera.book.handler.deliverybook.place;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.deliverybook.DeliveryBookPlaceHandler;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import com.blothera.event.nation.NationCapitalTransferEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;
import java.util.UUID;

import static com.blothera.util.NationConstants.CAPITAL_RELOCATION_DELIVERY;
import static com.blothera.util.NationConstants.RELOCATE_CAPITAL_DELIVERY_TYPE;

/**
 * Second stage handler for capital relocation.
 * <p>
 * When the delivery book is placed in a Town Lectern belonging to the
 * target town, the nation's capital is moved.
 */
public class RelocateCapitalPlaceHandler extends DeliveryBookPlaceHandler {

    private final NationDAO nationDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownLecternDAO townLecternDAO;

    public RelocateCapitalPlaceHandler(NationPlugin plugin) {
        super(plugin);
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(CAPITAL_RELOCATION_DELIVERY);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {

        String requestType = getRequestType(meta);
        if (requestType == null || !requestType.equals(RELOCATE_CAPITAL_DELIVERY_TYPE)) {
            return sendErrorBook(lecternBlock, player, "Invalid capital relocation book.");
        }

        String targetTownUuid = getTargetEntityUuid(meta);
        if (targetTownUuid == null) {
            return sendErrorBook(lecternBlock, player, "The target town does not exist.");
        }

        String targetTownName = townDAO.getTownName(targetTownUuid);
        if (targetTownName == null) {
            return sendErrorBook(lecternBlock, player, "Could not retrieve target town name.");
        }

        String nationUuid = getRequestingEntityUuid(meta);

        if (nationUuid == null) {
            return sendErrorBook(lecternBlock, player, "Missing nation data on book.");
        }

        String playerUuid = player.getUniqueId().toString();
        String playerNationUuid = nationMemberDAO.getNationUuid(playerUuid);
        if (playerNationUuid == null || !playerNationUuid.equals(nationUuid)) {
            return sendErrorBook(lecternBlock, player, "This book does not belong to your nation.");
        }

        String leaderUuid = nationDAO.getLeaderUuid(nationUuid);
        if (leaderUuid == null || !leaderUuid.equals(playerUuid)) {
            return sendErrorBook(lecternBlock, player, "Only the nation leader can place this book.");
        }

        // Check that the author is the current nation leader
        String currentLeaderUuidStr = nationDAO.getLeaderUuid(nationUuid);
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

        if (!nationUuid.equals(townDAO.getNationUuidFromTownUuid(targetTownUuid))) {
            return sendErrorBook(lecternBlock, player, "That town does not belong to your nation.");
        }

        if (!townClaimDAO.isLecternInTownChunk(targetTownUuid, lecternBlock)) {
            return sendErrorBook(lecternBlock, player, "This lectern is not inside the target town.");
        }

        if (townDAO.isCapital(targetTownUuid)) {
            return sendErrorBook(lecternBlock, player, "This town is already the capital of your nation.");
        }

        String oldCapitalName = nationDAO.getCapitalTownName(nationUuid);
        townDAO.setCapital(targetTownUuid, nationUuid);
        String newCapitalName = townDAO.getTownName(targetTownUuid);

        Bukkit.getPluginManager().callEvent(new NationCapitalTransferEvent(nationUuid, oldCapitalName, newCapitalName));

        return sendSuccessBook(lecternBlock, player,
                "Capital Relocated",
                "§lCapital Relocated§r" +
                        "\n" +
                        newCapitalName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "Henceforth, §o" + newCapitalName + "§r shall serve as the beating heart of your nation." +
                        "\n\n" +
                        "Let the land and its people recognize this change in seat and soul.",
                newCapitalName
        );
    }
}
