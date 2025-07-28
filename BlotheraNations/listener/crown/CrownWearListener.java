package com.blothera.listener.crown;

import com.blothera.item.CrownItem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import static com.blothera.util.NationConstants.*;

public class CrownWearListener implements Listener {

    /**
     * Listens for changes in a player's equipment, specifically when they wear a crown.
     * If the player wears a crown, it discovers the recipes for Nation, Town, and Diplomacy lecterns.
     *
     * @param event The EntityEquipmentChangedEvent triggered when a player's equipment changes.
     */
    @EventHandler
    public void onArmorChange(EntityEquipmentChangedEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;

        if (!event.getEquipmentChanges().containsKey(EquipmentSlot.HEAD)) return;

        ItemStack newHelmet = event.getEquipmentChanges().get(EquipmentSlot.HEAD).newItem();
        if (CrownItem.isCrown(newHelmet)) {
            if (!player.hasDiscoveredRecipe(NATION_LECTERN_RECIPE_KEY)) {
                player.discoverRecipe(NATION_LECTERN_RECIPE_KEY);
            }
            if (!player.hasDiscoveredRecipe(TOWN_LECTERN_RECIPE_KEY)) {
                player.discoverRecipe(TOWN_LECTERN_RECIPE_KEY);
            }
            if (!player.hasDiscoveredRecipe(DIPLOMACY_LECTERN_RECIPE_KEY)) {
                player.discoverRecipe(DIPLOMACY_LECTERN_RECIPE_KEY);
            }
        }
    }
}




