package com.blothera.task;

import com.blothera.TrainPlugin;
import com.blothera.manager.ChunkManager;
import com.blothera.manager.LinkManager;
import com.blothera.manager.VisualLinkManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

import static com.blothera.constant.TrainConstants.*;

public class TrainUpdateTask {
    LinkManager linkManager;
    ChunkManager chunkManager;

    public TrainUpdateTask(LinkManager linkManager, VisualLinkManager visualLinkManager, ChunkManager chunkManager) {
        this.linkManager = linkManager;
        this.chunkManager = chunkManager;

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getWorlds().forEach(world -> {

                    // Train physics
                    for (Minecart root : linkManager.getAllRootCarts(world)) {
                        if (root.getType() == EntityType.FURNACE_MINECART) {
                            chunkManager.updateChunksForCart(root);
                        }
                        for (Minecart cart : linkManager.getEntireTrain(root)) {
                            if (!linkManager.isLinked(cart)) continue;

                            UUID parentId = linkManager.getLinkedParentId(cart);
                            Minecart parent = (Minecart) Bukkit.getEntity(parentId);

                            if (parent == null || !parent.isValid()) {
                                linkManager.unlinkEntireTrain(cart, visualLinkManager);
                                continue;
                            }

                            double distanceBetween = cart.getLocation().distance(parent.getLocation());

                            if (distanceBetween > MAX_CART_DISTANCE) {
                                linkManager.unlinkEntireTrain(cart, visualLinkManager);
                                continue;
                            }

                            Vector parentVelocity = parent.getVelocity();

                            if (parentVelocity.lengthSquared() > SMALL_VELOCITY) {

                                if (parent.equals(root) && parent.getType() == EntityType.FURNACE_MINECART) {
                                    Vector parentVel = parent.getVelocity();
                                    Vector toChild = cart.getLocation().toVector().subtract(parent.getLocation().toVector());

                                    // Normalize
                                    Vector parentDir = parentVel.clone().normalize();
                                    Vector toChildDir = toChild.clone().normalize();

                                    double dot = parentDir.dot(toChildDir);

                                    // Check if aligned closely along axis (child is in front or behind)
                                    boolean alignedAxis = isRoughlyAlignedAxis(parent.getLocation(), cart.getLocation());

                                    // Furnace is moving *into* child AND they're aligned
                                    if (dot > REVERSE_DOT_PRODUCT && alignedAxis) {
                                        linkManager.unlinkEntireTrain(cart, visualLinkManager);
                                        continue;
                                    }
                                }

                                if (distanceBetween < MIN_CART_DISTANCE) {
                                    Vector pushBack = cart.getLocation().toVector().subtract(parent.getLocation().toVector()).normalize().multiply(PUSHBACK_TRAIN_VECTOR_MULTI);
                                    cart.setVelocity(pushBack);
                                    continue;
                                }

                                Vector direction = parentVelocity.clone().normalize().multiply(REVERSE_TRAIN_VECTOR_MULTI);
                                Vector targetPos = parent.getLocation().toVector().add(direction.multiply(DESIRED_CART_DISTANCE));
                                Vector currentPos = cart.getLocation().toVector();

                                Vector correction = targetPos.subtract(currentPos);

                                Vector springForce = correction.multiply(STIFFNESS);
                                Vector dampingForce = cart.getVelocity().multiply(-DAMPING);
                                Vector combined = springForce.add(dampingForce);

                                if (Double.isFinite(combined.getX()) && Double.isFinite(combined.getY()) && Double.isFinite(combined.getZ())) {
                                    cart.setVelocity(combined);
                                } else {
                                    Bukkit.getLogger().warning("[BlotheraTrain] Skipped applying invalid velocity to cart " + cart.getUniqueId());
                                }
                            } else {
                                cart.setVelocity(cart.getVelocity().multiply(SLOWDOWN_TRAIN_VECTOR_MULTI));
                            }
                        }
                    }
                });
            }
        }.runTaskTimer(TrainPlugin.getInstance(), TRAIN_TASK_DELAY_TICKS, TRAIN_TASK_PERIOD_TICKS);
    }

    private boolean isRoughlyAlignedAxis(Location a, Location b) {
        double dx = Math.abs(a.getX() - b.getX());
        double dz = Math.abs(a.getZ() - b.getZ());

        // Only unlink if they're aligned within ~0.6 blocks along one axis
        return (dx < AXIS_ALIGNMENT_THRESHOLD && dz > PERPENDICULAR_SEPARATION_MINIMUM)
                || (dz < AXIS_ALIGNMENT_THRESHOLD && dx > PERPENDICULAR_SEPARATION_MINIMUM);
    }
}


