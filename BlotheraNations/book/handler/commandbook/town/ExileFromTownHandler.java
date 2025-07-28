package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import com.blothera.database.TownDAOs.TownMemberDAO;
import com.blothera.event.town.TownMemberExileEvent;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.EXILE_TOWN_COMMAND;
import static com.blothera.util.NationConstants.KICK_TOWN_COMMAND;

public class ExileFromTownHandler extends CommandBookHandler {

    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final TownMemberDAO townMemberDAO;
    private final TownLecternDAO townLecternDAO;

    public ExileFromTownHandler(NationPlugin plugin) {
        super(plugin);
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townMemberDAO = plugin.getDatabase().getTownMemberDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(EXILE_TOWN_COMMAND, KICK_TOWN_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "Book must contain the player name.");
        }

        String targetName;
        try {
            String pageText = PlainTextComponentSerializer.plainText().serialize(meta.page(1));
            targetName = pageText.split("\n")[0].trim();
        } catch (Exception e) {
            return sendErrorBook(lecternBlock, player, "Could not parse book.");
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getName() == null && !target.isOnline()) {
            return sendErrorBook(lecternBlock, player, "That player does not exist.");
        }

        String targetUuid = target.getUniqueId().toString();

        // Get the town tied to the lectern
        String townUuid = townClaimDAO.getTownIdAt(
                lecternBlock.getWorld().getName(),
                lecternBlock.getChunk().getX(),
                lecternBlock.getChunk().getZ()
        );

        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player, "This lectern is not in a claimed town.");
        }

        if (townDAO.isDormant(townUuid)) {
            return sendErrorBook(lecternBlock, player, "This town is dormant and cannot be managed.");
        }

        // Leader check
        String leaderUuid = townDAO.getLeaderUuid(townUuid);
        if (!player.getUniqueId().toString().equals(leaderUuid)) {
            return sendErrorBook(lecternBlock, player, "You are not authorized to exile from this town.");
        }

        if (targetUuid.equals(player.getUniqueId().toString())) {
            return sendErrorBook(lecternBlock, player, "You can't exile yourself.");
        }

        // Confirm the player is a member of this town
        boolean isMember = townMemberDAO.isMemberOfTown(targetUuid, townUuid);
        if (!isMember) {
            return sendErrorBook(lecternBlock, player, "This player is not a member of this town.");
        }

        Bukkit.getPluginManager().callEvent(new TownMemberExileEvent(townUuid, targetUuid, player.getUniqueId().toString()));

        // Now do the actual removal AFTER
        boolean removed = townMemberDAO.removeMember(targetUuid, townUuid);
        if (!removed) {
            return sendErrorBook(lecternBlock, player, "Failed to exile the player. Contact an admin.");
        }
        String townName = townDAO.getTownName(townUuid);
        return sendSuccessBook(lecternBlock, player,
                "Edict of Exile",
                "§lEdict of Exile§r" +
                        "\n" +
                        targetName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "By the authority of its leader, §o" + targetName + "§r has been cast out from the town of §o" + townName + "§r." +
                        "\n\n" +
                        "Their presence is no longer recognized within these borders.",
                townName
        );

    }
}
