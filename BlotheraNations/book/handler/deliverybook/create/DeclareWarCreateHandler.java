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

public class DeclareWarCreateHandler extends DeliveryBookCreateHandler {

    private final NationDAO nationDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final DiplomacyDAO diplomacyDAO;

    public DeclareWarCreateHandler(NationPlugin plugin) {
        super(plugin);
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.diplomacyDAO = plugin.getDatabase().getDiplomacyDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(DECLARE_WAR_COMMAND, WAR_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String playerUuid = player.getUniqueId().toString();
        String sourceNationUuid = nationMemberDAO.getNationUuid(playerUuid);

        if (sourceNationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "You are not a member of any nation.");
        }

        String sourceNationName = nationDAO.getNationName(sourceNationUuid);
        if (sourceNationName == null) {
            return sendErrorBook(lecternBlock, player,
                    "Your nation does not exist or has been deleted. Contact an admin.");
        }

        if (!nationDAO.getLeaderUuid(sourceNationUuid).equals(playerUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "Only the nation leader can declare war.");
        }

        boolean inOwnTerritory = townDAO.getTownsByNationUuid(sourceNationUuid)
                .stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));

        if (!inOwnTerritory) {
            return sendErrorBook(lecternBlock, player,
                    "This diplomacy lectern is not within your nation's claimed territory.");
        }

        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "Book must contain the name of the nation you want to declare war on.");
        }

        String targetNationName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (targetNationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player,
                    "Target nation name cannot be empty. Write it on the first line of the book.");
        }

        String targetNationUuid = nationDAO.getNationUUIDByName(targetNationName);
        if (targetNationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "No nation by that name exists.");
        }

        targetNationName = nationDAO.getNationName(targetNationUuid);
        if (targetNationName == null) {
            return sendErrorBook(lecternBlock, player,
                    "Could not get canonical name for target nation. Please contact an admin.");
        }

        if (sourceNationUuid.equals(targetNationUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "You cannot declare war on your own nation.");
        }

        // Check if either nation is already in a war
        int sourceWarId = plugin.getDatabase().getWarDAO().getOngoingWarIdByNation(sourceNationUuid);
        int targetWarId = plugin.getDatabase().getWarDAO().getOngoingWarIdByNation(targetNationUuid);

        if (sourceWarId != -1 || targetWarId != -1) {
            return sendErrorBook(lecternBlock, player,
                    "Either your nation or " + targetNationName + " is already involved in another war.\n" +
                            "Nations can only be involved in one war at a time.\n\n" +
                            "If you'd like to join their conflict, use a §oJoin War Request§r book.");
        }

        if (diplomacyDAO.hasRelation(sourceNationUuid, targetNationUuid, WAR_RELATION)) {
            return sendErrorBook(lecternBlock, player,
                    "You are already at war with " + targetNationName + ".");
        }

        if (diplomacyDAO.hasRelation(sourceNationUuid, targetNationUuid, ALLIANCE_RELATION)) {
            return sendErrorBook(lecternBlock, player,
                    "You are currently allied with " + targetNationName + ". You must break the alliance first.");
        }


        // Create war declaration book
        ItemStack deliveryBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
        bookMeta.setTitle(WAR_DECLARATION_DELIVERY);
        bookMeta.setAuthor(player.getName());
        bookMeta.addPage("§lWar Declaration§r" +
                "\n" +
                targetNationName +
                "\n" +
                getDate() +
                "\n\n" +
                "To the leaders of §o" + targetNationName + "§r," +
                "\n\n" +
                "This book serves as a formal declaration of war." +
                "\n\n" +
                "Place this in their Diplomacy Lectern to declare war.");
        bookMeta.setLore(List.of("§7" + sourceNationName));

        plugin.getNationLogger().log(player.getName() + " made a war declaration delivery book, targeting " + targetNationName + ".");

        setDeliveryKeys(bookMeta, sourceNationUuid, targetNationUuid, DECLARE_WAR_DELIVERY_TYPE);
        deliveryBook.setItemMeta(bookMeta);
        return sendSuccessBook(lecternBlock, player, deliveryBook);
    }
}
