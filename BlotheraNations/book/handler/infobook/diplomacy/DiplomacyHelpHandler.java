package com.blothera.book.handler.infobook.diplomacy;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.infobook.InfoBookHandler;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.*;

public class DiplomacyHelpHandler extends InfoBookHandler {

    private final DiplomacyLecternDAO diplomacyLecternDAO;

    public DiplomacyHelpHandler(NationPlugin plugin) {
        super(plugin);
        this.diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(HELP_COMMAND, HELP_DIPLOMACY_COMMAND, DIPLOMACY_HELP_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return diplomacyLecternDAO.isDiplomacyLectern(lecternBlock);
    }


    @Override
    public BookResult execute(Player player, Block lecternBlock, BookMeta meta) {

        String helpText = "§lDiplomacy§r" +
                "\n" +
                getDate() + "\n" +
                "\n" +
                "Diplomacy allows nations to form alliances, declare war, and manage international relations.\n" +
                "\n" +
                "§lForming Alliance§r\n" +
                "Place a book titled 'Ally' into your diplomacy lectern.\n" +
                "\n" +
                "The first line of the book should contain the name of the nation you want to ally with.\n" +
                "\n" +
                "You must deliver this book to the target nation's diplomacy lectern, then they can accept or reject the request.\n" +
                "\n" +
                "To accept or deny an ally request, place a book with the title 'accept request' or 'deny request'.\n" +
                "\n" +
                "As always, the target should be on the first line of the book.\n" +
                "\n" +
                "§lBreaking Alliance§r\n" +
                "Place a book titled 'Break Ally' into your diplomacy lectern.\n" +
                "\n" +
                "The first line should contain the name of the allied nation.\n" +
                "\n" +
                "You must deliver this book to the target nation's diplomacy lectern, then the alliance will be instantly broken.\n" +
                "\n" +
                "§lDeclaring War§r\n" +
                "Place a book titled 'Declare War' into your diplomacy lectern.\n" +
                "\n" +
                "The first line must contain the name of the nation you want to declare war on.\n" +
                "\n" +
                "You must deliver this book to the target nation's diplomacy lectern, then your nations will be at war.\n" +
                "\n" +
                "§lMaking Peace§r\n" +
                "Place a book titled 'Offer Peace' into your diplomacy lectern.\n" +
                "\n" +
                "The first line should contain the name of the nation you want to make peace with.\n" +
                "\n" +
                "You must deliver this book to the target nation's diplomacy lectern, then they can accept or reject the peace request.\n" +
                "\n" +
                "§lViewing Info§r\n" +
                "Place a book titled 'Info' into your diplomacy lectern.\n" +
                "\n" +
                "This will update the book with a list of your current allies and enemies.\n" +
                "\n" +
                "'Help' Displays this help book.\n" +
                "\n" +
                "'Who Am I' Displays information about the player. Can be used in any lectern.\n" +
                "\n" +
                "§lRequests§r\n" +
                "Place a book titled 'Requests' into your diplomacy lectern.\n" +
                "\n" +
                "This will update to show you any pending diplomatic requests from other nations.\n" +
                "\n" +
                "§lTips§r\n" +
                "Some diplomatic actions require confirmation from both nations. Others do not.\n" +
                "\n" +
                "Use diplomacy to strengthen your position, but remember: alliances can shift quickly.\n" +
                "\n" +
                "As Blothera evolves, more diplomatic features may be added.\n" +
                "\n" +
                "Report any issues to the server staff.\n";

        List<String> pages = paginateString(helpText);
        String author = resolveAuthor(meta, lecternBlock, player);

        return sendSuccessBook(lecternBlock, player, DIPLOMACY_HELP_COMMAND, pages, "Diplomacy Help", author);
    }
}
