package com.blothera.book.handler.deliverybook.create;

import com.blothera.NationPlugin;
import com.blothera.book.handler.deliverybook.DeliveryBookCreateHandler;
import com.blothera.book.BookResult;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.*;

/**
 * First stage handler for relocating a nation's capital.
 * <p>
 * The nation leader uses a Town Lectern in the current capital to create
 * a delivery book, which must then be placed in a Town Lectern in the target
 * town to finalize the move.
 */
public class RelocateCapitalCreateHandler extends DeliveryBookCreateHandler {

    private final NationDAO nationDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownLecternDAO townLecternDAO;

    public RelocateCapitalCreateHandler(NationPlugin plugin) {
        super(plugin);
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(
                RELOCATE_CAPITAL_COMMAND,
                CHANGE_CAPITAL_COMMAND,
                MOVE_CAPITAL_COMMAND
        );
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {

        String playerUuid = player.getUniqueId().toString();
        String nationUuid = nationMemberDAO.getNationUuid(playerUuid);
        if (nationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "You are not part of any nation.");
        }

        if (!playerUuid.equals(nationDAO.getLeaderUuid(nationUuid))) {
            return sendErrorBook(lecternBlock, player,
                    "Only the nation leader can relocate the capital.");
        }

        String sourceTown = townClaimDAO.getTownIdAt(
                lecternBlock.getWorld().getName(),
                lecternBlock.getChunk().getX(),
                lecternBlock.getChunk().getZ()
        );
        if (sourceTown == null || !townDAO.isCapital(sourceTown) || !sourceTown.equals(nationDAO.getCapitalTownUuid(nationUuid))) {
            return sendErrorBook(lecternBlock, player,
                    "This lectern is not inside your current capital town.");
        }

        String sourceTownName = townDAO.getTownName(sourceTown);
        if (sourceTownName == null) {
            return sendErrorBook(lecternBlock, player,
                    "Could not retrieve current capital town canonical name. Contact an admin.");
        }

        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "You must write the name of the new capital town.");
        }

        String targetTownName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (targetTownName.isEmpty()) {
            return sendErrorBook(lecternBlock, player,
                    "Town name cannot be empty.");
        }

        String targetTownUuid = townDAO.getTownUuidByName(targetTownName);
        if (targetTownUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "That town does not exist.");
        }

        targetTownName = townDAO.getTownName(targetTownUuid);
        if (targetTownName == null) {
            return sendErrorBook(lecternBlock, player,
                    "Could not retrieve target town canonical name. Contact an admin.");
        }

        if (!nationUuid.equals(townDAO.getNationUuidFromTownUuid(targetTownUuid))) {
            return sendErrorBook(lecternBlock, player,
                    "That town does not belong to your nation.");
        }

        if (townDAO.isCapital(targetTownUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "This town is already the capital of your nation.");
        }

        ItemStack deliveryBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
        bookMeta.setTitle(CAPITAL_RELOCATION_DELIVERY);
        bookMeta.setAuthor(player.getName());
        bookMeta.addPage("§l" + CAPITAL_RELOCATION_DELIVERY + "§r" +
                "\n" +
                targetTownName +
                "\n" +
                getDate() +
                "\n\n" +
                "Deliver this book to a Town Lectern in §o" + targetTownName + "§r to enact the relocation.");
        String lore = townDAO.getCanonicalTownName(sourceTownName);
        bookMeta.setLore(List.of("§7" + lore));

        plugin.getNationLogger().log(player.getName() + " created a capital relocation book targeting " + targetTownName);

        setDeliveryKeys(bookMeta, nationUuid, targetTownUuid, RELOCATE_CAPITAL_DELIVERY_TYPE);
        deliveryBook.setItemMeta(bookMeta);
        return sendSuccessBook(lecternBlock, player, deliveryBook);
    }
}
