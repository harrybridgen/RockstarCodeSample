package com.blothera.listener.crown;

import com.blothera.item.CrownItem;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Protects crown items from destruction or pickup, except by void.
 */
public class CrownProtectionListener implements Listener {

    /**
     * Cancels damage to crown items except when caused by the void.
     */
    @EventHandler
    public void onItemDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Item item)) return;

        if (CrownItem.isCrown(item.getItemStack()) &&
                event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents crown items from being destroyed by entity-triggered explosions.
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.getEntity().getNearbyEntities(3, 3, 3).forEach(entity -> {
            if (entity instanceof Item item && CrownItem.isCrown(item.getItemStack())) {
                item.setInvulnerable(true); // Prevent removal
            }
        });
    }

    /**
     * Prevents crown items from being destroyed by block-triggered explosions (beds, crystals).
     */
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.getBlock().getWorld().getNearbyEntities(event.getBlock().getLocation(), 3, 3, 3).forEach(entity -> {
            if (entity instanceof Item item && CrownItem.isCrown(item.getItemStack())) {
                item.setInvulnerable(true);
            }
        });
    }

    /**
     * Prevents crown items from being set on fire.
     */
    @EventHandler
    public void onItemCombust(EntityCombustEvent event) {
        if (event.getEntity() instanceof Item item && CrownItem.isCrown(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    /**
     * Stops crown items from despawning naturally.
     */
    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        if (CrownItem.isCrown(event.getEntity().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents non-player entities from picking up crown items.
     */
    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (CrownItem.isCrown(event.getItem().getItemStack()) &&
                !(event.getEntity() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents hoppers and similar containers from picking up crown items.
     */
    @EventHandler
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (CrownItem.isCrown(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents crown items from merging with other golden helmets (preserves metadata).
     */
    @EventHandler
    public void onItemMerge(ItemMergeEvent event) {
        if (CrownItem.isCrown(event.getEntity().getItemStack()) ||
                CrownItem.isCrown(event.getTarget().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGrindstoneUse(PrepareGrindstoneEvent event) {
        GrindstoneInventory inv = event.getInventory();
        ItemStack first = inv.getItem(0);
        ItemStack second = inv.getItem(1);

        if (CrownItem.isCrown(first) || CrownItem.isCrown(second)) {
            event.setResult(null); // cancels output result
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilUse(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack first = inv.getItem(0);
        ItemStack second = inv.getItem(1);

        if (CrownItem.isCrown(first) || (CrownItem.isCrown(second))) {
            event.setResult(null);
        }
    }
}
