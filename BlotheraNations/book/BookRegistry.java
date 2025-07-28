package com.blothera.book;

import com.blothera.NationPlugin;
import com.blothera.book.handler.commandbook.diplomacy.*;
import com.blothera.book.handler.commandbook.nation.LeaveNationHandler;
import com.blothera.book.handler.commandbook.nation.NationFormationHandler;
import com.blothera.book.handler.commandbook.nation.RenameNationHandler;
import com.blothera.book.handler.commandbook.nation.TransferNationLeaderHandler;
import com.blothera.book.handler.commandbook.town.*;
import com.blothera.book.handler.deliverybook.create.*;
import com.blothera.book.handler.deliverybook.place.*;
import com.blothera.book.handler.infobook.diplomacy.DiplomacyHelpHandler;
import com.blothera.book.handler.infobook.diplomacy.DiplomacyInfoBookHandler;
import com.blothera.book.handler.infobook.diplomacy.ViewDiplomacyRequestsHandler;
import com.blothera.book.handler.infobook.diplomacy.WarInfoBookHandler;
import com.blothera.book.handler.infobook.nation.NationHelpHandler;
import com.blothera.book.handler.infobook.nation.NationInfoBookHandler;
import com.blothera.book.handler.infobook.player.WhoAmIHandler;
import com.blothera.book.handler.infobook.town.TownHelpHandler;
import com.blothera.book.handler.infobook.town.TownInfoBookHandler;
import com.blothera.book.handler.infobook.town.ViewJoinRequestsHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class BookRegistry {

    private static final List<BookHandler> handlers = new ArrayList<>();

    public BookRegistry(NationPlugin plugin) {

        register(new ViewJoinRequestsHandler(plugin));
        register(new TownInfoBookHandler(plugin));
        register(new NationFormationHandler(plugin));
        register(new TownFormationHandler(plugin));
        register(new NationInfoBookHandler(plugin));
        register(new RequestJoinHandler(plugin));
        register(new AcceptJoinRequestHandler(plugin));
        register(new DenyJoinRequestHandler(plugin));
        register(new LeaveNationHandler(plugin));
        register(new ExpandTownHandler(plugin));
        register(new DisbandTownHandler(plugin));
        register(new ExileFromTownHandler(plugin));
        register(new RelocateCapitalCreateHandler(plugin));
        register(new RelocateCapitalPlaceHandler(plugin));
        register(new RenameNationHandler(plugin));
        register(new RenameTownHandler(plugin));
        register(new TransferNationLeaderHandler(plugin));
        register(new TransferTownLeaderHandler(plugin));
        register(new RequestAllyCreateHandler(plugin));
        register(new ViewDiplomacyRequestsHandler(plugin));
        register(new RequestAllyPlaceHandler(plugin));
        register(new BreakAllianceCreateHandler(plugin));
        register(new AcceptAllyHandler(plugin));
        register(new DenyAllyHandler(plugin));
        register(new BreakAlliancePlaceHandler(plugin));
        register(new DiplomacyInfoBookHandler(plugin));
        register(new DeclareWarCreateHandler(plugin));
        register(new DeclareWarPlaceHandler(plugin));
        register(new RequestPeaceCreateHandler(plugin));
        register(new RequestPeacePlaceHandler(plugin));
        register(new AcceptPeaceHandler(plugin));
        register(new DenyPeaceHandler(plugin));
        register(new WithdrawJoinRequestHandler(plugin));
        register(new NationHelpHandler(plugin));
        register(new TownHelpHandler(plugin));
        register(new DiplomacyHelpHandler(plugin));
        register(new LeaveTownHandler(plugin));
        register(new PayTaxHandler(plugin));
        register(new WhoAmIHandler(plugin));
        register(new AcceptJoinWarHandler(plugin));
        register(new DenyJoinWarHandler(plugin));
        register(new RequestJoinWarPlaceHandler(plugin));
        register(new RequestJoinWarCreateHandler(plugin));
        register(new WarInfoBookHandler(plugin));
    }


    /**
     * Registers a book handler to the registry.
     *
     * @param handler The BookHandler to register.
     */
    private void register(BookHandler handler) {
        if (!handlers.contains(handler)) {
            handlers.add(handler);
        }
    }

    /**
     * Handles the book command for a player at a lectern.
     *
     * @param player       The player who is interacting with the book.
     * @param lecternBlock The block representing the lectern.
     * @param meta         The metadata of the book being handled.
     * @return A BookResult indicating the outcome of the handling.
     */
    public static BookResult handle(Player player, Block lecternBlock, BookMeta meta) {
        for (BookHandler handler : handlers) {

            boolean shouldHandle = handler.shouldHandleBook(meta, lecternBlock);

            if (!shouldHandle) {
                continue; // skip handlers that don't match the book or lectern
            }

            BookResult result = handler.handleBook(player, lecternBlock, meta);

            if (result == BookResult.ERROR_BOOK) {
                return BookResult.ERROR_BOOK;
            } else if (result == BookResult.HANDLED_BOOK) {
                return BookResult.HANDLED_BOOK;
            }
        }
        return BookResult.NOT_HANDLED;
    }
}



