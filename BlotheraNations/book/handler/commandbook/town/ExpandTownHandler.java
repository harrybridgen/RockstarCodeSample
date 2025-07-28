package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import com.blothera.event.town.TownExpandedEvent;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.blothera.util.NationConstants.*;


public class ExpandTownHandler extends CommandBookHandler {

    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final TownLecternDAO townLecternDAO;

    public ExpandTownHandler(NationPlugin plugin) {
        super(plugin);
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(EXPAND_TOWN_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "You must write the name of the town.");
        }

        String townName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (townName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Town name missing.");
        }

        String townUuid = townDAO.getTownUuidByName(townName);
        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player, "No such town exists.");
        }

        townName = townDAO.getTownName(townUuid);
        if (townName == null) {
            return sendErrorBook(lecternBlock, player, "Town name not found. Contact an admin.");
        }

        String playerUuid = player.getUniqueId().toString();
        String leaderUuid = townDAO.getLeaderUuid(townUuid);
        if (!playerUuid.equals(leaderUuid)) {
            return sendErrorBook(lecternBlock, player, "Only the town leader may expand the town.");
        }

        if (!townClaimDAO.isLecternInTownChunk(townUuid, lecternBlock)) {
            return sendErrorBook(lecternBlock, player, "This lectern is not within the town's claimed territory.");
        }

        if (townDAO.isDormant(townUuid)) {
            return sendErrorBook(lecternBlock, player, "This town is dormant and cannot be managed.");
        }

        Block chestBlock = getAdjacentChest(lecternBlock);
        if (!(chestBlock != null && chestBlock.getState() instanceof Chest coffer)) {
            return sendErrorBook(lecternBlock, player, "No adjacent chest found.");
        }

        List<int[]> claims = townClaimDAO.getClaims(townUuid);
        if (claims.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Could not determine the town center. Contact an admin.");
        }

        // Sort and find center
        claims.sort(Comparator.<int[]>comparingInt(c -> c[0]).thenComparingInt(c -> c[1]));
        int centerIndex = claims.size() / 2;
        int centerX = claims.get(centerIndex)[0];
        int centerZ = claims.get(centerIndex)[1];
        String world = lecternBlock.getWorld().getName();

        int currentRadius = (int) Math.sqrt(claims.size()) / 2;
        if (currentRadius + 1 >= MAX_TOWN_CHUNKS_RADIUS) {
            return sendErrorBook(lecternBlock, player, "Maximum town size reached. Cannot expand further.");
        }

        int nextRadius = currentRadius + 1;
        List<int[]> toClaim = new ArrayList<>();
        for (int dx = -nextRadius; dx <= nextRadius; dx++) {
            for (int dz = -nextRadius; dz <= nextRadius; dz++) {
                int cx = centerX + dx;
                int cz = centerZ + dz;
                if (!townClaimDAO.isChunkClaimed(world, cx, cz)) {
                    toClaim.add(new int[]{cx, cz});
                }
            }
        }

        int emeraldCost = toClaim.size() * EMERALD_COST_PER_CHUNK;

        Inventory inv = coffer.getInventory();
        int emeraldsAvailable = countEmeralds(inv);

        if (emeraldsAvailable < emeraldCost) {
            return sendErrorBook(lecternBlock, player, "Not enough emeralds. Need " + emeraldCost + " emeralds, but found only " + emeraldsAvailable + ".");
        }

        deductEmeralds(inv, emeraldCost);

        // Claim new chunks
        for (int[] coords : toClaim) {
            townClaimDAO.claimChunk(townUuid, world, coords[0], coords[1]);
        }
        Bukkit.getPluginManager().callEvent(new TownExpandedEvent(townUuid, playerUuid, toClaim.size(), lecternBlock.getLocation()));

        int newWidth = 2 * nextRadius + 1;
        return sendSuccessBook(lecternBlock, player,
                "Decree of Expansion",
                "§lBorders Expanded§r" +
                        "\n" +
                        townName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "The land border of §o" + townName + "§r now measures " + newWidth + " by " + newWidth + " chunks." +
                        "\n\n" +
                        emeraldCost + " emeralds were laid as tribute.",
                townName);

    }

}
