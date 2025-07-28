package com.blothera.book.handler.commandbook.nation;

import com.blothera.NationPlugin;
import com.blothera.book.BookHandler;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationLecternDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.event.nation.NationFormationEvent;
import com.blothera.event.town.BanditCheckEvent;
import com.blothera.item.CrownItem;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

import static com.blothera.util.NationConstants.*;

public class NationFormationHandler extends CommandBookHandler {

    private final NationLecternDAO nationLecternDAO;
    private final NationDAO nationDAO;
    private final NationMemberDAO nationMemberDAO;

    public NationFormationHandler(NationPlugin plugin) {
        super(plugin);
        this.nationLecternDAO = plugin.getDatabase().getNationLecternDAO();
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.nationMemberDAO = plugin.getDatabase().getNationMemberDAO();
    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(
                FORM_NATION_COMMAND,
                FOUND_NATION_COMMAND,
                MAKE_NATION_COMMAND,
                CREATE_NATION_COMMAND
        );
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return nationLecternDAO.isNationLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        BanditCheckEvent checkEvent = new BanditCheckEvent(player);
        Bukkit.getPluginManager().callEvent(checkEvent);
        if (checkEvent.isCancelled()) {
            return sendErrorBook(lecternBlock, player, checkEvent.getCancelReason());
        }

        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player, "The land does not recognize silence. Name your Nation.");
        }

        ItemStack helmet = player.getInventory().getHelmet();

        if (!CrownItem.isCrown(helmet)) {
            return sendErrorBook(lecternBlock, player, "The land does not accept your claim. You lack the Crown.");
        }

        if (!CrownItem.isUnused(helmet)) {
            return sendErrorBook(lecternBlock, player, "This Crown has already spoken. The land will not hear it again.");
        }

        if (nationDAO.playerInNation(player.getUniqueId().toString())) {
            return sendErrorBook(lecternBlock, player, "You already belong to a Nation. The land will not be fooled.");
        }

        String nationName = meta.getPage(1).split("\n")[0].trim();

        if (nationName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "The land does not recognize silence. Name your Nation.");
        }
        if (nationName.length() < 3) {
            return sendErrorBook(lecternBlock, player, "The name " + nationName + " is too short for your grand Nation!");
        }
        if (nationName.length() > 24) {
            return sendErrorBook(lecternBlock, player, "The name " + nationName + " is too long, the land cannot remember it.");
        }
        if (!isValidName(nationName) && !player.isOp()) {
            return sendErrorBook(lecternBlock, player, "The name '" + nationName + "' is rejected by the land.");
        }
        if (nationDAO.nationNameExists(nationName)) {
            return sendErrorBook(lecternBlock, player, "That name is already spoken. The land remembers it.");
        }

        String leaderUuid = player.getUniqueId().toString();
        String nationUuid = java.util.UUID.randomUUID().toString();
        nationDAO.createNation(nationUuid, nationName, leaderUuid, 1);
        nationMemberDAO.addMember(leaderUuid, nationUuid);
        CrownItem.markUsed(helmet, nationName);

        String title = "Declaration of Nation";
        String dbDate = nationDAO.getFormationDate(nationUuid);
        if (dbDate == null) {
            return sendErrorBook(lecternBlock, player, "The land does not remember the date of your formation. Please contact an admin.");
        }

        Bukkit.getPluginManager().callEvent(new NationFormationEvent(nationName, player.getName(), lecternBlock.getLocation()));

        String date = BookHandler.formatDate(dbDate);
        String content = "§lNation Founded§r" +
                "\n" +
                nationName +
                "\n" +
                date +
                "\n\n" +
                "§o" + player.getName() + "§r has raised the banner of a new nation: §o" + nationName + "§r." +
                "\n\n" +
                "Let the land witness this founding and remember its name.";

        return sendSuccessBook(lecternBlock, player, title, content, nationName);
    }
}
