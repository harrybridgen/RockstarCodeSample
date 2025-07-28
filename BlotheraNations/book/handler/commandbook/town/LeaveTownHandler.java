package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import com.blothera.database.TownDAOs.TownMemberDAO;
import com.blothera.event.town.TownMemberLeaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import java.util.List;

import static com.blothera.util.NationConstants.LEAVE_TOWN_COMMAND;

public class LeaveTownHandler extends CommandBookHandler {

    private final TownDAO townDAO;
    private final TownLecternDAO townLecternDAO;
    private final TownMemberDAO townMemberDAO;

    public LeaveTownHandler(NationPlugin plugin) {
        super(plugin);
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        this.townMemberDAO = plugin.getDatabase().getTownMemberDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(LEAVE_TOWN_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String playerUuid = player.getUniqueId().toString();

        // Get town UUID at this lectern
        String townUuid = plugin.getDatabase().getTownClaimDAO().getTownIdAt(
                lecternBlock.getWorld().getName(),
                lecternBlock.getChunk().getX(),
                lecternBlock.getChunk().getZ()
        );
        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player, "This lectern is not in a claimed town.");
        }

        // Check if player is even a member
        if (!townMemberDAO.isMemberOfTown(playerUuid, townUuid)) {
            return sendErrorBook(lecternBlock, player, "You are not a member of this town.");
        }

        // Prevent town leaders from leaving
        String leaderUuid = townDAO.getLeaderUuid(townUuid);
        if (playerUuid.equals(leaderUuid)) {
            return sendErrorBook(lecternBlock, player, "You are the leader of this town. You must transfer leadership before leaving.");
        }

        // Remove the player
        boolean removed = townMemberDAO.removeMember(playerUuid, townUuid);
        if (!removed) {
            return sendErrorBook(lecternBlock, player, "Failed to leave the town. Contact an admin.");
        }
        Bukkit.getPluginManager().callEvent(new TownMemberLeaveEvent(townUuid, playerUuid));

        String townName = townDAO.getTownName(townUuid);
        return sendSuccessBook(lecternBlock, player,
                "Notice of Departure",
                "§lNotice of Departure§r" +
                        "\n" +
                        player.getName() + "\n" +
                        getDate() + "\n\n" +
                        "You have officially left the town of §o" + townName + "§r." +
                        "\n\n" +
                        "Your citizenship and residence within this town are now revoked.",
                townName
        );
    }
}
