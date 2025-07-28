package com.blothera.book.handler.deliverybook.create;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.deliverybook.DeliveryBookCreateHandler;
import com.blothera.database.DiplomacyDAOs.DiplomacyDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
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

public class BreakAllianceCreateHandler extends DeliveryBookCreateHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final NationDAO nationDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final DiplomacyDAO diplomacyDAO;


    public BreakAllianceCreateHandler(NationPlugin plugin) {
        super(plugin);
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.diplomacyDAO = plugin.getDatabase().getDiplomacyDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(BREAK_ALLY_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String playerUuid = player.getUniqueId().toString();
        String playerNationUuid = nationMemberDAO.getNationUuid(playerUuid);

        // Check if player is part of a nation
        if (playerNationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "You are not a member of any nation.");
        }

        // Check if source nation exists
        String sourceNationName = nationDAO.getNationName(playerNationUuid);
        if (sourceNationName == null) {
            return sendErrorBook(lecternBlock, player,
                    "Your nation does not exist or has been deleted. Contact an admin.");
        }

        // Check if player is the leader of their nation
        if (!nationDAO.getLeaderUuid(playerNationUuid).equals(playerUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "Only the nation leader can initiate breaking an alliance.");
        }
        
        // Check if the lectern is within the player's nation's town
        boolean isInOwnLectern = townDAO
                .getTownsByNationUuid(playerNationUuid)
                .stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));

        if (!isInOwnLectern) {
            return sendErrorBook(lecternBlock, player,
                    "This diplomacy lectern is not within your nation's town.");
        }

        // Check if book actually contains anything
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "Book must contain the name of the nation to break alliance with. Write the name on the first line of the book.");
        }

        // Check if target nation name is provided
        String targetNationName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (targetNationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player,
                    "You must provide a nation name. Write the nation's name on the first line of the book.");
        }

        // Check if target nation exists
        String targetNationUuid = nationDAO.getNationUUIDByName(targetNationName);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "No nation by that name exists.");
        }

        // Normalize the target nation name
        targetNationName = nationDAO.getNationName(targetNationUuid);
        if (targetNationName == null) {
            return sendErrorBook(lecternBlock, player,
                    "Could not get canonical name for target nation. Please contact an admin.");
        }

        // Check if the target nation is allied with the player's nation
        boolean isAllied = diplomacyDAO.hasRelation(playerNationUuid, targetNationUuid, ALLIANCE_RELATION);
        if (!isAllied) {
            return sendErrorBook(lecternBlock, player,
                    "You are not currently allied with " + targetNationName + ".");
        }

        // Create "Break Alliance" book
        ItemStack deliveryBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
        bookMeta.setTitle(BREAK_ALLIANCE_DELIVERY);
        bookMeta.setAuthor(player.getName());
        bookMeta.addPage("§lBreak Alliance§r" +
                "\n" +
                targetNationName +
                "\n" +
                getDate() + "\n\n" +
                "This book serves as a declaration to break the alliance with §o" + targetNationName + "§r." +
                "\n\n" +
                "Deliver this to the target nation's Diplomacy Lectern.");
        bookMeta.setLore(List.of("§7" + sourceNationName));

        plugin.getNationLogger().log(player.getName() + " from " + sourceNationName + " has made a broke alliance delivery book, targeting " + targetNationName);

        setDeliveryKeys(bookMeta, playerNationUuid, targetNationUuid, BREAK_ALLIANCE_DELIVERY_TYPE);
        deliveryBook.setItemMeta(bookMeta);
        return sendSuccessBook(lecternBlock, player, deliveryBook);
    }
}
