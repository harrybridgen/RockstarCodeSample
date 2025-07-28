package com.blothera.book;

import com.blothera.NationPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LecternInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

import static com.blothera.util.NationConstants.*;


public abstract class BookHandler {
    protected final NationPlugin plugin;

    public BookHandler(NationPlugin plugin) {
        this.plugin = plugin;
    }

    protected NationPlugin getPlugin() {
        return plugin;
    }

    /**
     * Checks if this handler should handle the command based on the book's metadata
     * and the lectern block it is placed on.
     *
     * @param meta         The BookMeta of the book.
     * @param lecternBlock The Block representing the lectern.
     * @return true if this handler should handle the command, false otherwise.
     */
    public abstract boolean shouldHandleBook(BookMeta meta, Block lecternBlock);

    /**
     * Handles the command associated with the book when it is placed on the lectern.
     * This method should contain the logic for what happens when the book is used.
     *
     * @param player       The player who placed the book on the lectern.
     * @param lecternBlock The Block representing the lectern.
     * @param meta         The BookMeta of the book.
     * @return A BookResult indicating the outcome of handling the command.
     */
    public abstract BookResult handleBook(Player player, Block lecternBlock, BookMeta meta);


    /**
     * Sends a book to the player and places it on the lectern.
     * This book will be opened for the player if they are close enough.
     *
     * @param lecternBlock The lectern block where the book should be placed.
     * @param player       The player who will receive the book.
     * @param title        The title of the book.
     * @param message      The content of the book as a single string.
     * @param lore         Optional lore to add to the book.
     * @return A BookResult indicating that the book was handled and should be kept.
     */
    protected BookResult sendSuccessBook(Block lecternBlock, Player player, String title, String message, String lore) {
        Bukkit.getScheduler().runTask(getPlugin(), () -> {
            BookMeta meta = (BookMeta) Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
            meta.setTitle(title);
            meta.setAuthor(player.getName());
            meta.addPage(message);

            if (lore != null) {
                meta.setLore(List.of("§7" + lore));
            }

            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            book.setItemMeta(meta);
            placeBookOnLectern(lecternBlock, player, book);
        });
        return BookResult.HANDLED_BOOK;
    }

    /**
     * Sends a book to the player and places it on the lectern.
     * This book will be opened for the player if they are close enough.
     *
     * @param lecternBlock   The lectern block where the book should be placed.
     * @param player         The player who will receive the book.
     * @param title          The title of the book.
     * @param message        The content of the book as a list of strings.
     * @param lore           Optional lore to add to the book.
     * @param originalAuthor The original author of the book, if available.
     * @return A BookResult indicating that the book was handled and should be kept.
     */
    protected BookResult sendSuccessBook(Block lecternBlock, Player player, String title, List<String> message, String lore, String originalAuthor) {
        Bukkit.getScheduler().runTask(getPlugin(), () -> {
            BookMeta meta = (BookMeta) Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
            meta.setTitle(title);
            meta.setAuthor(originalAuthor != null ? originalAuthor : "Error, this should not happen");
            meta.setPages(message);

            if (lore != null) {
                meta.setLore(List.of("§7" + lore));
            }

            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            book.setItemMeta(meta);
            placeBookOnLectern(lecternBlock, player, book);
        });
        return BookResult.HANDLED_BOOK;
    }

    /**
     * Sends a book to the player and places it on the lectern.
     * This book will be opened for the player if they are close enough.
     *
     * @param lecternBlock The lectern block where the book should be placed.
     * @param player       The player who will receive the book.
     * @param title        The title of the book.
     * @param message      The content of the book as a list of strings.
     * @param lore         Optional lore to add to the book.
     * @return A BookResult indicating that the book was handled and should be kept.
     */
    protected BookResult sendSuccessBook(Block lecternBlock, Player player, String title, List<String> message, String lore) {
        Bukkit.getScheduler().runTask(getPlugin(), () -> {
            BookMeta meta = (BookMeta) Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
            meta.setTitle(title);
            meta.setAuthor(player.getName());
            meta.setPages(message);

            if (lore != null) {
                meta.setLore(List.of("§7" + lore));
            }

            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            book.setItemMeta(meta);
            placeBookOnLectern(lecternBlock, player, book);
        });
        return BookResult.HANDLED_BOOK;
    }


    /**
     * Sends a book to the player and places it on the lectern.
     * This book will be opened for the player if they are close enough.
     *
     * @param lecternBlock The lectern block where the book should be placed.
     * @param player       The player who will receive the book.
     * @param book         The ItemStack representing the book to be placed.
     * @return A BookResult indicating that the book was handled and should be kept.
     */
    protected BookResult sendSuccessBook(Block lecternBlock, Player player, ItemStack book) {
        Bukkit.getScheduler().runTask(getPlugin(), () -> {
            placeBookOnLectern(lecternBlock, player, book);
        });
        return BookResult.HANDLED_BOOK;
    }


    /**
     * Sends an error book to the player with a specific message.
     * This book will be placed on the lectern and opened for the player.
     *
     * @param lecternBlock The lectern block where the book should be placed.
     * @param player       The player who will receive the error book.
     * @param message      The error message to display in the book.
     * @return A BookResult indicating that the book was handled and should be kept.
     */
    protected BookResult sendErrorBook(Block lecternBlock, Player player, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            BookMeta meta = (BookMeta) Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
            meta.setTitle(ERROR_TITLE);
            meta.setAuthor(player.getName());
            meta.addPage(message);
            meta.getPersistentDataContainer().set(
                    ERROR_BOOK_KEY,
                    PersistentDataType.INTEGER,
                    1
            );
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            book.setItemMeta(meta);
            placeBookOnLectern(lecternBlock, player, book);
        });
        return BookResult.ERROR_BOOK;
    }

    /**
     * Places the given book on the lectern and opens it for the player if they are close enough.
     *
     * @param lecternBlock The lectern block where the book should be placed.
     * @param player       The player who will receive the book.
     * @param book         The ItemStack representing the book to be placed.
     */
    private void placeBookOnLectern(Block lecternBlock, Player player, ItemStack book) {
        Lectern lectern = (Lectern) lecternBlock.getState();
        LecternInventory inv = (LecternInventory) lectern.getInventory();
        inv.clear();
        inv.setItem(0, book);

        if (player.getLocation().distance(lecternBlock.getLocation()) < 5) {
            player.openBook(book);
        }
    }

    /**
     * Retrieves the author of the book on the lectern, if available.
     * If the lectern has no book or the book has no author, returns null.
     *
     * @param lecternBlock The lectern block to check.
     * @return The author of the book, or null if not available.
     */
    protected String getLecternBookAuthor(Block lecternBlock) {
        if (lecternBlock.getState() instanceof Lectern lectern) {
            ItemStack item = lectern.getInventory().getItem(0);
            if (item != null && item.getItemMeta() instanceof BookMeta meta) {
                return meta.getAuthor();
            }
        }
        return null;
    }

    /**
     * Resolves the author of a book, preferring the book's metadata if available,
     * then checking the lectern's book, and finally falling back to the player's name.
     *
     * @param meta         The BookMeta of the book, may be null.
     * @param lecternBlock The lectern block where the book is placed.
     * @param fallback     The player to use as a fallback for the author.
     * @return The resolved author name.
     */
    protected String resolveAuthor(BookMeta meta, Block lecternBlock, Player fallback) {
        if (meta != null && meta.hasAuthor()) {
            return meta.getAuthor();
        }
        String lecternAuthor = getLecternBookAuthor(lecternBlock);

        String author;
        if (lecternAuthor != null && !lecternAuthor.isBlank()) {
            author = lecternAuthor;
        } else {
            author = fallback.getName();
        }
        return author;
    }

    /**
     * Paginate a raw string into multiple pages based on the maximum line width and number of lines.
     * This method handles word wrapping and ensures that no page exceeds the maximum line width.
     *
     * @param raw The raw string to paginate.
     * @return A list of strings, each representing a page of the book.
     */
    protected List<String> paginateString(String raw) {
        List<String> pages = new ArrayList<>();
        List<String> currentLines = new ArrayList<>();

        for (String paragraph : raw.split("\n")) {
            StringBuilder line = new StringBuilder();
            int lineWidth = 0;

            // If paragraph is completely blank, treat it as a blank line
            if (paragraph.isBlank()) {
                if (currentLines.size() < MAX_LINES_PER_PAGE) {
                    currentLines.add("");
                } else {
                    // Flush page, but skip the blank line at top
                    pages.add(String.join("\n", currentLines));
                    currentLines.clear();
                }
                continue;
            }

            for (String word : paragraph.split(" ")) {
                int wordWidth = getPixelWidth(word + " ");
                if (lineWidth + wordWidth > MAX_LINE_WIDTH) {
                    String builtLine = line.toString().stripTrailing();

                    // Don't start a new page with a blank line
                    if (currentLines.size() >= MAX_LINES_PER_PAGE) {
                        pages.add(String.join("\n", currentLines));
                        currentLines.clear();
                    }

                    currentLines.add(builtLine);
                    line = new StringBuilder();
                    lineWidth = 0;
                }

                line.append(word).append(" ");
                lineWidth += wordWidth;
            }

            if (!line.isEmpty()) {
                if (currentLines.size() >= MAX_LINES_PER_PAGE) {
                    pages.add(String.join("\n", currentLines));
                    currentLines.clear();
                }
                currentLines.add(line.toString().stripTrailing());
            }
        }

        // Final flush, removing leading blank line if needed
        while (!currentLines.isEmpty() && currentLines.getFirst().isBlank()) {
            currentLines.removeFirst();
        }

        if (!currentLines.isEmpty()) {
            pages.add(String.join("\n", currentLines));
        }

        return pages;
    }


    /**
     * Paginate a list of strings into multiple pages based on the maximum line width and number of lines.
     * This method handles word wrapping and ensures that no page exceeds the maximum line width.
     *
     * @param lines The list of strings to paginate.
     * @return A list of strings, each representing a page of the book.
     */
    protected List<String> paginateLines(List<String> lines) {
        return paginateString(String.join("\n", lines));
    }

    /**
     * Calculates the pixel width of a line of text, considering formatting codes and character widths.
     * This method supports basic formatting like bold and resets formatting codes.
     *
     * @param line The line of text to measure.
     * @return The pixel width of the line.
     */
    private int getPixelWidth(String line) {
        int width = 0;
        boolean skipNext = false;
        boolean bold = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (skipNext) {
                // Handle formatting codes like §l, §r, etc.
                skipNext = false;
                switch (c) {
                    case 'l' -> bold = true;  // Enable bold
                    case 'r' -> bold = false; // Reset formatting
                    // Add more cases if needed (e.g., §o for italic, etc.)
                }
                continue;
            }

            if (c == '§') {
                skipNext = true;
                continue;
            }

            int charWidth = CHARACTER_WIDTHS.getOrDefault(c, 6);
            if (bold && c != ' ') {
                charWidth += 1;
            }

            width += charWidth;
        }

        return width;
    }

    /**
     * Validates a name according to specific rules:
     * - Must start with a letter
     * - Can only contain letters, spaces, hyphens, and apostrophes
     * - No double spaces or awkward start/end characters
     * - Cannot contain banned patterns
     *
     * @param name The name to validate.
     * @return true if the name is valid, false otherwise.
     */
    public static boolean isValidName(String name) {
        if (name == null) return false;

        name = name.trim();

        if (name.length() < 3 || name.length() > 24) {
            return false; // Length must be between 3 and 24 characters
        }

        // Must start with a letter
        if (!Character.isLetter(name.charAt(0))) return false;

        if (name.startsWith(NATION_HEADER) || name.startsWith(TOWN_HEADER)
                || name.startsWith(DIPLOMACY_HEADER)) {
            return false; // Cannot start with these headers
        }

        // Allowed characters: letters, space, hyphen, apostrophe
        for (char c : name.toCharArray()) {
            if (!Character.isLetter(c) && c != ' ' && c != '-' && c != '\'') {
                return false; // Invalid character found
            }
        }

        // No double spaces or awkward start/end chars
        if (name.matches(".*\\s{2,}.*")) return false;
        if (name.startsWith(" ") || name.endsWith(" ")
                || name.startsWith("-") || name.endsWith("-")
                || name.startsWith("'") || name.endsWith("'")) return false;

        // Check banned pattern
        return !BANNED_PATTERN.matcher(name.toLowerCase()).find();
    }


    /**
     * Gets the current date formatted as "Month DaySuffix, Year".
     * Example: "January 1st, 2023"
     *
     * @return The formatted date string.
     */
    public static String getDate() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String month = today.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
        int day = today.getDayOfMonth();
        String suffix = (day >= 11 && day <= 13) ? "th" : switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
        return month + " " + day + suffix + ", " + today.getYear();
    }

    /**
     * Formats a raw date string in "yyyy-MM-dd" format to "Month DaySuffix, Year".
     * Example: "2023-01-01" becomes "January 1st, 2023"
     *
     * @param rawDate The raw date string to format: Expects "yyyy-MM-dd".
     * @return The formatted date string, or the original if parsing fails.
     */
    public static String formatDate(String rawDate) {
        try {
            java.time.LocalDate parsed = java.time.LocalDate.parse(rawDate);
            String month = parsed.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
            int day = parsed.getDayOfMonth();
            String suffix = (day >= 11 && day <= 13) ? "th" : switch (day % 10) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
            return month + " " + day + suffix + ", " + parsed.getYear();
        } catch (Exception e) {
            return rawDate; // fallback
        }
    }

    /**
     * Counts the number of emeralds in the given inventory.
     *
     * @param inv The inventory to check.
     * @return The total number of emeralds in the inventory.
     */
    protected int countEmeralds(Inventory inv) {
        return inv.all(Material.EMERALD).values().stream().mapToInt(ItemStack::getAmount).sum();
    }

    /**
     * Deducts a specified amount of emeralds from the inventory.
     * It will remove emeralds from the first available stacks until the amount is met.
     *
     * @param inv    The inventory to deduct emeralds from.
     * @param amount The amount of emeralds to deduct.
     */
    protected void deductEmeralds(Inventory inv, int amount) {
        int remaining = amount;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == Material.EMERALD) {
                int take = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
    }

    /**
     * Checks if there is a chest adjacent to the lectern.
     * It checks the four cardinal directions (north, south, east, west).
     *
     * @param lectern The lectern block to check.
     * @return The adjacent chest block if found, otherwise null.
     */
    protected Block getAdjacentChest(Block lectern) {
        for (Block face : List.of(
                lectern.getRelative(1, 0, 0),
                lectern.getRelative(-1, 0, 0),
                lectern.getRelative(0, 0, 1),
                lectern.getRelative(0, 0, -1)
        )) {
            if (face.getType() == Material.CHEST) return face;
        }
        return null;
    }

}

