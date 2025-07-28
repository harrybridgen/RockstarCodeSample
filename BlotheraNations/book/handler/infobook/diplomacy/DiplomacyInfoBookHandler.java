package com.blothera.book.handler.infobook.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.infobook.InfoBookHandler;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static com.blothera.util.NationConstants.*;

public class DiplomacyInfoBookHandler extends InfoBookHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final NationDAO nationDAO;
    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;

    public DiplomacyInfoBookHandler(NationPlugin plugin) {
        super(plugin);
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(INFO_COMMAND, DIPLOMACY_INFO_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    public BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "This Information book is missing a valid nation name.");
        }

        String page;
        try {
            page = PlainTextComponentSerializer.plainText().serialize(meta.page(1));
        } catch (Exception e) {
            return sendErrorBook(lecternBlock, player,
                    "Failed to read Information book. Contact an admin.");
        }

        String[] lines = page.split("\n");
        String firstLine = lines.length > 0 ? lines[0].trim() : "";
        String secondLine = lines.length > 1 ? lines[1].trim() : "";

        String nationName = firstLine.equalsIgnoreCase(DIPLOMACY_HEADER) ? secondLine : firstLine;
        if (nationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player,
                    "This Information book is missing a valid nation name.");
        }

        String nationUuid = nationDAO.getNationUUIDByName(nationName);
        if (nationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "Nation '" + nationName + "' does not exist.");
        }

        String canonicalNationName = nationDAO.getNationName(nationUuid);
        if (canonicalNationName == null) {
            return sendErrorBook(lecternBlock, player,
                    "Error retrieving nation name for UUID: " + nationUuid + ", please contact an admin.");
        }

        boolean isInTheirOwnTerritory = townDAO.getTownsByNationUuid(nationUuid).stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));

        if (!isInTheirOwnTerritory) {
            return sendErrorBook(lecternBlock, player,
                    "This lectern is not inside " + canonicalNationName + "'s claimed territory.");
        }

        String originalAuthor = resolveAuthor(meta, lecternBlock, player);
        return generateBook(player, lecternBlock, canonicalNationName, originalAuthor);
    }

    private BookResult generateBook(Player player, Block lecternBlock, String nationName, String author) {
        StringBuilder bookContents = new StringBuilder();
        try {
            String nationUuid = nationDAO.getNationUUIDByName(nationName);
            if (nationUuid == null) {
                return sendErrorBook(lecternBlock, player, "Nation '" + nationName + "' not found.");
            }

            bookContents.append("§l" + DIPLOMACY_HEADER + "§r\n")
                    .append(nationName)
                    .append("\n")
                    .append(getDate())
                    .append("\n\n")
                    .append("§lRelations§r\n");

            boolean hasRelations = false;
            try (PreparedStatement stmt = plugin.getDatabase().getConnection().prepareStatement(
                    "SELECT n.name, d.relation_type " +
                            "FROM diplomatic_relations d " +
                            "JOIN nations n ON n.uuid = CASE " +
                            "    WHEN d.nation_a = ? THEN d.nation_b " +
                            "    WHEN d.nation_b = ? THEN d.nation_a " +
                            "    ELSE NULL END " +
                            "WHERE d.nation_a = ? OR d.nation_b = ?"
            )) {
                stmt.setString(1, nationUuid);
                stmt.setString(2, nationUuid);
                stmt.setString(3, nationUuid);
                stmt.setString(4, nationUuid);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        hasRelations = true;
                        String otherName = rs.getString("name");
                        String type = rs.getString("relation_type");
                        bookContents.append(" - ").append(otherName)
                                .append(" (").append(type).append(")\n");
                    }
                }
            }

            if (!hasRelations) {
                bookContents.append("§oNo diplomatic relations.§r\n");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Diplomacy info load error: " + e.getMessage());
            bookContents.setLength(0);
            bookContents.append("Error loading diplomacy info. Contact an admin.");
        }

        List<String> pages = paginateString(bookContents.toString());
        return sendSuccessBook(lecternBlock, player, DIPLOMACY_INFO_COMMAND, pages, nationName, author);
    }
}
