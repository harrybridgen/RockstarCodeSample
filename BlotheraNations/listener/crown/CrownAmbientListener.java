package com.blothera.listener.crown;

import com.blothera.item.CrownItem;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles ambient effects for crown items when they are dropped in the world.
 * Crowns will occasionally play a humming sound and are made invulnerable.
 */
public class CrownAmbientListener implements Listener {

    private final Plugin plugin;
    private final Map<UUID, BukkitRunnable> tasks = new ConcurrentHashMap<>();

    // === Constants ===

    /**
     * Number of ticks per second in Minecraft (used for timing conversions).
     */
    private static final long ONE_SECOND_TICKS = 20L;

    /**
     * Delay before the first hum burst starts (in ticks).
     * This controls the initial silent period after a crown is dropped.
     */
    private static final long INITIAL_DELAY = ONE_SECOND_TICKS * 10; // SECONDS

    /**
     * Minimum number of hum sounds in one burst.
     * Controls the least amount of hums played per cycle.
     */
    private static final int HUM_MIN = 4;

    /**
     * Maximum number of hum sounds in one burst.
     * Controls the most hums played in a single ambient cycle.
     */
    private static final int HUM_MAX = 7;

    /**
     * Base delay (in ticks) between each hum in a burst.
     * Determines base interval between individual hums.
     */
    private static final int HUM_BASE_DELAY_TICKS = 20;

    /**
     * Added random delay variance (in ticks) to stagger hum timing.
     * Prevents the hums from sounding perfectly rhythmic or robotic.
     */
    private static final int HUM_VARIANCE_TICKS = 20;

    /**
     * Minimum delay between hum bursts (in seconds).
     * Sets the shortest possible cooldown between each ambient cycle.
     */
    private static final long NEXT_BURST_MIN_DELAY = 30; // SECONDS

    /**
     * Maximum delay between hum bursts (in seconds).
     * Sets the longest possible cooldown between ambient sound cycles.
     */
    private static final long NEXT_BURST_MAX_DELAY = 60; // SECONDS

    /**
     * Volume of the ambient hum sound.
     * 1.0 is normal; 2.0 is twice as loud.
     */
    private static final float HUM_VOLUME = 2.0f;

    /**
     * Minimum pitch of the hum sound.
     * Lower pitch = deeper sound.
     */
    private static final float HUM_PITCH_MIN = 0.5f;

    /**
     * Random pitch variation added to the base pitch.
     * Helps diversify the tone of each hum.
     */
    private static final float HUM_PITCH_VARIANCE = 0.2f;

    /**
     * Sound used for the crown's ambient hum.
     * Gives the mystical audio effect around the dropped crown.
     */
    private static final Sound HUM_SOUND = Sound.BLOCK_SCULK_CATALYST_BLOOM;


    public CrownAmbientListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Item item && CrownItem.isCrown(item.getItemStack())) {
                cancelTask(item.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Item item && CrownItem.isCrown(item.getItemStack())) {
                UUID uuid = item.getUniqueId();
                if (!tasks.containsKey(uuid)) {
                    startAmbientTask(item);
                }
            }
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (!CrownItem.isCrown(item.getItemStack())) return;
        item.setInvulnerable(true);
        item.setUnlimitedLifetime(true);
        item.setCanMobPickup(false);
        item.setGlowing(true);
        startAmbientTask(item);
    }

    private void startAmbientTask(Item item) {
        UUID uuid = item.getUniqueId();

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!item.isValid() || item.isDead()) {
                    cancelTask(uuid);
                    return;
                }

                int humCount = HUM_MIN + (int) (Math.random() * (HUM_MAX - HUM_MIN + 1));

                for (int i = 0; i < humCount; i++) {
                    int delay = i * (HUM_BASE_DELAY_TICKS + (int) (Math.random() * HUM_VARIANCE_TICKS));

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!item.isValid() || item.isDead()) return;

                            item.getWorld().playSound(
                                    item.getLocation(),
                                    HUM_SOUND,
                                    HUM_VOLUME,
                                    HUM_PITCH_MIN + (float) (Math.random() * HUM_PITCH_VARIANCE)
                            );
                        }
                    }.runTaskLater(plugin, delay);
                }

                long nextDelay = ONE_SECOND_TICKS * (NEXT_BURST_MIN_DELAY + (int) (Math.random() * (NEXT_BURST_MAX_DELAY - NEXT_BURST_MIN_DELAY + 1)));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        tasks.remove(uuid);
                        startAmbientTask(item);
                    }
                }.runTaskLater(plugin, nextDelay);
            }
        };

        task.runTaskLater(plugin, INITIAL_DELAY);
        tasks.put(uuid, task);
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        Item item = event.getItem();
        if (!CrownItem.isCrown(item.getItemStack())) return;

        cancelTask(item.getUniqueId());
    }

    private void cancelTask(UUID uuid) {
        BukkitRunnable task = tasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }
}
