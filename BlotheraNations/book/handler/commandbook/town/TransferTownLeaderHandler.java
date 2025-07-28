package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownMemberDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import com.blothera.event.town.TownLeaderChangedEvent;
import java.util.List;

import static com.blothera.util.NationConstants.TRANSFER_TOWN_LEADERSHIP_COMMAND;

public class TransferTownLeaderHandler extends CommandBookHandler {

    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final TownMemberDAO townMemberDAO;

    public TransferTownLeaderHandler(NationPlugin plugin) {
        super(plugin);
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townMemberDAO = plugin.getDatabase().getTownMemberDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(TRANSFER_TOWN_LEADERSHIP_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return lecternBlock != null && lecternBlock.getType().name().endsWith("LECTERN");
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "The book is empty. Write the new leader's name on the first line.");
        }
        String page = PlainTextComponentSerializer.plainText().serialize(meta.page(1));
        String targetName = page.split("\n")[0].trim();

        if (targetName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "The new leader's name cannot be empty. Write it on the first line.");
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getName() == null && !target.isOnline()) {
            return sendErrorBook(lecternBlock, player, "This person is not known to the land.");
        }

        String townUuid = townClaimDAO.getTownIdAt(
                lecternBlock.getWorld().getName(),
                lecternBlock.getChunk().getX(),
                lecternBlock.getChunk().getZ()
        );
        if (townUuid == null || !townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock)) {
            return sendErrorBook(lecternBlock, player, "You must enact this command within your town's territory.");
        }

        if (townDAO.isDormant(townUuid)) {
            return sendErrorBook(lecternBlock, player, "This town is dormant and cannot be managed.");
        }

        String playerUuid = player.getUniqueId().toString();
        String currentLeader = townDAO.getLeaderUuid(townUuid);
        if (currentLeader == null || !currentLeader.equals(playerUuid)) {
            return sendErrorBook(lecternBlock, player, "Only the town leader may transfer leadership. You are not the leader of this town.");
        }

        String targetUuid = target.getUniqueId().toString();
        if (playerUuid.equals(targetUuid)) {
            return sendErrorBook(lecternBlock, player, "The land will not be fooled! You cannot transfer leadership to yourself.");
        }

        if (!townMemberDAO.isMemberOfTown(targetUuid, townUuid)) {
            return sendErrorBook(lecternBlock, player, "This person is not a member of your town. They must be a citizen to become leader.");
        }

        if (townDAO.isTownLeader(targetUuid)) {
            return sendErrorBook(lecternBlock, player, "This person is already a leader of another town. They cannot lead two towns at once.");
        }

        String oldLeader = townDAO.getLeaderUuid(townUuid);
        townDAO.changeLeader(townUuid, targetUuid);
        Bukkit.getPluginManager().callEvent(new TownLeaderChangedEvent(townUuid, oldLeader, targetUuid));
        String townName = townDAO.getTownName(townUuid);

        return sendSuccessBook(lecternBlock, player,
                "Edict of Succession",
                "§lLeadership Transfer§r" +
                        "\n" +
                        townName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "§o" + player.getName() + "§r has entrusted the leadership of §o" + townName + "§r to §o" + targetName + "§r." +
                        "\n\n" +
                        "May the town flourish under their guidance, and may their name be remembered in the land’s long memory.",
                townName);

    }
}
