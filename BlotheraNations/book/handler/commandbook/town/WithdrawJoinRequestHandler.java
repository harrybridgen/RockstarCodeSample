package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownJoinRequestDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import java.util.List;

import static com.blothera.util.NationConstants.*;

public class WithdrawJoinRequestHandler extends CommandBookHandler {

    private final TownJoinRequestDAO joinRequestDAO;
    private final TownDAO townDAO;
    private final TownLecternDAO townLecternDAO;

    public WithdrawJoinRequestHandler(NationPlugin plugin) {
        super(plugin);
        this.joinRequestDAO = plugin.getDatabase().getJoinRequestDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(WITHDRAW_JOIN_COMMAND, STOP_JOIN_COMMAND, CANCEL_JOIN_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "You must write the name of the town.");
        }
        String playerUuid = player.getUniqueId().toString();
        String townName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (townName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "You must specify the town name.");
        }

        String townUuid = townDAO.getTownUuidByName(townName);
        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player, "No town named '" + townName + "' exists.");
        }

        if (!joinRequestDAO.hasRequestToTown(playerUuid, townUuid)) {
            return sendErrorBook(lecternBlock, player, "You do not have a pending request to " + townName + ".");
        }

        townName = townDAO.getTownName(townUuid);

        joinRequestDAO.deleteRequest(playerUuid, townUuid);

        plugin.getNationLogger().log(player.getName() + " has withdrawn their join request to " + townName + ".");
        return sendSuccessBook(lecternBlock, player,
                "Petition Withdrawn",
                "§lRequest Cancelled§r" +
                        "\n" +
                        townName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "You have withdrawn your request to join §o" + townName + "§r." +
                        "\n\n" +
                        "The elders shall hear no more of your petition.",
                townName
        );
    }
}
