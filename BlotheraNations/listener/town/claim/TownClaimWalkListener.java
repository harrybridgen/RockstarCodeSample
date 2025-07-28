package com.blothera.listener.town.claim;

import com.blothera.NationPlugin;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import static com.blothera.util.NationConstants.*;

public class TownClaimWalkListener implements Listener {

    private final HashMap<UUID, String> lastKnownTown = new HashMap<>();
    private final HashMap<UUID, Long> lastMessageTime = new HashMap<>();

    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;

    public TownClaimWalkListener(NationPlugin plugin) {
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
    }

    /**
     * Listens for player movement events to check if the player has entered a new town or wilderness area.
     * If the player moves into a new chunk, it checks the town claim database
     * to determine the town or wilderness status of that chunk.
     * If the player enters a new town or wilderness area,
     * it sends an action bar message to the player with the town or wilderness name.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (event.getFrom().getChunk().equals(event.getTo().getChunk()) && lastKnownTown.containsKey(playerId)) return;

        long now = System.currentTimeMillis();

        if (now - lastMessageTime.getOrDefault(playerId, 0L) < TERRITORY_MESSAGE_COOLDOWN_MS) return;

        Chunk toChunk = event.getTo().getChunk();
        String world = toChunk.getWorld().getName();
        int chunkX = toChunk.getX();
        int chunkZ = toChunk.getZ();

        String newTownId = townClaimDAO.getTownIdAt(world, chunkX, chunkZ);

        if (newTownId == null || Objects.equals(townDAO.getTownName(newTownId), UNKNOWN_TOWN)) {
            newTownId = WILDERNESS_STRING;
        }

        String lastTownId = lastKnownTown.get(playerId);

        if (lastTownId == null || !lastTownId.equals(newTownId)) {
            lastKnownTown.put(playerId, newTownId);
            lastMessageTime.put(playerId, now);

            String message;
            if (newTownId.equals(WILDERNESS_STRING)) {
                message = ChatColor.GRAY + WILDERNESS_STRING;
            } else {

                String townName = townDAO.getTownName(newTownId);
                String nationName = townDAO.getNationNameForTown(newTownId);

                // EDGE CASE: This happens when a town or nation is disbanded or deleted
                if ((townName.equals(UNKNOWN_TOWN)) || (nationName.equals(UNKNOWN_NATION))) {
                    message = WILDERNESS_STRING;
                } else {
                    boolean isDormant = townDAO.isDormant(newTownId);
                    boolean isCapital = townDAO.isCapital(newTownId);
                    if (isDormant) {
                        message = ChatColor.GRAY + townName + ChatColor.RED + " (Dormant)";
                    } else if (isCapital) {
                        message = ChatColor.GOLD + townName + ChatColor.GRAY + " - " + ChatColor.BLUE + nationName;
                    } else {
                        message = ChatColor.GREEN + townName + ChatColor.GRAY + " - " + ChatColor.BLUE + nationName;
                    }
                }
            }

            player.sendActionBar(net.kyori.adventure.text.Component.text(message));
        }
    }

    /**
     * Cleans up the last known town and message time for a player when they quit.
     * This prevents memory leaks and ensures that the data is not retained unnecessarily.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastKnownTown.remove(playerId);
        lastMessageTime.remove(playerId);
    }
}
