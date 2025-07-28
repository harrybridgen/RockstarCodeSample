package com.blothera.book.handler.infobook.player;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.infobook.InfoBookHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.IDENTITY_BOOK_COMMAND;
import static com.blothera.util.NationConstants.ME_COMMAND;
import static com.blothera.util.NationConstants.WHOAMI_COMMAND;
import static com.blothera.util.NationConstants.WHO_AM_I_COMMAND;

public class WhoAmIHandler extends InfoBookHandler {

    public WhoAmIHandler(NationPlugin plugin) {
        super(plugin);
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(WHO_AM_I_COMMAND, WHOAMI_COMMAND, IDENTITY_BOOK_COMMAND, ME_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return true;
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        String playerName = player.getName();
        String playerUuid = player.getUniqueId().toString();

        StringBuilder book = new StringBuilder();

        book.append("§lIdentity Book§r\n")
                .append(getDate()).append("\n\n")
                .append("§lPlayer§r\n")
                .append(playerName).append("\n\n");

        // Nation Info
        String nationUuid = plugin.getDatabase().getNationMemberDAO().getNationUuid(playerUuid);
        if (nationUuid != null) {
            String nationName = plugin.getDatabase().getNationDAO().getNationName(nationUuid);
            String nationLeader = plugin.getDatabase().getNationDAO().getLeaderUuid(nationUuid);
            book.append("§lNation§r\n");
            book.append(nationName != null ? nationName : "Unknown");
            if (playerUuid.equals(nationLeader)) {
                book.append(" (§oLeader§r)");
            }
            book.append("\n\n");
        } else {
            book.append("§lNation§r\nNone\n\n");
        }

        // Town Info
        List<String> towns = plugin.getDatabase().getTownDAO().getTownUuidsByPlayerUuid(playerUuid);
        if (towns.isEmpty()) {
            book.append("§lTowns§r\nNone\n");
        } else {
            book.append("§lTowns§r\n");
            for (String townUuid : towns) {
                String name = plugin.getDatabase().getTownDAO().getTownName(townUuid);
                String leader = plugin.getDatabase().getTownDAO().getLeaderUuid(townUuid);
                book.append(" - ").append(name);
                if (playerUuid.equals(leader)) {
                    book.append(" (§oLeader§r)");
                }
                book.append("\n");
            }
        }

        String lore = playerName + "'s Identity";
        
        List<String> pages = paginateString(book.toString());
        return sendSuccessBook(lecternBlock, player, IDENTITY_BOOK_COMMAND, pages, lore);
    }

}
