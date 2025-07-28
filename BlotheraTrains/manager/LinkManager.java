package com.blothera.manager;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import com.blothera.TrainPlugin;

import java.util.*;

import static com.blothera.constant.TrainConstants.*;

public class LinkManager {
    private final Map<UUID, Double> linkDistances = new HashMap<>();
    private final NamespacedKey trainKey = getTrainLinkKey(TrainPlugin.getInstance());
    private final NamespacedKey rootKey = getTrainRootKey(TrainPlugin.getInstance());
    private final Set<UUID> rootCarts = new HashSet<>();
    private final Map<UUID, UUID> childToParent = new HashMap<>();
    private final Map<UUID, List<UUID>> parentToChildren = new HashMap<>();
    private final ChunkManager chunkManager;

    public LinkManager(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    public void link(Minecart child, UUID parentId, double distance) {
        UUID childId = child.getUniqueId();

        // Store data
        child.getPersistentDataContainer().set(trainKey, PersistentDataType.STRING, parentId.toString());
        linkDistances.put(childId, distance);
        childToParent.put(childId, parentId);

        // Update parentToChildren
        parentToChildren.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);

        // Set root cart flag
        Minecart parent = (Minecart) Bukkit.getEntity(parentId);
        if (parent != null && parent.getType() == EntityType.FURNACE_MINECART) {
            parent.getPersistentDataContainer().set(rootKey, PersistentDataType.INTEGER, 1);
            rootCarts.add(parentId);
        }
    }


    public boolean isRootCart(Minecart cart) {
        return cart.getPersistentDataContainer().has(rootKey, PersistentDataType.INTEGER);
    }

    public Minecart getRootCar(Minecart cart) {
        Minecart current = cart;
        while (isLinked(current)) {
            UUID parentId = getLinkedParentId(current);
            if (parentId == null) break;
            Minecart parent = (Minecart) Bukkit.getEntity(parentId);
            if (parent == null) break;
            current = parent;
        }
        return current;
    }

    public boolean isParentCart(Minecart cart) {
        UUID cartId = cart.getUniqueId();
        List<UUID> children = parentToChildren.get(cartId);
        return children != null && !children.isEmpty();
    }


    public int countLinkedCartsFrom(Minecart root) {
        int count = 0;
        UUID currentId = root.getUniqueId();

        while (true) {
            List<UUID> children = parentToChildren.get(currentId);
            if (children == null || children.isEmpty()) break;

            // Assumes linear link for this method
            currentId = children.getFirst();
            count++;
        }

        return count;
    }

    public void rebuildFromWorld(World world) {
        for (Minecart cart : world.getEntitiesByClass(Minecart.class)) {
            if (isLinked(cart)) {
                UUID childId = cart.getUniqueId();
                UUID parentId = getLinkedParentId(cart);
                childToParent.put(childId, parentId);
                parentToChildren.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);
            }
            if (isRootCart(cart)) {
                rootCarts.add(cart.getUniqueId());
            }
        }
    }

    public void clear() {
        linkDistances.clear();
        rootCarts.clear();
        childToParent.clear();
        parentToChildren.clear();
    }


    public List<Minecart> getAllRootCarts(World world) {
        List<Minecart> result = new ArrayList<>();
        for (UUID id : rootCarts) {
            Minecart cart = (Minecart) Bukkit.getEntity(id);
            if (cart != null && cart.getWorld().equals(world) && cart.isValid()) {
                result.add(cart);
            }
        }
        return result;
    }

    public void unlink(Minecart cart) {
        UUID cartId = cart.getUniqueId();
        UUID parentId = childToParent.remove(cartId);
        linkDistances.remove(cartId);
        cart.getPersistentDataContainer().remove(trainKey);

        if (parentId != null) {
            List<UUID> children = parentToChildren.get(parentId);
            if (children != null) {
                children.remove(cartId);
                if (children.isEmpty()) {
                    parentToChildren.remove(parentId);
                }
            }
        }

        if (isRootCart(cart)) {
            cart.getPersistentDataContainer().remove(rootKey);
            rootCarts.remove(cartId);
        }
    }


    public UUID getLinkedParentId(Minecart cart) {
        String id = cart.getPersistentDataContainer().get(trainKey, PersistentDataType.STRING);
        return id != null ? UUID.fromString(id) : null;
    }

    public boolean isLinked(Minecart cart) {
        return cart.getPersistentDataContainer().has(trainKey, PersistentDataType.STRING);
    }

    public Set<Minecart> getEntireTrain(Minecart start) {
        Set<UUID> visited = new HashSet<>();
        Queue<UUID> toVisit = new LinkedList<>();
        Set<Minecart> train = new HashSet<>();

        toVisit.add(start.getUniqueId());

        while (!toVisit.isEmpty()) {
            UUID id = toVisit.poll();
            if (!visited.add(id)) continue;

            Minecart cart = (Minecart) Bukkit.getEntity(id);
            if (cart != null && cart.isValid()) {
                train.add(cart);

                UUID parentId = childToParent.get(id);
                if (parentId != null) toVisit.add(parentId);

                List<UUID> children = parentToChildren.getOrDefault(id, Collections.emptyList());
                toVisit.addAll(children);
            }
        }

        return train;
    }


    public void unlinkEntireTrain(Minecart startCar, VisualLinkManager visualLinkManager) {
        Set<Minecart> trainCarts = getEntireTrain(startCar);

        // Clean up forced chunks for this train
        chunkManager.unforceAllChunksForTrain(startCar);

        for (Minecart cart : trainCarts) {
            unlink(cart);
            cart.setVelocity(cart.getVelocity().zero());
            visualLinkManager.unlinkVisual(cart);

            // Clear selection if this cart is part of a pending link
            for (Map.Entry<UUID, Map.Entry<org.bukkit.entity.Bat, Minecart>> entry : visualLinkManager.getTempBats().entrySet()) {
                if (entry.getValue().getValue().equals(cart)) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        visualLinkManager.cancelPendingLink(player);
                    }
                }
            }

            if (isRootCart(cart)) {
                cart.getPersistentDataContainer().remove(rootKey);
            }
        }
    }
}
