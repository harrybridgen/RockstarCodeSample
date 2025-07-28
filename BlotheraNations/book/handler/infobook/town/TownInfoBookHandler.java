package com.blothera.book.handler.infobook.town;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.infobook.InfoBookHandler;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import static com.blothera.util.NationConstants.*;

public class TownInfoBookHandler extends InfoBookHandler {

    private final TownLecternDAO townLecternDAO;
    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;

    public TownInfoBookHandler(NationPlugin plugin) {
        super(plugin);
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(INFO_COMMAND, TOWN_INFO_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    public BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "To view town information, write the town name on the first line of the book.");
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

        String townName = firstLine.equalsIgnoreCase(TOWN_HEADER) ? secondLine : firstLine;
        if (townName.isEmpty()) {
            return sendErrorBook(lecternBlock, player,
                    "To view town information, write the town name on the first line of the book.");
        }

        String townUuid = townDAO.getTownUuidByName(townName);
        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player,
                    "Town '" + townName + "' does not exist.");
        }

        if (!townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock)) {
            return sendErrorBook(lecternBlock, player,
                    "This lectern is not within the territory of " + townName + ".");
        }

        String canonicalName = townDAO.getCanonicalTownName(townName);
        String originalAuthor = resolveAuthor(meta, lecternBlock, player);

        return updateTownInfoBook(lecternBlock, player, canonicalName, originalAuthor);
    }

    private BookResult updateTownInfoBook(Block lecternBlock, Player player, String townName, String author) {
        StringBuilder bookContents = new StringBuilder();
        try (PreparedStatement stmt = plugin.getDatabase().getConnection().prepareStatement(
                "SELECT t.uuid, t.name, t.founded_at, t.nation_uuid, t.is_capital, t.leader_uuid, " +
                        "t.tax_paid_until, t.is_dormant, n.name AS nation_name " +
                        "FROM towns t LEFT JOIN nations n ON t.nation_uuid = n.uuid WHERE LOWER(t.name) = LOWER(?)"
        )) {
            stmt.setString(1, townName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return sendErrorBook(lecternBlock, player, "Town '" + townName + "' not found.");
                }

                String townUuid = rs.getString("uuid");
                int claimCount = townClaimDAO.getNumberOfClaims(townUuid);

                String townActualName = rs.getString("name");
                String nationName = rs.getString("nation_name");
                String leaderUuid = rs.getString("leader_uuid");
                String taxPaidUntil = rs.getString("tax_paid_until");
                boolean dormant = rs.getInt("is_dormant") == 1;
                boolean hasTownLectern = plugin.getDatabase().getTownLecternDAO().townHasLectern(townUuid, townClaimDAO);
                boolean isCapital = rs.getInt("is_capital") == 1;
                boolean hasNationLectern = true;
                boolean hasDiplomacyLectern = true;

                if (isCapital) {
                    hasNationLectern = plugin.getDatabase().getNationLecternDAO().townHasLectern(townUuid, townClaimDAO);
                    hasDiplomacyLectern = plugin.getDatabase().getDiplomacyLecternDAO().townHasLectern(townUuid, townClaimDAO);
                }

                boolean hasRequiredLecterns = isCapital
                        ? (hasTownLectern && hasNationLectern && hasDiplomacyLectern)
                        : hasTownLectern;


                bookContents.append("§l" + TOWN_HEADER + "§r\n")
                        .append(townActualName).append("\n")
                        .append(getDate());

                if (nationName != null) {
                    bookContents.append("\n");
                    bookContents.append(isCapital ? "Capital of " : "Town in ")
                            .append(nationName);
                }

                bookContents
                        .append("\n\n")
                        .append("§lFounded§r")
                        .append("\n")
                        .append(formatDate((rs.getString("founded_at"))))
                        .append("\n")
                        .append("Chunks Claimed: ").append(claimCount >= 0 ? claimCount : "Unknown Error. Report to admin.")
                        .append("\n\n")
                        .append("§lStatus§r\n");
                if (dormant) {
                    bookContents.append("Dormant\n");

                    List<String> reasons = new ArrayList<>();
                    if (!hasRequiredLecterns) reasons.add("- Missing lecterns");
                    if (taxPaidUntil == null || java.time.LocalDate.now().isAfter(java.time.LocalDate.parse(taxPaidUntil))) {
                        reasons.add("- Unpaid taxes");
                    }
                    for (String reason : reasons) {
                        bookContents.append(reason).append("\n");
                    }
                    try {
                        if (taxPaidUntil != null) {
                            java.time.LocalDate taxDate = java.time.LocalDate.parse(taxPaidUntil);
                            bookContents.append("Tax Due: ").append(formatDate(taxPaidUntil)).append("\n");

                            java.time.LocalDate deletionDate = taxDate.plusDays(DORMANT_DAYS_TO_DELETE_TOWN);
                            bookContents.append("§cDisband on: ").append(formatDate(deletionDate.toString())).append("\n§0");
                        } else {
                            bookContents.append("Tax Due: Unknown (No tax date set, contact an admin)\n");
                        }
                    } catch (Exception e) {
                        bookContents.append("Tax Due: Unknown (Invalid date format, contact an admin)\n");
                    }
                } else {
                    bookContents.append("Active\n");
                    bookContents.append("Tax Due: ").append(formatDate(taxPaidUntil)).append("\n");
                }

                bookContents.append("\n")
                        .append("§lMembers§r")
                        .append("\n");

                try (PreparedStatement memberStmt = plugin.getDatabase().getConnection().prepareStatement(
                        "SELECT player_uuid FROM town_members WHERE town_uuid = ?")) {
                    memberStmt.setString(1, townUuid);
                    try (ResultSet members = memberStmt.executeQuery()) {
                        boolean found = false;
                        while (members.next()) {
                            found = true;
                            String uuid = members.getString("player_uuid");
                            String name = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
                            bookContents.append(" - ").append(name != null ? name : "Unknown");
                            if (uuid.equals(leaderUuid)) {
                                bookContents.append(" (Leader)");
                            }
                            bookContents.append("\n");
                        }
                        if (!found) {
                            bookContents.append("Error: No members found. Report this to an admin.\n");
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load town members: " + e.getMessage());
                    bookContents.append("Error: Loaded no members. Report to admin.\n");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load town info: " + e.getMessage());
            bookContents.setLength(0);
            bookContents.append("Error loading town data. Contact an admin.");
        }

        List<String> pages = paginateString(bookContents.toString());
        return sendSuccessBook(lecternBlock, player, TOWN_INFO_COMMAND, pages, townName, author);
    }
}
