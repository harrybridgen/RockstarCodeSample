package com.blothera.book.handler.infobook.nation;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.infobook.InfoBookHandler;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationLecternDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static com.blothera.util.NationConstants.*;

public class NationInfoBookHandler extends InfoBookHandler {

    private final NationLecternDAO nationLecternDAO;
    private final NationDAO nationDAO;
    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;

    public NationInfoBookHandler(NationPlugin plugin) {
        super(plugin);
        this.nationLecternDAO = plugin.getDatabase().getNationLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(INFO_COMMAND, NATION_INFO_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return nationLecternDAO.isNationLectern(lecternBlock);
    }

    @Override
    public BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "This Information book is missing a valid nation name.");
        }

        String firstPage;
        try {
            firstPage = PlainTextComponentSerializer.plainText().serialize(meta.page(1));
        } catch (Exception e) {
            return sendErrorBook(lecternBlock, player,
                    "Failed to read Information book. Contact an admin.");
        }

        String firstLine = firstPage.split("\n")[0].trim();
        String secondLine = firstPage.split("\n").length > 1 ? firstPage.split("\n")[1].trim() : "";

        String nationName = firstLine.equalsIgnoreCase(NATION_HEADER) ? secondLine : firstLine;
        if (nationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player,
                    "This Information book is missing a valid nation name.");
        }

        String nationUuid = nationDAO.getNationUUIDByName(nationName);
        if (nationUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "Nation '" + nationName + "' not found.");
        }

        boolean isInTheirOwnTerritory = townDAO.getTownsByNationUuid(nationUuid).stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock));
        if (!isInTheirOwnTerritory) {
            return sendErrorBook(lecternBlock, player,
                    "This lectern is not inside claimed territory.");
        }

        String canonicalNationName = nationDAO.getNationName(nationUuid);
        if (canonicalNationName == null) {
            return sendErrorBook(lecternBlock, player,
                    "Nation '" + nationName + "' not found.");
        }

        String originalAuthor = resolveAuthor(meta, lecternBlock, player);
        return rewriteNationInfoBook(lecternBlock, player, canonicalNationName, originalAuthor);
    }

    private BookResult rewriteNationInfoBook(Block lecternBlock, Player player, String nationName, String author) {
        StringBuilder bookContents = new StringBuilder();

        try (PreparedStatement stmt = plugin.getDatabase().getConnection().prepareStatement(
                "SELECT * FROM nations WHERE LOWER(name) = LOWER(?)")) {
            stmt.setString(1, nationName.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return sendErrorBook(lecternBlock, player, "Nation '" + nationName + "' not found.");
                } else {
                    bookContents.append("§l" + NATION_HEADER + "§r\n")
                            .append(rs.getString("name")).append("\n")
                            .append(getDate())
                            .append("\n")
                            .append("\n§lFounded§r\n")
                            .append(formatDate((rs.getString("founded_at"))))
                            .append("\n")
                            .append("\n§lAdmin Range§r\n")
                            .append(townDAO.getCapitalRadius(nationDAO.getNationUUIDByName(nationName))).append(" blocks\n");
                    bookContents.append("\n§lTowns§r\n");

                    try (PreparedStatement townStmt = plugin.getDatabase().getConnection().prepareStatement(
                            "SELECT name, is_capital, is_dormant FROM towns WHERE nation_uuid = ?")) {
                        townStmt.setString(1, rs.getString("uuid"));
                        try (ResultSet towns = townStmt.executeQuery()) {
                            boolean foundTown = false;
                            while (towns.next()) {
                                foundTown = true;
                                String name = towns.getString("name");
                                boolean capital = towns.getInt("is_capital") == 1;
                                boolean dormant = towns.getInt("is_dormant") == 1;

                                bookContents.append(" - ");
                                if (dormant) {
                                    bookContents.append("§c"); // red
                                }
                                bookContents.append(name);
                                if (capital) {
                                    bookContents.append(" (Capital)");
                                }
                                bookContents.append("§r\n");
                            }
                            if (!foundTown) {
                                bookContents.append("§oNo towns, yet§r\n");
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load nation towns: " + e.getMessage());
                        bookContents.append("Error loading nation data. Contact an admin. \n");
                    }
                    bookContents.append("\n§lMembers§r\n");
                    String nationUuid = rs.getString("uuid");
                    String leaderUuid = rs.getString("leader_uuid");
                    try (PreparedStatement memberStmt = plugin.getDatabase().getConnection().prepareStatement(
                            "SELECT uuid FROM nation_members WHERE nation_uuid = ?")) {
                        memberStmt.setString(1, nationUuid);
                        try (ResultSet members = memberStmt.executeQuery()) {
                            boolean membersFound = false;
                            while (members.next()) {
                                String uuid = members.getString("uuid");
                                String name = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid)).getName();
                                bookContents.append(" - ")
                                        .append(name != null ? name : "Unknown");
                                membersFound = true;

                                if (leaderUuid != null && leaderUuid.equals(uuid)) {
                                    bookContents.append(" (Leader)");
                                }

                                bookContents.append("\n");
                            }
                            if (!membersFound) {
                                bookContents.append("Error: Failed to load members. Report this to an admin.\n");
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load nation members: " + e.getMessage());
                        bookContents.append("Error: Failed to load members. Report this to an admin.\n");
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Nation info fetch failed: " + e.getMessage());
            bookContents.setLength(0);
            bookContents.append("Error loading nation data. Contact an admin.");
        }

        List<String> pages = paginateString(String.valueOf(bookContents));
        return sendSuccessBook(lecternBlock, player, NATION_INFO_COMMAND, pages, nationName, author);
    }
}
