package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import com.blothera.event.town.TownRemovedEvent;

import static com.blothera.util.NationConstants.DISBAND_TOWN_COMMAND;
import static com.blothera.util.NationConstants.DISSOLVE_TOWN_COMMAND;

public class DisbandTownHandler extends CommandBookHandler {

    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final TownLecternDAO townLecternDAO;

    public DisbandTownHandler(NationPlugin plugin) {
        super(plugin);
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(DISSOLVE_TOWN_COMMAND, DISBAND_TOWN_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {

        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "You must write the town name.");
        }

        String townName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (townName.isEmpty()) {
            return sendErrorBook(lecternBlock, player,
                    "Town name is missing.");
        }

        String townUuid = townDAO.getTownUuidByName(townName);
        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "No such town exists.");
        }

        if (townDAO.isDormant(townUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "This town is dormant and cannot be managed.");
        }

        // Leadership check
        String playerUuid = player.getUniqueId().toString();
        String townLeader = townDAO.getLeaderUuid(townUuid);

        if (!playerUuid.equals(townLeader)) {
            return sendErrorBook(lecternBlock, player,
                    "Only the town leader can disband this town.");
        }

        boolean isInTown = townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock);
        if (!isInTown) {
            return sendErrorBook(lecternBlock, player,
                    "This lectern is not within the town's claimed territory.");
        }

        boolean isCapital = townDAO.isCapital(townUuid);
        String nationUuid = townDAO.getNationUuidFromTownUuid(townUuid);

        boolean success = townDAO.removeTown(townUuid);

        if (!success) {
            sendErrorBook(lecternBlock, player,
                    "Failed to disband the town. Contact an admin if this persists.");
        }

        Bukkit.getPluginManager().callEvent(new TownRemovedEvent(townUuid, townName, nationUuid, isCapital));

        return sendSuccessBook(lecternBlock, player,
                "Dissolution of Town",
                "§lTown Disbanded§r" +
                        "\n" +
                        townName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "By decree of its leader, the town of §o" + townName + "§r has been formally dissolved." +
                        "\n\n" +
                        "Its claims are relinquished, its name consigned to history.",
                townName
        );

    }
}
