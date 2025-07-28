package com.blothera.manager;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

import java.util.*;

import com.blothera.TrainPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import static com.blothera.constant.TrainConstants.*;

public class VisualLinkManager {
    private final Map<UUID, Bat> linkBats = new HashMap<>();
    private final Map<UUID, Map.Entry<Bat, Minecart>> tempBats = new HashMap<>();
    private final Map<UUID, BukkitTask> tempLinkTimeouts = new HashMap<>();
    private Consumer<Player> cancelCallback;

    public void setCancelCallback(Consumer<Player> cancelCallback) {
        this.cancelCallback = cancelCallback;
    }

    public void createVisualLink(Minecart childCart, Minecart parentCart) {
        if (linkBats.containsKey(childCart.getUniqueId())) return;

        Bat bat = createVisualLinkBat(childCart);
        bat.setLeashHolder(parentCart);

        setScale(bat);
        NamespacedKey key = getLinkTypeKey(TrainPlugin.getInstance());
        bat.getPersistentDataContainer().set(key, PersistentDataType.STRING, LINK_TYPE_LINKED);
        linkBats.put(childCart.getUniqueId(), bat);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!childCart.isValid() || !parentCart.isValid() || !bat.isValid()) {
                    unlinkVisual(childCart);
                    cancel();
                    return;
                }

                // Move the bat to the childCar
                bat.teleport(getAdjustedBatLocation(childCart));
            }
        }.runTaskTimer(TrainPlugin.getInstance(), TRAIN_TASK_DELAY_TICKS, TRAIN_TASK_PERIOD_TICKS);
    }

    public boolean isCurrentlySelecting(Player player) {
        UUID playerId = player.getUniqueId();
        return tempBats.containsKey(playerId);
    }

    public Map.Entry<Bat, Minecart> getTempBat(Player player) {
        UUID playerId = player.getUniqueId();
        return tempBats.get(playerId);
    }

    public void spawnPendingLinkBat(Minecart cart, Player player) {
        Bat bat = createVisualLinkBat(cart);
        bat.setLeashHolder(player);

        setScale(bat);
        bat.getPersistentDataContainer().set(getLinkTypeKey(TrainPlugin.getInstance()), PersistentDataType.STRING, LINK_TYPE_TEMP);

        tempBats.put(player.getUniqueId(), Map.entry(bat, cart));

        // Auto-remove if they never finish
        BukkitTask task = Bukkit.getScheduler().runTaskLater(TrainPlugin.getInstance(), () -> {
            cancelPendingLink(player);
            tempLinkTimeouts.remove(player.getUniqueId());
        }, LINK_TIMEOUT_TICKS);

        tempLinkTimeouts.put(player.getUniqueId(), task);
    }

    private static void setScale(Bat bat) {
        AttributeInstance scaleAttr = bat.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(BAT_SCALE);
        }
    }

    private static @NotNull Bat createVisualLinkBat(Minecart cart) {
        Bat bat = (Bat) cart.getWorld().spawnEntity(cart.getLocation().add(LINK_BAT_X_OFFSET, LINK_BAT_Y_OFFSET, LINK_BAT_Z_OFFSET), EntityType.BAT);
        bat.setInvisible(true);
        bat.setSilent(true);
        bat.setAwake(false);
        bat.setCollidable(false);
        bat.setAI(false);
        bat.setRemoveWhenFarAway(false);
        bat.setGravity(false);
        bat.setAware(false);
        bat.setInvulnerable(true);
        bat.setCustomNameVisible(false);
        bat.setPersistent(true);
        bat.customName(Component.text(TEMP_BAT_NAME));
        return bat;
    }

    public void finalizePendingLink(Player player, Minecart childCart) {
        Map.Entry<Bat, Minecart> entry = tempBats.remove(player.getUniqueId());
        if (entry == null) return;

        // Cancel and remove the pending timeout
        BukkitTask timeout = tempLinkTimeouts.remove(player.getUniqueId());
        if (timeout != null) {
            timeout.cancel();
        }

        Bat bat = entry.getKey();
        Minecart anchorCart = entry.getValue(); // the furnace cart

        if (bat != null && bat.isValid()) {
            bat.setLeashHolder(childCart); // the lead now goes to the child cart

            linkBats.put(childCart.getUniqueId(), bat);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!anchorCart.isValid() || !bat.isValid()) {
                        unlinkVisual(childCart);
                        cancel();
                    } else {
                        bat.teleport(anchorCart.getLocation().add(LINK_BAT_X_OFFSET, LINK_BAT_Y_OFFSET, LINK_BAT_Z_OFFSET)); // always floats at furnace cart
                    }
                }
            }.runTaskTimer(TrainPlugin.getInstance(), TRAIN_TASK_DELAY_TICKS, TRAIN_TASK_PERIOD_TICKS);
        }
    }

    public void cancelPendingLink(Player player) {
        // Remove and cancel the timeout task
        BukkitTask timeout = tempLinkTimeouts.remove(player.getUniqueId());
        if (timeout != null) {
            timeout.cancel();
        }

        Map.Entry<Bat, Minecart> entry = tempBats.remove(player.getUniqueId());
        if (entry != null && entry.getKey().isValid()) {
            cancelCallback.accept(player);
            Bat bat = entry.getKey();
            bat.setLeashHolder(null);
            entry.getKey().remove();
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_FALL, 1.0f, 2.0f);
        }
    }

    public Map<UUID, Map.Entry<Bat, Minecart>> getTempBats() {
        return tempBats;
    }

    public UUID getCartLinkedToBat(Bat bat) {
        for (Map.Entry<UUID, Bat> entry : linkBats.entrySet()) {
            if (entry.getValue().equals(bat)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void unlinkVisual(Minecart cart) {
        Bat bat = linkBats.remove(cart.getUniqueId());
        if (bat != null && bat.isValid()) {
            bat.setLeashHolder(null);
            bat.remove();
            cart.getWorld().dropItemNaturally(cart.getLocation(), new ItemStack(Material.CHAIN));
            cart.getWorld().playSound(cart.getLocation(), Sound.BLOCK_CHAIN_BREAK, 0.7f, 0.6f);
        }
    }

    public void unlinkVisualByBat(Bat bat) {
        UUID foundCart = null;

        for (Map.Entry<UUID, Bat> entry : linkBats.entrySet()) {
            if (entry.getValue().equals(bat)) {
                foundCart = entry.getKey();
                break;
            }
        }

        if (foundCart != null) {
            Minecart cart = (Minecart) Bukkit.getEntity(foundCart);
            Bat linkedBat = linkBats.remove(foundCart);

            if (linkedBat != null && linkedBat.isValid()) {
                linkedBat.setLeashHolder(null);
                linkedBat.remove();
            }

            // Return chain to cart location
            if (cart != null && cart.isValid()) {
                cart.getWorld().dropItemNaturally(cart.getLocation(), new ItemStack(Material.CHAIN));
            }
        }
    }

    private boolean batsVisible = false;

    public boolean toggleBatDebugVisibility() {
        batsVisible = !batsVisible;

        // Update link bats
        for (Bat bat : linkBats.values()) {
            if (bat != null && bat.isValid()) {
                bat.setInvisible(!batsVisible);
                bat.setCustomNameVisible(batsVisible);
            }
        }

        // Update temp bats
        for (Map.Entry<Bat, Minecart> entry : tempBats.values()) {
            Bat bat = entry.getKey();
            if (bat != null && bat.isValid()) {
                bat.setInvisible(!batsVisible);
                bat.setCustomNameVisible(batsVisible);
            }
        }

        return batsVisible;
    }

    public void unlinkAll() {
        for (Bat bat : linkBats.values()) {
            if (bat != null && bat.isValid()) {
                bat.setLeashHolder(null);
                bat.remove();
            }
        }
        linkBats.clear();
    }

    private Location getAdjustedBatLocation(Minecart cart) {
        Location loc = cart.getLocation();
        Block block = loc.getBlock();

        if (block.getBlockData() instanceof org.bukkit.block.data.Rail rail) {
            switch (rail.getShape()) {
                case ASCENDING_EAST:
                case ASCENDING_WEST:
                case ASCENDING_NORTH:
                case ASCENDING_SOUTH:
                    return loc.add(LINK_BAT_X_OFFSET, LINK_BAT_Y_OFFSET_RAISED, LINK_BAT_Z_OFFSET); // raise for sloped rails
            }
        }

        return loc.add(LINK_BAT_X_OFFSET, LINK_BAT_Y_OFFSET, LINK_BAT_Z_OFFSET); // default for flat rails
    }
}


