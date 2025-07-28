package com.blothera.book.handler.infobook.town;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.infobook.InfoBookHandler;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownJoinRequestDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.blothera.util.NationConstants.*;

public class ViewJoinRequestsHandler extends InfoBookHandler {

    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final TownLecternDAO townLecternDAO;
    private final TownJoinRequestDAO joinRequestDAO;

    public ViewJoinRequestsHandler(NationPlugin plugin) {
        super(plugin);
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        this.joinRequestDAO = plugin.getDatabase().getJoinRequestDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(REQUESTS_COMMAND, JOIN_REQUESTS_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    public BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "To view join requests, write the town name on the first page on the first line.");
        }

        String pageText;
        try {
            pageText = PlainTextComponentSerializer.plainText().serialize(meta.page(1));
        } catch (Exception e) {
            return sendErrorBook(lecternBlock, player,
                    "Failed to parse book. Contact an admin.");
        }

        String firstLine = pageText.split("\n")[0].trim();
        String secondLine = pageText.split("\n").length > 1 ? pageText.split("\n")[1].trim() : "";

        String townName = firstLine.equalsIgnoreCase(TOWN_HEADER) ? secondLine : firstLine;
        if (townName.isEmpty()) {
            return sendErrorBook(lecternBlock, player,
                    "You must write the name of the town on the first line of the book.");
        }

        String townUuid = townDAO.getTownUuidByName(townName);
        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "Town '" + townName + "' does not exist.");
        }

        String canonicalTownName = townDAO.getCanonicalTownName(townName);

        if (!townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock)) {
            return sendErrorBook(lecternBlock, player,
                    "This lectern is not within the borders of " + canonicalTownName + ".");
        }

        String originalAuthor = resolveAuthor(meta, lecternBlock, player);
        return rewriteRequestBook(lecternBlock, player, canonicalTownName, originalAuthor);
    }


    private BookResult rewriteRequestBook(Block lecternBlock, Player player, String townName, String author) {
        List<String> bookContents = new ArrayList<>();
        bookContents.add("§l" + TOWN_HEADER + "§r");
        bookContents.add(townName);
        bookContents.add(getDate());
        bookContents.add("");

        // Fetch join requests
        String townUuid = townDAO.getTownUuidByName(townName);
        List<String> joinRequests = joinRequestDAO.getRequestsForTown(townUuid);

        bookContents.add("§l" + JOIN_REQUESTS_COMMAND + "§r");

        if (joinRequests.isEmpty()) {
            bookContents.add("§oNo pending join requests.");
        } else {
            List<String> names = new ArrayList<>();
            for (String uuid : joinRequests) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                String name = target.getName() != null ? target.getName() : "Error: Player not found";
                names.add(name);
            }

            for (String name : names) {
                bookContents.add(" - " + name);
            }
        }

        List<String> pages = paginateLines((bookContents));
        return sendSuccessBook(lecternBlock, player, JOIN_REQUESTS_COMMAND, pages, townName, author);
    }
}
