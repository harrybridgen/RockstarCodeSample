package com.blothera.book.handler.infobook.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.infobook.InfoBookHandler;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.WarDAOs.WarAlliesDAO;
import com.blothera.database.WarDAOs.WarBattleDAO;
import com.blothera.database.WarDAOs.WarBattleDAO.WarBattle;
import com.blothera.database.WarDAOs.WarDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class WarInfoBookHandler extends InfoBookHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;
    private final NationDAO nationDAO;
    private final TownDAO townDAO;
    private final TownClaimDAO townClaimDAO;
    private final WarDAO warDAO;
    private final WarAlliesDAO warAlliesDAO;
    private final WarBattleDAO warBattleDAO;

    public WarInfoBookHandler(NationPlugin plugin) {
        super(plugin);
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.warDAO = plugin.getDatabase().getWarDAO();
        this.warAlliesDAO = plugin.getDatabase().getWarAlliesDAO();
        this.warBattleDAO = plugin.getDatabase().getWarBattleDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of("War Info");
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }

    @Override
    public BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "You must write your nation name on the first page.");
        }

        String pageText;
        try {
            pageText = PlainTextComponentSerializer.plainText().serialize(meta.page(1));
        } catch (Exception e) {
            return sendErrorBook(lecternBlock, player, "Failed to parse book.");
        }

        String[] lines = pageText.split("\n");
        String firstLine = lines.length > 0 ? lines[0].trim() : "";
        String secondLine = lines.length > 1 ? lines[1].trim() : "";

        String nationName = firstLine.equalsIgnoreCase("War Info") ? secondLine : firstLine;
        if (nationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Nation name is required on the first page.");
        }

        String nationUuid = nationDAO.getNationUUIDByName(nationName);
        if (nationUuid == null) {
            return sendErrorBook(lecternBlock, player, "Nation '" + nationName + "' does not exist.");
        }

        if (!townDAO.getTownsByNationUuid(nationUuid).stream()
                .anyMatch(townUuid -> townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock))) {
            return sendErrorBook(lecternBlock, player, "This lectern is not inside " + nationName + "'s territory.");
        }

        String originalAuthor = resolveAuthor(meta, lecternBlock, player);

        String cononicalNationName = nationDAO.getNationName(nationUuid);
        if (cononicalNationName == null) {
            return sendErrorBook(lecternBlock, player, "Error retrieving nation name for UUID: " + nationUuid + ", please contact an admin.");
        }
        
        int warId = warDAO.getOngoingWarIdByNation(nationUuid);
        if (warId == -1) {
            return generateNoWarBook(lecternBlock, player, cononicalNationName, originalAuthor);
        }

        return generateWarBook(lecternBlock, player, warId, cononicalNationName, originalAuthor);
    }

    private BookResult generateNoWarBook(Block lecternBlock, Player player, String nationName, String author) {
        List<String> book = new ArrayList<>();
        book.add("§lWar Info§r");
        book.add(nationName);
        book.add(getDate());
        book.add("");
        book.add("§oThis nation is not currently involved in any wars.§r");

        List<String> pages = paginateLines(book);
        return sendSuccessBook(lecternBlock, player, "War Info", pages, nationName, author);
    }

    private BookResult generateWarBook(Block lecternBlock, Player player, int warId, String nationName, String author) {
        List<String> book = new ArrayList<>();
        book.add("§lWar Info§r");
        book.add(nationName);
        book.add(getDate());
        book.add("");

        WarDAO.War war = warDAO.getWarById(warId);
        if (war == null) {
            return sendErrorBook(lecternBlock, player, "War data not found.");
        }

        String attackerName = nationDAO.getNationName(war.attackerNationUuid());
        String defenderName = nationDAO.getNationName(war.defenderNationUuid());

        book.add("§lWar Leaders§r");
        book.add("- Attacker: " + attackerName);
        book.add("- Defender: " + defenderName);
        book.add("");

        book.add("§lWar Allies§r");
        List<String> attackerAllies = warAlliesDAO.getAcceptedAllies(warId, "attacker");
        List<String> defenderAllies = warAlliesDAO.getAcceptedAllies(warId, "defender");

        book.add("Attacker Allies:");
        if (attackerAllies.isEmpty()) {
            book.add(" §oNone");
        } else {
            for (String ally : attackerAllies) {
                book.add(" - " + nationDAO.getNationName(ally));
            }
        }

        book.add("Defender Allies:");
        if (defenderAllies.isEmpty()) {
            book.add(" §oNone");
        } else {
            for (String ally : defenderAllies) {
                book.add(" - " + nationDAO.getNationName(ally));
            }
        }

        book.add("");
        book.add("§lUpcoming Battles§r");
        List<WarBattle> battles = warBattleDAO.getUpcomingBattles(warId);
        if (battles.isEmpty()) {
            book.add(" §oNo scheduled battles.");
        } else {
            String townUuid = townDAO.getTownsByNationUuid(nationDAO.getNationUUIDByName(nationName)).stream()
                    .filter(id -> townClaimDAO.isLecternInTownChunk(id, lecternBlock))
                    .findFirst()
                    .orElse(null);

            if (townUuid == null) {
                return sendErrorBook(lecternBlock, player, "Could not determine the town this lectern belongs to.");
            }

            Location townCenter = townClaimDAO.getFirstClaimCenterLocation(townUuid);
            if (townCenter == null) {
                return sendErrorBook(lecternBlock, player, "Could not resolve town center location.");
            }

            for (WarBattle battle : battles) {
                book.add("- " + battle.scheduledDate() + " (" + battle.slot() + ")");

                String[] parts = battle.location().split(",");
                if (parts.length != 3) {
                    book.add("  §oInvalid location format");
                    continue;
                }

                try {
                    int chunkX = Integer.parseInt(parts[1]);
                    int chunkZ = Integer.parseInt(parts[2]);

                    int townChunkX = townCenter.getBlockX() >> 4;
                    int townChunkZ = townCenter.getBlockZ() >> 4;

                    int dx = chunkX - townChunkX;
                    int dz = chunkZ - townChunkZ;
                    int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));

                    book.add("  Distance: " + dist + " chunks");

                } catch (NumberFormatException e) {
                    book.add("  §oInvalid coordinates in location");
                }
            }
        }

        List<String> pages = paginateLines(book);
        return sendSuccessBook(lecternBlock, player, "War Info", pages, nationName, author);
    }
}
