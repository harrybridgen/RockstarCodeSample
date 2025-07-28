package com.blothera.listener.nation;

import com.blothera.NationPlugin;
import com.blothera.event.nation.NationCapitalTransferEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NationCapitalTransferListener implements Listener {

    NationPlugin plugin;

    public NationCapitalTransferListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the NationCapitalChangedEvent to log the change of nation capital and broadcast a message.
     *
     * @param event The NationCapitalChangedEvent containing details about the capital change.
     */
    @EventHandler
    public void onNationCapitalTransfer(NationCapitalTransferEvent event) {
        String nationName = plugin.getDatabase().getNationDAO().getNationName(event.getNationUuid());
        String oldCapitalName = event.getOldCapitalName();
        String newCapitalName = event.getNewCapitalName();
        plugin.getNationLogger().log("Capital of nation " + nationName + " " + event.getNationUuid() + " moved from " + oldCapitalName + " to " + newCapitalName);
        plugin.getLogger().info("Capital of nation " + nationName + " " + event.getNationUuid() + " moved from " + oldCapitalName + " to " + newCapitalName);
    }
}
