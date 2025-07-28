package com.blothera.book.handler.infobook.nation;

import com.blothera.NationPlugin;
import com.blothera.book.handler.infobook.InfoBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.NationDAOs.NationLecternDAO;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.*;

public class NationHelpHandler extends InfoBookHandler {

    private final NationLecternDAO nationLecternDAO;

    public NationHelpHandler(NationPlugin plugin) {
        super(plugin);
        this.nationLecternDAO = plugin.getDatabase().getNationLecternDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(
                HELP_COMMAND,
                HELP_NATION_COMMAND,
                NATION_HELP_COMMAND
        );
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return nationLecternDAO.isNationLectern(lecternBlock);
    }


    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {

        String helpText = "§lNation Scrapbook§r" +
                "\n" +
                getDate() + "\n" +
                "\n" +
                "Only those who wear a Crown may form a Nation.\n" +
                "\n" +
                "§lCreating a Nation§r\n" +
                "Place a book titled 'Form Nation' into your nation's lectern.\n" +
                "\n" +
                "The first line of the book should contain the name of your nation.\n" +
                "\n" +
                "§lJoining a Nation§r\n" +
                "To join a nation, you must first join a town.\n" +
                "\n" +
                "Place a book titled 'Join' into the town lectern.\n" +
                "\n" +
                "The first line of the book should contain the name of the town you wish to join.\n" +
                "\n" +
                "If the town accepts your request, you will be added to the town.\n" +
                "\n" +
                "You will automatically become a citizen of the nation.\n" +
                "\n" +
                "You will be able to join other towns within the same nation.\n" +
                "\n" +
                "§lLeaving a Nation§r\n" +
                "There are two ways to leave a nation:\n" +
                "\n" +
                "1) Leave every town within the nation.\n" +
                "\n" +
                "2) Place a book titled 'Leave Nation' into any lectern within the nation.\n" +
                "\n" +
                "§lManaging a Nation§r\n" +
                "To manage a nation, you must be the nation leader.\n" +
                "\n" +
                "Place books with the following titles into your nation lectern:\n" +
                "\n" +
                "'Rename Nation' Rename the nation.\n" +
                "\n" +
                "'Nation Leader' Sets a new nation leader.\n" +
                "\n" +
                "'Move Capital' Relocates the capital. Must be delivered to target town.\n" +
                "\n" +
                "'Kick Nation' Kicks the player from the nation.\n" +
                "\n" +
                "'Info' Displays information about the nation. Anyone can use this book.\n" +
                "\n" +
                "'Help' Displays this help book.\n" +
                "\n" +
                "'Who Am I' Displays information about the player. Can be used in any lectern.\n" +
                "\n" +
                "§lTips§r\n" +
                "Nations are formed by players who wear a Crown.\n" +
                "\n" +
                "Titles or targets do not need to be capitalized.\n" +
                "\n" +
                "We recommend naming your nation and towns with correct capitalization.\n" +
                "\n" +
                "Some books are 'update books', meaning they will update on the lectern with the latest information, including this book.\n" +
                "For example, 'Nation Info' will update with the latest information about the nation.\n" +
                "\n" +
                "There may be multiple ways to perform the same action, such as 'Form Nation', 'Create Nation', 'Make Nation'.\n" +
                "\n" +
                "BlotheraNations is still in development, and new features are being added regularly.\n" +
                "\n" +
                "If you encounter any issues, please report them to the server administrators.\n";

        List<String> pages = paginateString(helpText);

        String originalAuthor = resolveAuthor(meta, lecternBlock, player);
        return sendSuccessBook(lecternBlock, player, NATION_HELP_COMMAND, pages, "Nation Help", originalAuthor);
    }
}
