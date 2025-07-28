package com.blothera.book.handler.commandbook.nation;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationLecternDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.event.nation.NationLeaderChangedEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import java.util.List;

import static com.blothera.util.NationConstants.TRANSFER_NATION_LEADERSHIP_COMMAND;

public class TransferNationLeaderHandler extends CommandBookHandler {

    private final NationDAO nationDAO;
    private final NationMemberDAO nationMemberDAO;
    private final NationLecternDAO nationLecternDAO;

    public TransferNationLeaderHandler(NationPlugin plugin) {
        super(plugin);
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.nationLecternDAO = plugin.getDatabase().getNationLecternDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(TRANSFER_NATION_LEADERSHIP_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return nationLecternDAO.isNationLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "Enter a player name on the first line.");
        }
        String playerName = extractName(meta);
        if (playerName == null) {
            return sendErrorBook(lecternBlock, player, "Enter a player name on the first line.");
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (target.getName() == null && !target.isOnline()) {
            return sendErrorBook(lecternBlock, player, "Player '" + playerName + "' does not exist.");
        }

        String senderUuid = player.getUniqueId().toString();
        String targetUuid = target.getUniqueId().toString();

        String nationUuid = nationMemberDAO.getNationUuid(senderUuid);
        if (nationUuid == null) {
            return sendErrorBook(lecternBlock, player, "You are not part of a nation.");
        }

        String currentLeader = nationDAO.getLeaderUuid(nationUuid);
        if (currentLeader == null || !currentLeader.equals(senderUuid)) {
            return sendErrorBook(lecternBlock, player, "Only the nation leader can transfer leadership.");
        }

        if (!nationMemberDAO.isMemberOfNation(targetUuid, nationUuid)) {
            return sendErrorBook(lecternBlock, player, "Target is not a member of the nation.");
        }

        if (targetUuid.equals(senderUuid)) {
            return sendErrorBook(lecternBlock, player, "You cannot transfer leadership to yourself.");
        }

        String oldLeader = nationDAO.getLeaderUuid(nationUuid);
        nationDAO.changeLeader(nationUuid, targetUuid);
        Bukkit.getPluginManager().callEvent(new NationLeaderChangedEvent(nationUuid, oldLeader, targetUuid));

        String nationName = nationDAO.getNationName(nationUuid);
        return sendSuccessBook(lecternBlock, player,
                "New Leadership",
                "§lCrown Passed§r" +
                        "\n" +
                        playerName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "Leadership of §o" + nationName + "§r has been formally entrusted to §o" + playerName + "§r." +
                        "\n\n" +
                        "May they rule with wisdom, and the land remember the transfer.",
                nationName
        );

    }

    private String extractName(BookMeta meta) {
        if (meta.getPageCount() < 1) return null;
        String raw = PlainTextComponentSerializer.plainText().serialize(meta.page(1));
        return raw.split("\n")[0].trim();
    }
}
