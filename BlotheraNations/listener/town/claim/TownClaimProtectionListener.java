package com.blothera.listener.town.claim;

import com.blothera.NationPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;

import static com.blothera.util.ParticleSpawner.spawnParticles;

public class TownClaimProtectionListener implements Listener {

    private final NationPlugin plugin;

    public TownClaimProtectionListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if the player has permission to interact with the block.
     * If the player is not a member of the town that owns the claim, they are denied permission.
     * <p>
     * If a town is dormant, it loses its protection, and players can interact with blocks in that town's claims.
     *
     * @param player The player attempting to interact with the block.
     * @param block  The block being interacted with.
     * @return true if the player has no permission, false otherwise.
     */
    private boolean noPermission(Player player, Block block) {
        Chunk chunk = block.getChunk();
        String world = block.getWorld().getName();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        var claimDAO = plugin.getDatabase().getTownClaimDAO();
        var townDAO = plugin.getDatabase().getTownDAO();
        var memberDAO = plugin.getDatabase().getTownMemberDAO();

        String townId = claimDAO.getTownIdAt(world, chunkX, chunkZ);
        if (townId == null) return false;

        // Check if town is dormant — dormant towns lose protection
        if (townDAO.isDormant(townId)) return false;

        // If the player is not a member, deny access
        return !memberDAO.isMemberOfTown(player.getUniqueId().toString(), townId);
    }

    /**
     * Handles block break events to prevent players from breaking blocks in town claims
     * if they do not have permission.
     *
     * @param event The BlockBreakEvent triggered when a player attempts to break a block.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location blockLocation = block.getLocation();
        World world = block.getWorld();


        if (noPermission(player, block)) {
            event.setCancelled(true);
            spawnParticles(blockLocation, world);
        }
    }

    /**
     * Handles block place events to prevent players from placing blocks in town claims
     * if they do not have permission.
     *
     * @param event The BlockPlaceEvent triggered when a player attempts to place a block.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        Location blockLocation = block.getLocation();
        World world = block.getWorld();

        if (noPermission(player, block)) {
            event.setCancelled(true);
            spawnParticles(blockLocation, world);
        }
    }

    /**
     * Handles player interactions with blocks to prevent unauthorized interactions
     * in town claims.
     * It still allows interactions with lecterns, as they are often used for town management and public information.
     *
     * @param event The PlayerInteractEvent triggered when a player interacts with a block.
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Location blockLocation = block.getLocation();
        World world = block.getWorld();

        if (block.getType() == Material.LECTERN) return;

        if (noPermission(player, block)) {
            event.setCancelled(true);
            spawnParticles(blockLocation, world);
        }
    }
}
