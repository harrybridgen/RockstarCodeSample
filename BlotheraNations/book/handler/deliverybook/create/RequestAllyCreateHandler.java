package com.blothera.book.handler.deliverybook.create;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.deliverybook.DeliveryBookCreateHandler;
import com.blothera.database.DiplomacyDAOs.DiplomacyDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyRequestsDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.*;

public class RequestAllyCreateHandler extends DeliveryBookCreateHandler {

    private final NationDAO nationDAO;
    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final DiplomacyDAO diplomacyDAO;
    private final NationMemberDAO nationMemberDAO;
    private final DiplomacyRequestsDAO diplomacyRequestsDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;


    public RequestAllyCreateHandler(NationPlugin plugin) {
        super(plugin);
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.diplomacyDAO = plugin.getDatabase().getDiplomacyDAO();
        this.diplomacyRequestsDAO = plugin.getDatabase().getDiplomacyRequestsDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(
                REQUEST_ALLIANCE_COMMAND,
                ALLIANCE_COMMAND,
                ALLY_COMMAND,
                OFFER_ALLY_COMMAND
        );
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String playerUuid = player.getUniqueId().toString();
        String playerNationUuid = nationMemberDAO.getNationUuid(playerUuid);

        if (playerNationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "You are not a member of any nation.");
        }

        String sourceNationName = nationDAO.getNationName(playerNationUuid);
        if (sourceNationName == null) {
            return sendErrorBook(lecternBlock, player, "Your nation does not exist. Please contact an admin.");
        }

        // Check if player is the nation leader
        if (!playerUuid.equals(nationDAO.getLeaderUuid(playerNationUuid))) {
            return sendErrorBook(lecternBlock, player, "Only the nation leader can initiate an alliance request.");
        }

        // Validate that the lectern is in a valid town chunk of the player's nation
        boolean validChunk = townDAO
                .getTownsByNationUuid(playerNationUuid)
                .stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));
        if (!validChunk) {
            return sendErrorBook(lecternBlock, player, "This lectern is not inside any of your nation's towns.");
        }

        // Check if the book has the required persistent data
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "Book must contain the target nation name. Write it on the first line of the book.");
        }

        // Get target nation name from the first page
        String targetNationName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (targetNationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Target nation name cannot be empty. Write it on the first line of the book.");
        }

        // Validate target nation exists
        String targetNationUuid = nationDAO.getNationUUIDByName(targetNationName);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "No nation by that name exists.");
        }

        // Get the actual name of the target nation
        targetNationName = nationDAO.getNationName(targetNationUuid);
        if (targetNationName == null) {
            return sendErrorBook(lecternBlock, player, "Could not get canonical name for target nation. Please contact an admin.");
        }

        if (playerNationUuid.equals(targetNationUuid)) {
            return sendErrorBook(lecternBlock, player, "You cannot form an alliance with your own nation.");
        }

        // Check if player is already allied with the target nation
        if (diplomacyDAO.hasRelation(playerNationUuid, targetNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You are already allied with " + targetNationName + ".");
        }

        // Prevent allying with war enemies
        if (diplomacyDAO.hasRelation(playerNationUuid, targetNationUuid, WAR_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You are currently at war with " + targetNationName + ". You must end the war before forming an alliance.");
        }

        // Prevent allying with a nation you are at war with indirectly (i.e. war allies)
        int sourceWarId = plugin.getDatabase().getWarDAO().getOngoingWarIdByNation(playerNationUuid);
        int targetWarId = plugin.getDatabase().getWarDAO().getOngoingWarIdByNation(targetNationUuid);
        if (sourceWarId != -1 && sourceWarId == targetWarId) {
            var war = plugin.getDatabase().getWarDAO().getWarById(sourceWarId);
            if (war != null) {
                String enemyLeaderUuid = targetNationUuid.equals(war.attackerNationUuid())
                        ? war.defenderNationUuid()
                        : war.attackerNationUuid();

                if (enemyLeaderUuid.equals(playerNationUuid)) {
                    return sendErrorBook(lecternBlock, player,
                            "You are involved in a war against " + targetNationName + ". You cannot form an alliance with them.");
                }
            }
        }

        // Check if there are pending requests
        if (diplomacyRequestsDAO.hasPendingRequestBetween(playerNationUuid, targetNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player, "You already have a pending Alliance request to " + targetNationName + ".");
        }

        // Check if the target nation has a pending request from this nation
        if (diplomacyRequestsDAO.hasPendingRequestBetween(targetNationUuid, playerNationUuid, ALLIANCE_RELATION)) {
            diplomacyDAO.createRelation(playerNationUuid, targetNationUuid, ALLIANCE_RELATION);
            return sendSuccessBook(lecternBlock, player,
                    "Alliance Formed",
                    "§lAlliance Forged§r" +
                            "\n" +
                            getDate() +
                            "\n\n" +
                            "§o" + targetNationName + "§r had already extended their hand in alliance." +
                            "\n\n" +
                            "Your matching proposal has sealed the accord, an alliance now binds your people together.",
                    sourceNationName);

        }

        // Create alliance "delivery" book
        ItemStack deliveryBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
        bookMeta.setTitle(ALLIANCE_REQUEST_DELIVERY);
        bookMeta.setAuthor(player.getName());
        bookMeta.addPage("§l" + ALLIANCE_REQUEST_DELIVERY + "§r" +
                "\n" +
                targetNationName +
                "\n" +
                getDate() +
                "\n\n" +
                "This book is a request for an alliance with §o" + targetNationName + "§r." +
                "\n\n" +
                "Place this in their Diplomacy Lectern to deliver the request.");

        bookMeta.setLore(List.of("§7" + sourceNationName));

        plugin.getNationLogger().log(player.getName() + " from " + sourceNationName + " has created an alliance request delivery book, targeting " + targetNationName);

        setDeliveryKeys(bookMeta, playerNationUuid, targetNationUuid, ALLIANCE_REQUEST_DELIVERY_TYPE);
        deliveryBook.setItemMeta(bookMeta);
        return sendSuccessBook(lecternBlock, player, deliveryBook);
    }
}
