package com.blothera.book.handler.infobook.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.infobook.InfoBookHandler;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyRequestsDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyRequestsDAO.DiplomacyRequest;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

import static com.blothera.util.NationConstants.*;

public class ViewDiplomacyRequestsHandler extends InfoBookHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final NationDAO nationDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final DiplomacyRequestsDAO diplomacyRequestsDAO;

    public ViewDiplomacyRequestsHandler(NationPlugin plugin) {
        super(plugin);
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.diplomacyRequestsDAO = plugin.getDatabase().getDiplomacyRequestsDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(REQUESTS_COMMAND, DIPLOMACY_REQUESTS_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    public BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "You must write the name of your nation on the first page.");
        }

        String pageText;
        try {
            pageText = PlainTextComponentSerializer.plainText().serialize(meta.page(1));
        } catch (Exception e) {
            return sendErrorBook(lecternBlock, player,
                    "Failed to parse book. Make sure it contains a nation name.");
        }

        String firstLine = pageText.split("\n")[0].trim();
        String secondLine = pageText.split("\n").length > 1 ? pageText.split("\n")[1].trim() : "";

        String nationName = firstLine.equalsIgnoreCase(DIPLOMACY_HEADER) ? secondLine : firstLine;
        if (nationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Nation name cannot be empty.");
        }

        String nationUuid = nationDAO.getNationUUIDByName(nationName);
        if (nationUuid == null) {
            return sendErrorBook(lecternBlock, player, "No nation by that name exists.");
        }

        String canonicalNationName = nationDAO.getNationName(nationUuid);
        if (canonicalNationName == null) {
            return sendErrorBook(lecternBlock, player,
                    "Failed to retrieve nation name for UUID: " + nationUuid);
        }

        String world = lecternBlock.getWorld().getName();
        int chunkX = lecternBlock.getChunk().getX();
        int chunkZ = lecternBlock.getChunk().getZ();

        String townUuid = townClaimDAO.getTownIdAt(world, chunkX, chunkZ);
        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "This lectern is not within any claimed town.");
        }

        String townNationUuid = townDAO.getNationUuidFromTownUuid(townUuid);
        if (!nationUuid.equals(townNationUuid)) {
            return sendErrorBook(lecternBlock, player,
                    "This lectern is not within your nation's territory.");
        }

        String originalAuthor = resolveAuthor(meta, lecternBlock, player);
        return rewriteRequestBook(lecternBlock, player, canonicalNationName, nationUuid, originalAuthor);
    }

    private BookResult rewriteRequestBook(Block lecternBlock, Player player, String nationName, String nationUuid, String author) {
        List<String> bookContents = new ArrayList<>();
        bookContents.add("§l" + DIPLOMACY_HEADER + "§r");
        bookContents.add(nationName);
        bookContents.add(getDate());
        bookContents.add("");

        List<DiplomacyRequest> requests = diplomacyRequestsDAO.getPendingRequests(nationUuid);

        bookContents.add("§lRequests§r");

        if (requests.isEmpty()) {
            bookContents.add("§oNo pending diplomacy requests.");
        } else {
            for (DiplomacyRequest req : requests) {
                String fromName = nationDAO.getNationName(req.fromNationUuid());
                String type = req.type().substring(0, 1).toUpperCase() + req.type().substring(1).toLowerCase();

                bookContents.add("- " + fromName + " (" + type + ")");
                bookContents.add("§r"); // spacing
            }
        }

        List<String> pages = paginateLines(bookContents);
        return sendSuccessBook(lecternBlock, player, DIPLOMACY_REQUESTS_COMMAND, pages, nationName, author);
    }
}
