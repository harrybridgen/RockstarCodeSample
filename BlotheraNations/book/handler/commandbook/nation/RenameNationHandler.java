package com.blothera.book.handler.commandbook.nation;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.CommandBookHandler;
import com.blothera.book.BookResult;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationLecternDAO;
import com.blothera.database.TownDAOs.TownClaimDAO;
import com.blothera.database.TownDAOs.TownDAO;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import com.blothera.event.nation.NationRenamedEvent;
import java.util.List;

import static com.blothera.util.NationConstants.RENAME_NATION_COMMAND;

public class RenameNationHandler extends CommandBookHandler {

    private final NationDAO nationDAO;
    private final TownClaimDAO townClaimDAO;
    private final TownDAO townDAO;
    private final NationLecternDAO nationLecternDAO;

    public RenameNationHandler(NationPlugin plugin) {
        super(plugin);
        this.nationDAO = plugin.getDatabase().getNationDAO();
        this.townClaimDAO = plugin.getDatabase().getTownClaimDAO();
        this.townDAO = plugin.getDatabase().getTownDAO();
        this.nationLecternDAO = plugin.getDatabase().getNationLecternDAO();

    }

    @Override
    protected List<String> getAcceptedTitles() {
        return List.of(RENAME_NATION_COMMAND);
    }

    @Override
    protected boolean isCorrectLectern(Block lecternBlock) {
        return nationLecternDAO.isNationLectern(lecternBlock);
    }

    @Override
    protected BookResult execute(Player player, Block lecternBlock, BookMeta meta) {
        if (meta.getPageCount() < 1) {
            return sendErrorBook(lecternBlock, player,
                    "You must write the new name on the first page.");
        }
        String newName = PlainTextComponentSerializer.plainText().serialize(meta.page(1)).split("\n")[0].trim();
        if (newName.isEmpty()) {
            return sendErrorBook(lecternBlock, player, "Nation name cannot be empty.");
        }
        if (newName.length() < 3) {
            return sendErrorBook(lecternBlock, player, "The name " + newName + " is minimal, the land cannot remember it.");
        }
        if (newName.length() > 24) {
            return sendErrorBook(lecternBlock, player, "The name " + newName + " is much too large for mere mortals to remember.");
        }
        if (!isValidName(newName)) {
            return sendErrorBook(lecternBlock, player, "The name '" + newName + "' is rejected by the land.");
        }
        if (nationDAO.nationExists(newName)) {
            return sendErrorBook(lecternBlock, player, "A nation with that name already exists.");
        }

        String townUuid = townClaimDAO.getTownIdAt(
                lecternBlock.getWorld().getName(),
                lecternBlock.getChunk().getX(),
                lecternBlock.getChunk().getZ()
        );
        if (townUuid == null) {
            return sendErrorBook(lecternBlock, player, "This lectern is not in a claimed town.");
        }

        String nationUuid = townDAO.getNationUuidFromTownUuid(townUuid);
        if (nationUuid == null) {
            return sendErrorBook(lecternBlock, player, "This town does not belong to a nation.");
        }

        if (!nationDAO.getLeaderUuid(nationUuid).equals(player.getUniqueId().toString())) {
            return sendErrorBook(lecternBlock, player, "Only the nation leader can rename the nation.");
        }

        String oldName = nationDAO.getNationName(nationUuid);
        nationDAO.renameNation(nationUuid, newName);
        Bukkit.getPluginManager().callEvent(new NationRenamedEvent(nationUuid, oldName, newName));

        return sendSuccessBook(lecternBlock, player,
                "Proclamation of Renaming",
                "§lNation Renamed§r" +
                        "\n" +
                        newName +
                        "\n" +
                        getDate() +
                        "\n\n" +
                        "By the will of its people and the hand of its leader, the nation shall now be known as §o" + newName + "§r." +
                        "\n\n" +
                        "Let the land remember this change, and the world bear witness.",
                newName
        );

    }

}
