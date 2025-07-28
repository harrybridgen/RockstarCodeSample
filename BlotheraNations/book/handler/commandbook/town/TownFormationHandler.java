package com.blothera.book.handler.commandbook.town;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationLecternDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import com.blothera.database.TownDAOs.TownLecternDAO;
import com.blothera.database.TownDAOs.TownMemberDAO;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;
import java.util.UUID;

import static com.blothera.util.NationConstants.*;

import com.blothera.event.town.TownFormationEvent;

public class TownFormationHandler extends CommandBookHandler {

    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final NationMemberDAO nationMemberDAO;
    private final TownMemberDAO townMemberDAO;
    private final TownLecternDAO townLecternDAO;
    private final NationLecternDAO nationLecternDAO;
    private final NationDAO nationDao;
    private final DiplomacyLecternDAO diplomacyLecternDAO;

    public TownFormationHandler(NationPlugin plugin) {
        super(plugin);
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.townMemberDAO = plugin.getDatabase().getTownMemberDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
        this.nationLecternDAO = plugin.getDatabase().getNationLecternDAO();
        this.nationDao = plugin.getDatabase().getNationDAO();
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(FORM_TOWN_COMMAND, FOUND_TOWN_COMMAND, MAKE_TOWN_COMMAND, CREATE_TOWN_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "This book is empty.\n" +
                    "Speak the name of the Town you wish to found.");
        }

        String townName = meta.getPage(1).split("\n")[0].trim();
        if (townName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Speak the town's name, for silence is forgotten.");
        }
        if (townName.length() < 3) {
            return sendErrorBook(lecternBlock, player, townName + " is too short.");
        }
        if (townName.length() > 24) {
            return sendErrorBook(lecternBlock, player, townName + " is too long.");
        }
        if (!isValidName(townName) && !player.isOp()) {
            return sendErrorBook(lecternBlock, player, "The name '" + townName + "' is rejected by the land.");
        }
        if (townDAO.townExists(townName)) {
            return sendErrorBook(lecternBlock, player, "This name is already bound to land.");
        }

        String playerUuid = player.getUniqueId().toString();
        String nationUuid = nationMemberDAO.getNationUuid(playerUuid);
        if (nationUuid == null) {
            return sendErrorBook(lecternBlock, player, "Only members of a Nation may found a Town.");
        }

        if (townDAO.isTownLeader(playerUuid)) {
            return sendErrorBook(lecternBlock, player, "You already lead a Town. You must relinquish it before founding another.");
        }

        if (getAdjacentChest(lecternBlock) == null) {
            return sendErrorBook(lecternBlock, player, "The ritual requires a chest beside the Lectern.");
        }

        if (!townDAO.nationHasAnyTowns(nationUuid) && !hasNearbyNationLectern(lecternBlock)) {
            return sendErrorBook(lecternBlock, player, "The Capital Town must rise close to a Nation Lectern.");
        }

        if (!townDAO.nationHasAnyTowns(nationUuid) && !hasNearByDiplomacyLectern(lecternBlock)) {
            return sendErrorBook(lecternBlock, player, "The Capital Town must rise close to a Diplomacy Lectern.");
        }

        Location newTownLoc = lecternBlock.getLocation();
        String worldString = newTownLoc.getWorld().getName();

        // 1. Too close to spawn
        Location spawnLoc = newTownLoc.getWorld().getSpawnLocation();

        if (newTownLoc.distance(spawnLoc) < MIN_BLOCKS_TO_SPAWN && !player.isOp()) {
            return sendErrorBook(lecternBlock, player, "You must found your town farther than " + MIN_BLOCKS_TO_SPAWN + " blocks from spawn.");
        }

        // 2. Too close to any town (regardless of nation, including for the capital)
        for (String otherTownUuid : townDAO.getAllTownUuids()) {
            // Skip if town is from the same nation
            String otherTownNation = nationDao.getNationUuidByTownUuid(otherTownUuid);
            if (nationUuid.equals(otherTownNation)) continue;

            List<int[]> claims = townClaimDAO.getClaims(otherTownUuid);
            if (claims.isEmpty()) continue;

            Location center = getTownCenter(worldString, claims);
            double dist = center.distance(newTownLoc);
            if (center.getWorld().equals(newTownLoc.getWorld()) && dist < MIN_BLOCKS_FROM_FOREIGN_TOWN) {
                String name = townDAO.getTownName(otherTownUuid);
                return sendErrorBook(lecternBlock, player,
                        "Too close to another town's heart.\n\n" +
                                "§o" + name + "§r is too close.\n" +
                                "You must be at least " + MIN_BLOCKS_FROM_FOREIGN_TOWN + " blocks from a foreign town.");
            }
        }

        // 3. Distance checks relative to your own nation’s towns (only applies if nation already has towns)
        if (townDAO.nationHasAnyTowns(nationUuid)) {
            List<String> nationTowns = townDAO.getTownsByNationUuid(nationUuid);
            boolean tooClose = false, tooFar = true;

            int minDistSq = MIN_BLOCKS_FROM_OWN_TOWN * MIN_BLOCKS_FROM_OWN_TOWN;
            int maxDistSq = MAX_BLOCKS_FROM_OWN_TOWN * MAX_BLOCKS_FROM_OWN_TOWN;

            for (String townUuid : nationTowns) {
                List<int[]> claims = townClaimDAO.getClaims(townUuid);
                if (claims.isEmpty()) continue;

                Location center = getTownCenter(worldString, claims);
                double distSquared = center.distanceSquared(newTownLoc);
                if (distSquared < minDistSq) tooClose = true;
                if (distSquared <= maxDistSq) tooFar = false;

            }
            if (townDAO.nationHasAnyTowns(nationUuid)) {
                String capitalTownUuid = nationDao.getCapitalTownUuid(nationUuid);
                List<int[]> capitalClaims = townClaimDAO.getClaims(capitalTownUuid);
                if (capitalClaims.isEmpty()) {
                    return sendErrorBook(lecternBlock, player, "Your nation's capital has no claims. Contact an admin.");
                }

                Location capitalCenter = getTownCenter(worldString, capitalClaims);
                double dist = capitalCenter.distance(newTownLoc);

                int capitalRadius = townDAO.getCapitalRadius(nationUuid);

                if (dist > capitalRadius) {
                    return sendErrorBook(lecternBlock, player,
                            "Too far from your nation's capital.\n\n" +
                                    "§o" + townDAO.getTownName(capitalTownUuid) + "§r is too far away away.\n" +
                                    "The current administration range from your capital is " + capitalRadius + " blocks." +
                                    "\n" +
                                    "This distance increases by " + ADDITIONAL_BLOCK_RADIUS_PER_TOWN_BLOCKS + " with every new town.");
                }
            }

            if (tooClose || tooFar) {
                for (String townUuid : nationTowns) {
                    List<int[]> claims = townClaimDAO.getClaims(townUuid);
                    if (claims.isEmpty()) continue;

                    Location center = getTownCenter(worldString, claims);
                    double dist = center.distance(newTownLoc);
                    String name = townDAO.getTownName(townUuid);

                    if (tooClose && dist < MIN_BLOCKS_FROM_OWN_TOWN) {
                        return sendErrorBook(lecternBlock, player,
                                "Too close to your nation's existing town.\n\n" +
                                        "§o" + name + "§r is too close.\n" +
                                        "Minimum spacing is " + MIN_BLOCKS_FROM_OWN_TOWN + " blocks in between towns.");
                    }

                    if (tooFar && dist <= MAX_BLOCKS_FROM_OWN_TOWN) {
                        tooFar = false; // another town is actually within range
                        break;
                    }
                }

                if (tooFar) {
                    return sendErrorBook(lecternBlock, player,
                            "Too far from your nation's existing towns.\n" +
                                    "Towns must be within " + MAX_BLOCKS_FROM_OWN_TOWN + " blocks of an existing town.");
                }
            }

        }

        townName = townDAO.getCanonicalTownName(townName);
        if (townName == null) {
            return sendErrorBook(lecternBlock, player, "Invalid town name. Contact an admin for assistance.");
        }

        boolean isCapital = !townDAO.nationHasAnyTowns(nationUuid);
        if (!isCapital) {
            Block chestBlock = getAdjacentChest(lecternBlock);
            if (!(chestBlock != null && chestBlock.getState() instanceof Chest coffer)) {
                return sendErrorBook(lecternBlock, player, "A chest with emeralds must be beside the Lectern.");
            }

            int townCount = townDAO.getTownsByNationUuid(nationUuid).size();
            int foundingCost = 9 + (townCount - 1) * 3;

            Inventory inv = coffer.getInventory();
            int emeralds = countEmeralds(inv);

            if (emeralds < foundingCost) {
                return sendErrorBook(lecternBlock, player,
                        "Insufficient emeralds to found a new town.\n\n" +
                                "§oRequired:§r " + foundingCost + " emeralds\n" +
                                "§oAvailable:§r " + emeralds + "\n\n" +
                                "The cost grows with each new town in your nation.");
            }

            deductEmeralds(inv, foundingCost);
        }

        String townUuid = UUID.randomUUID().toString();

        townDAO.createTown(townUuid, nationUuid, townName, isCapital, playerUuid);
        townMemberDAO.addMember(playerUuid, townUuid);
        initTownChunks(lecternBlock, townUuid);


        Bukkit.getPluginManager().callEvent(new TownFormationEvent(townUuid, townName, nationUuid, playerUuid, isCapital, lecternBlock.getLocation()));

        return sendSuccessBook(lecternBlock, player,
                "Proclamation of Town",
                "§lNew Town§r" +
                        "\n" +
                        townName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "§o" + player.getName() + "§r founded the town of §o" + townName + "§r." +
                        "\n\n" +
                        "From these humble borders shall rise stone, story, and sovereignty.",
                townName);
    }

    private void initTownChunks(Block lecternBlock, String townUuid) {
        if (townUuid == null) return;
        String world = lecternBlock.getWorld().getName();
        int baseX = lecternBlock.getChunk().getX();
        int baseZ = lecternBlock.getChunk().getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int chunkX = baseX + dx;
                int chunkZ = baseZ + dz;
                townClaimDAO.claimChunk(townUuid, world, chunkX, chunkZ);

            }
        }
    }

    private Location getTownCenter(String world, List<int[]> claims) {
        int totalX = 0, totalZ = 0;

        for (int[] claim : claims) {
            totalX += claim[0];
            totalZ += claim[1];
        }

        int avgX = totalX / claims.size();
        int avgZ = totalZ / claims.size();

        return new Location(Bukkit.getWorld(world), avgX * 16 + 8, 64, avgZ * 16 + 8);
    }

    private boolean hasNearbyNationLectern(Block lecternBlock) {
        int baseX = lecternBlock.getChunk().getX();
        int baseZ = lecternBlock.getChunk().getZ();
        String world = lecternBlock.getWorld().getName();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (nationLecternDAO.hasLecternInChunk(world, baseX + dx, baseZ + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasNearByDiplomacyLectern(Block lecternBlock) {
        int baseX = lecternBlock.getChunk().getX();
        int baseZ = lecternBlock.getChunk().getZ();
        String world = lecternBlock.getWorld().getName();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (diplomacyLecternDAO.hasLecternInChunk(world, baseX + dx, baseZ + dz)) {
                    return true;
                }
            }
        }
        return false;
    }
}
