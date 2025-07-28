package com.blothera.book.handler.infobook.town;

import com.blothera.NationPlugin;
import com.blothera.book.BookResult;
import com.blothera.book.handler.infobook.InfoBookHandler;
import com.blothera.database.TownDAOs.TownLecternDAO;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.*;

public class TownHelpHandler extends InfoBookHandler {

    private final TownLecternDAO townLecternDAO;

    public TownHelpHandler(NationPlugin plugin) {
        super(plugin);
        this.townLecternDAO = plugin.getDatabase().getTownLecternDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(HELP_COMMAND, HELP_TOWN_COMMAND, TOWN_HELP_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return townLecternDAO.isTownLectern(lecternBlock);
    }


    @Override
    public BookResult execute(Player player, Block lecternBlock, BookMeta meta) {

        String bookText = "§lTown Scrapbook§r" +
                "\n" +
                getDate() + "\n" +
                "\n" +
                "Towns are the foundation of nations.\n" +
                "\n" +
                "§lCreating a Town§r\n" +
                "You must be part of a nation to create a town.\n" +
                "\n" +
                "Place a book titled 'Form Town' into a town lectern.\n" +
                "\n" +
                "The first line of the book should contain the name of the town.\n" +
                "\n" +
                "You must be standing within an unclaimed chunk to form a town.\n" +
                "\n" +
                "§lJoining a Town§r\n" +
                "Place a book titled 'Join' into a town lectern within the town you want to join.\n" +
                "\n" +
                "The first line of the book should contain the town name.\n" +
                "\n" +
                "Your request will be reviewed by the town leader.\n" +
                "\n" +
                "You can withdraw your request by placing a book titled 'Withdraw Join' into the town lectern.\n" +
                "\n" +
                "§lLeaving a Town§r\n" +
                "To leave a town, place a book titled 'Leave Town' into any town lectern.\n" +
                "\n" +
                "You will immediately be removed from the town.\n" +
                "\n" +
                "If you leave your last town in a nation, you will automatically leave the nation.\n" +
                "\n" +
                "§lManaging a Town§r\n" +
                "To manage a town, you must be the leader of the town.\n" +
                "\n" +
                "Place books with the following titles into your town lectern:\n" +
                "\n" +
                "'Pay Tax' Pay the weekly town tax.\n" +
                "\n" +
                "'Accept Join' Accepts a join request from a player.\n" +
                "\n" +
                "'Deny Join' Denies a join request from a player.\n" +
                "\n" +
                "'Requests' Shows the join requests for this town.\n" +
                "\n" +
                "'Rename Town' Renames the town.\n" +
                "\n" +
                "'Expand Town' Expands your town's chunk claims.\n" +
                "\n" +
                "'Kick Town' Exile a player from the town.\n" +
                "\n" +
                "'Disband Town' Disband the town. This action cannot be undone.\n" +
                "\n" +
                "'Town Leader' Transfer the leadership of the town to another player.\n" +
                "\n" +
                "'Info' Displays information about the town. Anyone can use this book.\n" +
                "\n" +
                "'Help' Displays this help book.\n" +
                "\n" +
                "'Who Am I' Displays information about the player. Can be used in any lectern.\n" +
                "\n" +
                "§lTown Tax§r\n" +
                "Towns must pay a recurring tax to remain active.\n" +
                "\n" +
                "Use the 'Pay Tax' book in your town lectern.\n" +
                "\n" +
                "Failure to pay tax will mark the town as dormant unless certain conditions are met.\n" +
                "\n" +
                "§lDormant Towns§r\n" +
                "A town is considered dormant if it has not paid tax and has no active lecterns.\n" +
                "\n" +
                "Dormant towns lose protection and are flagged for deletion after " + DORMANT_DAYS_TO_DELETE_TOWN + " days.\n" +
                "\n" +
                "Capitals must maintain all 3 lecterns: town, nation, and diplomacy.\n" +
                "\n" +
                "None capital towns only need a town lectern to remain active.\n" +
                "\n" +
                "If a dormant town is deleted, its land is permanently lost.\n" +
                "\n" +
                "If a dormant capital is deleted, leadership will attempt to transfer to the strongest candidate.\n" +
                "\n" +
                "§lTips§r\n" +
                "Claiming chunks protects the land from griefing.\n" +
                "\n" +
                "All town actions are performed using books placed in town lecterns.\n" +
                "\n" +
                "Some books are considered 'update books' and will refresh with new data when used.\n" +
                "\n" +
                "More features will be added as Blothera develops.\n" +
                "\n" +
                "Report any issues to the server staff.\n";

        List<String> pages = paginateString(bookText);
        String originalAuthor = resolveAuthor(meta, lecternBlock, player);

        return sendSuccessBook(lecternBlock, player, TOWN_HELP_COMMAND, pages, "Town Help", originalAuthor);
    }
}
