package com.blothera.book.handler.commandbook.nation;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import com.blothera.event.nation.NationMemberLeaveEvent;

import java.util.List;

import static com.blothera.util.NationConstants.*;

public class LeaveNationHandler extends CommandBookHandler {

    private final NationDAO nationDAO;
    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final NationMemberDAO nationMemberDAO;

    public LeaveNationHandler(NationPlugin plugin) {
        super(plugin);
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(LEAVE_NATION_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return true;
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String playerUuid = player.getUniqueId().toString();
        String nationUuid = nationMemberDAO.getNationUuid(playerUuid);

        // Check if they are in a nation
        if (nationUuid == null) {
            return sendErrorBook(lecternBlock, player, "You are not in a nation.");
        }

        // Check if the player is a leader
        String leaderUuid = nationDAO.getLeaderUuid(nationUuid);
        if (playerUuid.equals(leaderUuid)) {
            return sendErrorBook(lecternBlock, player, "Leaders cannot leave their own nation.");
        }

        // Check if the lectern is in town territory
        List<String> townUuids = townDAO.getTownsByNationUuid(nationUuid);
        boolean inClaimedTown = townUuids.stream().anyMatch(townUuid ->
                townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock)
        );
        if (!inClaimedTown) {
            return sendErrorBook(lecternBlock, player, "This lectern is not within your nation's claimed territory.");
        }

        // Check if the player is a leader of a town in the nation
        boolean isTownLeader = townUuids.stream().anyMatch(townUuid ->
                townDAO.isTownLeader(playerUuid, townUuid)
        );
        if (isTownLeader) {
            return sendErrorBook(lecternBlock, player, "You are a leader of a town in this nation. You must step down before leaving.");
        }

        String nationName = nationDAO.getNationName(nationUuid);
        if (nationName == null) {
            return sendErrorBook(lecternBlock, player, "Your nation does not exist or has been deleted. Contact an admin.");
        }

        // Remove the player from the nation
        nationMemberDAO.removeMember(playerUuid);
        Bukkit.getPluginManager().callEvent(new NationMemberLeaveEvent(nationUuid, playerUuid));

        return sendSuccessBook(lecternBlock, player,
                "Citizenship Renounced",
                "§lCitizenship Renounced§r" +
                        "\n" +
                        nationName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "§o" + player.getName() + "§r has formally renounced their ties to the nation of §o" + nationName + "§r." +
                        "\n\n" +
                        "From this day forward, they walk the land unaffiliated.",
                nationName);

    }
}
