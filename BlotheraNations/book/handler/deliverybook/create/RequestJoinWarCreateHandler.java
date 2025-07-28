package com.blothera.book.handler.deliverybook.create;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.deliverybook.DeliveryBookCreateHandler;
import com.blothera.database.DiplomacyDAOs.DiplomacyRequestsDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.WarDAOs.WarDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.ALLIANCE_RELATION;
import static com.blothera.util.NationConstants.REQUEST_JOIN_WAR_DELIVERY_TYPE;

public class RequestJoinWarCreateHandler extends DeliveryBookCreateHandler {

    private final NationDAO nationDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final DiplomacyRequestsDAO diplomacyRequestsDAO;
    private final WarDAO warDAO;

    public RequestJoinWarCreateHandler(NationPlugin plugin) {
        super(plugin);
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.diplomacyRequestsDAO = plugin.getDatabase().getDiplomacyRequestsDAO();
        this.warDAO = plugin.getDatabase().getWarDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of("JOIN WAR", "WAR JOIN");
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        // Can reuse diplomacy lectern check if applicable
        return plugin.getDatabase().getDiplomacyLecternDAO().isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String playerUuid = player.getUniqueId().toString();
        String playerNationUuid = nationMemberDAO.getNationUuid(playerUuid);
        if (playerNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "You are not a member of any nation.");
        }

        if (!playerUuid.equals(nationDAO.getLeaderUuid(playerNationUuid))) {
            return sendErrorBook(lecternBlock, player, "Only nation leaders may request to join a war.");
        }

        boolean validChunk = townDAO
                .getTownsByNationUuid(playerNationUuid)
                .stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));
        if (!validChunk) {
            return sendErrorBook(lecternBlock, player, "This lectern is not inside a town owned by your nation.");
        }

        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "Book must contain the target nation name. Write it on the first line.");
        }

        String targetNationName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (targetNationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Target nation name cannot be empty.");
        }

        String targetNationUuid = nationDAO.getNationUUIDByName(targetNationName);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player, "No nation by that name exists.");
        }
        if (!plugin.getDatabase().getDiplomacyDAO().hasRelation(playerNationUuid, targetNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player,
                    "You must be allied with " + targetNationName + " to request joining their war.");
        }
        int warId = warDAO.getOngoingWarIdByNation(targetNationUuid);
        if (warId == -1) {
            return sendErrorBook(lecternBlock, player, targetNationName + " is not in an ongoing war.");
        }
        var war = warDAO.getWarById(warId);
        if (war == null) {
            return sendErrorBook(lecternBlock, player, "Unable to find war data. Please try again or contact an admin.");
        }

        String attacker = war.attackerNationUuid();
        String defender = war.defenderNationUuid();

        // Determine the enemy of the target nation
        String enemyUuid = targetNationUuid.equals(attacker) ? defender :
                targetNationUuid.equals(defender) ? attacker : null;

        if (enemyUuid == null) {
            return sendErrorBook(lecternBlock, player, "Target nation is not a valid participant in this war.");
        }

        // Check if player nation is allied with the enemy
        if (plugin.getDatabase().getDiplomacyDAO().hasRelation(playerNationUuid, enemyUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player,
                    "You cannot join the war because your nation is allied with the enemy of " + targetNationName + ".");
        }

        if (warDAO.isNationInWar(warId, playerNationUuid)) {
            return sendErrorBook(lecternBlock, player, "You are already participating in this war.");
        }

        if (diplomacyRequestsDAO.hasPendingRequestBetween(playerNationUuid, targetNationUuid, REQUEST_JOIN_WAR_DELIVERY_TYPE)) {
            return sendErrorBook(lecternBlock, player, "A join war request to " + targetNationName + " is already pending.");
        }

        // Create delivery book
        ItemStack deliveryBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
        bookMeta.setTitle("Join War Request");
        bookMeta.setAuthor(player.getName());

        bookMeta.addPage("§lJoin War Request§r\n" +
                targetNationName + "\n" + getDate() + "\n\n" +
                "Your nation seeks to join the war alongside §o" + targetNationName + "§r.\n\n" +
                "Place this book in their Diplomacy Lectern to submit the request.");

        bookMeta.setLore(List.of("§7" + nationDAO.getNationName(playerNationUuid)));

        setDeliveryKeys(bookMeta, playerNationUuid, targetNationUuid, REQUEST_JOIN_WAR_DELIVERY_TYPE);
        deliveryBook.setItemMeta(bookMeta);

        plugin.getNationLogger().log(player.getName() + " from " +
                nationDAO.getNationName(playerNationUuid) +
                " created a join war request book targeting " + targetNationName);

        return sendSuccessBook(lecternBlock, player, deliveryBook);
    }
}
