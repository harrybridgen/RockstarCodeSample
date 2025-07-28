package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import com.blothera.event.town.TownRenamedEvent;

import java.util.List;

import static com.blothera.util.NationConstants.RENAME_TOWN_COMMAND;

public class RenameTownHandler extends CommandBookHandler {

    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final TownLecternDAO townLecternDAO;
    private final NationDAO nationDAO;

    public RenameTownHandler(NationPlugin plugin) {
        super(plugin);
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(RENAME_TOWN_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {

        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "You must write the new name on the first page.");
        }

        String newTownName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (newTownName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Town name cannot be empty.");
        }

        if (newTownName.length() < 3) {
            return sendErrorBook(lecternBlock, player, "The name " + newTownName + " is too short for your bad eyes.");
        }

        if (newTownName.length() > 24) {
            return sendErrorBook(lecternBlock, player, "The name " + newTownName + " is too long and would not fit on book pages.");
        }

        if (!isValidName(newTownName)) {
            return sendErrorBook(lecternBlock, player, "The name '" + newTownName + "' is rejected by the land.");
        }

        if (townDAO.townExists(newTownName)) {
            return sendErrorBook(lecternBlock, player, "A town with that name already exists.");
        }

        String townUuid = townClaimDAO.getTownIdAt(
                lecternBlock.getWorld().getName(),
                lecternBlock.getChunk().getX(),
                lecternBlock.getChunk().getZ()
        );

        if (townUuid == null || !townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock)) {
            return sendErrorBook(lecternBlock, player, "This lectern is not within the town's claimed territory.");
        }

        if (townDAO.isDormant(townUuid)) {
            return sendErrorBook(lecternBlock, player, "This town is dormant and cannot be managed.");
        }

        if (!townDAO.getLeaderUuid(townUuid).equals(player.getUniqueId().toString())) {
            return sendErrorBook(lecternBlock, player, "Only the town leader can rename the town.");
        }

        String oldTownName = townDAO.getTownName(townUuid);
        String nationName = nationDAO.getNationNameByTownUuid(townUuid);
        townDAO.renameTown(townUuid, newTownName);
        Bukkit.getPluginManager().callEvent(new TownRenamedEvent(townUuid, townDAO.getNationUuidFromTownUuid(townUuid), oldTownName, newTownName));
        String playerName = player.getName();
        String date = getDate();
        return sendSuccessBook(lecternBlock, player,
                "Proclamation of Renaming",
                "§lTown Renamed§r" +
                        "\n" +
                        newTownName +
                        "\n" +
                        date +
                        "\n\n" +
                        "Let it be known that henceforth, this town shall be called §o" + newTownName + "§r." +
                        "\n\n" +
                        "The name has been etched into the memory of the land.",
                newTownName
        );

    }

}
