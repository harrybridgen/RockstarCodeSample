package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.*;
import com.blothera.event.town.BanditCheckEvent;
import com.blothera.event.town.TownJoinRequestEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.JOIN_COMMAND;
import static com.blothera.util.NationConstants.JOIN_TOWN_COMMAND;

public class RequestJoinHandler extends CommandBookHandler {

    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownJoinRequestDAO joinRequestDAO;
    private final TownLecternDAO townLecternDAO;
    private final TownMemberDAO townMemberDAO;

    public RequestJoinHandler(NationPlugin plugin) {
        super(plugin);
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.joinRequestDAO = plugin.getDatabase().getJoinRequestDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        this.townMemberDAO = plugin.getDatabase().getTownMemberDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(JOIN_COMMAND, JOIN_TOWN_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String playerUuid = player.getUniqueId().toString();

        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "You must write the name of the town.");
        }

        String townName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (townName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "You must write the name of the town.");
        }

        String townUuid = townDAO.getTownUuidByName(townName);
        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player, "No town by that name exists.");
        }

        townName = townDAO.getTownName(townUuid);
        if (townName == null) {
            return sendErrorBook(lecternBlock, player, "No town by that name exists.");
        }

        if (!townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock)) {
            return sendErrorBook(lecternBlock, player, "This lectern is not within the territory of " + townName + ".");
        }

        // check if the player is already a citizen of the town
        if (townMemberDAO.isMemberOfTown(playerUuid, townUuid)) {
            return sendErrorBook(lecternBlock, player, "The land shall not be fooled!\n\nYou are already a citizen of " + townName + ".");
        }

        if (joinRequestDAO.hasRequestToTown(playerUuid, townUuid)) {
            return sendErrorBook(lecternBlock, player, "You already requested to join this town.");
        }

        String townNationUuid = townDAO.getNationUuidFromTownUuid(townUuid);
        String playerNationUuid = nationMemberDAO.getNationUuid(playerUuid);

        if (playerNationUuid == null) {
            for (String requestedTownUuid : joinRequestDAO.getRequestedTownUuids(playerUuid)) {
                String requestedNationUuid = townDAO.getNationUuidFromTownUuid(requestedTownUuid);
                if (requestedNationUuid != null && requestedNationUuid.equals(townNationUuid)) {
                    return sendErrorBook(lecternBlock, player,
                            """
                                    You already have a request to a town in this nation.
                                    
                                    You must wait for that request to be processed before requesting
                                    to join another town in the same nation.""");
                }
            }
        } else {
            if (!playerNationUuid.equals(townNationUuid)) {
                return sendErrorBook(lecternBlock, player, "You cannot join a town from a different nation.");
            }
        }

        BanditCheckEvent checkEvent = new BanditCheckEvent(player);
        Bukkit.getPluginManager().callEvent(checkEvent);
        if (checkEvent.isCancelled()) {
            return sendErrorBook(lecternBlock, player, checkEvent.getCancelReason());
        }

        joinRequestDAO.createRequest(playerUuid, townUuid);
        Bukkit.getPluginManager().callEvent(new TownJoinRequestEvent(townUuid, playerUuid));
        return sendSuccessBook(lecternBlock, player,
                "Petition for Citizenship",
                "§lCitizenship Request§r" +
                        "\n" +
                        townName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "A formal request has been sent to the town of §o" + townName + "§r." +
                        "\n\n" +
                        "The elders will consider your plea.",
                townName);

    }

}
