package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import com.blothera.database.TownDAOs.TownMemberDAO;
import com.blothera.event.town.TownTaxPaidEvent;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.PAY_TAX_COMMAND;
import static com.blothera.util.NationConstants.*;


public class PayTaxHandler extends CommandBookHandler {

    private final TownLecternDAO townLecternDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final TownMemberDAO townMemberDAO;

    public PayTaxHandler(NationPlugin plugin) {
        super(plugin);
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townMemberDAO = plugin.getDatabase().getTownMemberDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(PAY_TAX_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "You must write the town name on the first line.");
        }

        String townName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (townName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Town name missing.");
        }

        String townUuid = townDAO.getTownUuidByName(townName);
        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player, "No such town exists.");
        }

        String canonicalTownName = townDAO.getTownName(townUuid);

        Block chestBlock = getAdjacentChest(lecternBlock);
        if (!(chestBlock != null && chestBlock.getState() instanceof Chest chest)) {
            return sendErrorBook(lecternBlock, player, "A chest with emeralds must be beside the lectern.");
        }

        if (!townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock)) {
            return sendErrorBook(lecternBlock, player, "This lectern is not within that town's territory.");
        }

        String playerUuid = player.getUniqueId().toString();
        if (!townMemberDAO.isMemberOfTown(playerUuid, townUuid)) {
            return sendErrorBook(lecternBlock, player, "You are not a member of this town.");
        }

        String currentPaidUntil = townDAO.getTaxPaidUntil(townUuid);
        if (currentPaidUntil == null) {
            return sendErrorBook(lecternBlock, player, "This town has never paid tax before. Please contact an admin.");
        }

        Inventory inv = chest.getInventory();
        int emeralds = countEmeralds(inv);

        if (emeralds < BASE_TAX_EMERALDS) {
            return sendErrorBook(lecternBlock, player,
                    "Not enough emeralds in the chest. You need at least " + BASE_TAX_EMERALDS + " emeralds.");
        }

        deductEmeralds(inv, BASE_TAX_EMERALDS);

        java.time.LocalDate lastPaid = java.time.LocalDate.parse(currentPaidUntil);
        java.time.LocalDate newDate = lastPaid.isAfter(java.time.LocalDate.now())
                ? lastPaid.plusDays(BASE_DAYS_FOR_TAX)
                : java.time.LocalDate.now().plusDays(BASE_DAYS_FOR_TAX);
        townDAO.setTaxPaidUntil(townUuid, newDate.toString());
        townDAO.setDormant(townUuid, false);
        Bukkit.getPluginManager().callEvent(new TownTaxPaidEvent(townUuid, playerUuid, BASE_TAX_EMERALDS, lecternBlock.getLocation()));

        return sendSuccessBook(lecternBlock, player,
                "Tax Receipt",
                "§lTax Paid§r\n" + canonicalTownName + "\n" + getDate() + "\n\n" +
                        "Tax secured until " + formatDate(newDate.toString()) + ".",
                canonicalTownName
        );
    }
}
