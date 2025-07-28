import pygame

from utilities.constants import (
    WHITE,
    TAN,
    BLACK,
    OFF_BLACK,
    PADDING,
    DARK_TAN,
    DARK_GREY,
)


class Inventory:
    def __init__(self):
        self.items = []
        self.rows = 5
        self.columns = 4
        self.cell_size = 40
        self.is_dragging_inv = False
        self.is_dragging_equip = False
        self.drag_offset_x_inv = 0
        self.drag_offset_y_inv = 0
        self.drag_offset_x_equip = 0
        self.drag_offset_y_equip = 0
        self.font = pygame.font.Font(None, 25)
        self.title_surface = self.font.render("Inventory", True, BLACK)
        self.title_height = self.title_surface.get_height()
        self.padding_top = 10
        self.padding_bottom = 30

        self.grid_height = self.rows * self.cell_size
        self.grid_width = self.columns * self.cell_size

        self.inv_width = 200
        self.inv_height = 260
        self.inv_pos_x = 10
        self.inv_pos_y = 130

        self.equip_inv_height = 260
        self.equip_inv_width = 200
        self.equip_inv_pos_x = 10
        self.equip_inv_pos_y = self.inv_pos_y + self.inv_height + 10

    def add_item(self, item, amount=1):
        for _ in range(amount):
            self.items.append(item)

    def remove_item(self, item, amount=1):
        removed_count = 0
        for inv_item in self.items[:]:
            if removed_count >= amount:
                break
            if inv_item.name == item.name:
                self.items.remove(inv_item)
                removed_count += 1

    def move_item(self, item, target_inventory):
        if item in self.items:
            self.remove_item(item)
            target_inventory.add_item(item)

    def replace_item(self, item_to_insert, item_to_remove):
        index_to_remove = self.items.index(item_to_remove)
        self.items.insert(index_to_remove, item_to_insert)
        self.items.remove(item_to_remove)

    def handle_event(self, event):
        screen_width, screen_height = pygame.display.get_surface().get_size()
        if event.type == pygame.MOUSEBUTTONDOWN:
            inv_title_rect = pygame.Rect(
                self.inv_pos_x
                + self.inv_width // 2
                - self.title_surface.get_width() // 2,
                self.inv_pos_y + self.padding_top,
                self.title_surface.get_width(),
                self.title_surface.get_height(),
            )

            equip_title_surface = self.font.render("Equipment", True, WHITE)
            equip_title_rect = pygame.Rect(
                self.equip_inv_pos_x
                + self.equip_inv_width // 2
                - equip_title_surface.get_width() // 2,
                self.equip_inv_pos_y + 10,
                equip_title_surface.get_width(),
                equip_title_surface.get_height(),
            )
            if inv_title_rect.collidepoint(event.pos):
                self.is_dragging_inv = True
                self.drag_offset_x_inv = self.inv_pos_x - event.pos[0]
                self.drag_offset_y_inv = self.inv_pos_y - event.pos[1]

            elif equip_title_rect.collidepoint(event.pos):
                self.is_dragging_equip = True
                self.drag_offset_x_equip = self.equip_inv_pos_x - event.pos[0]
                self.drag_offset_y_equip = self.equip_inv_pos_y - event.pos[1]

        elif event.type == pygame.MOUSEBUTTONUP:
            self.is_dragging_inv = False
            self.is_dragging_equip = False

        elif event.type == pygame.MOUSEMOTION:
            if self.is_dragging_inv:
                new_inv_x = event.pos[0] + self.drag_offset_x_inv
                new_inv_y = event.pos[1] + self.drag_offset_y_inv

                self.inv_pos_x = max(min(new_inv_x, screen_width - self.inv_width), 0)
                self.inv_pos_y = max(min(new_inv_y, screen_height - self.inv_height), 0)

            elif self.is_dragging_equip:
                new_equip_x = event.pos[0] + self.drag_offset_x_equip
                new_equip_y = event.pos[1] + self.drag_offset_y_equip

                self.equip_inv_pos_x = max(
                    min(new_equip_x, screen_width - self.equip_inv_width), 0
                )
                self.equip_inv_pos_y = max(
                    min(new_equip_y, screen_height - self.equip_inv_height), 0
                )

    def draw_inventory(self, screen):
        tooltip_bg_surface = None
        self.inv_image = pygame.Surface(
            (
                self.inv_width + 2 * PADDING,
                self.inv_height + 2 * PADDING,
            )
        )
        pygame.draw.rect(
            self.inv_image,
            DARK_GREY,
            (
                0,
                0,
                self.inv_width + 2 * PADDING,
                self.inv_height + 2 * PADDING,
            ),
        )

        pygame.draw.rect(
            self.inv_image,
            TAN,
            (
                PADDING,
                PADDING,
                self.inv_width,
                self.inv_height,
            ),
        )

        self.inv_image.blit(
            self.title_surface,
            (
                self.inv_width // 2 - self.title_surface.get_width() // 2 + PADDING,
                self.padding_top + PADDING,
            ),
        )
        mouse_pos = pygame.mouse.get_pos()

        y_offset = self.padding_top + self.title_height + 10
        x_offset = (self.inv_width - self.grid_width) // 2

        for row in range(self.rows):
            for col in range(self.columns):
                x = x_offset + col * self.cell_size + PADDING
                y = y_offset + row * self.cell_size

                rect = pygame.Rect(x, y, self.cell_size, self.cell_size)
                adjusted_mouse_pos = (
                    mouse_pos[0] - self.inv_pos_x,
                    mouse_pos[1] - self.inv_pos_y,
                )

                if rect.collidepoint(adjusted_mouse_pos):
                    pygame.draw.rect(
                        self.inv_image,
                        DARK_TAN,
                        (x, y, self.cell_size, self.cell_size),
                    )

                item_index = row * self.columns + col
                if item_index < len(self.items):
                    item = self.items[item_index]
                    icon_width, icon_height = item.icon.get_size()
                    center_x = x + (self.cell_size - icon_width) // 2
                    center_y = y + (self.cell_size - icon_height) // 2
                    self.inv_image.blit(item.icon, (center_x, center_y))

                if rect.collidepoint(adjusted_mouse_pos) and item_index < len(
                    self.items
                ):
                    item = self.items[item_index]
                    font = pygame.font.Font(None, 22)
                    tooltip_text_surface = font.render(item.name, True, BLACK)
                    text_width, text_height = tooltip_text_surface.get_size()
                    padding = 5
                    tooltip_width = text_width + 2 * padding
                    tooltip_height = text_height + 2 * padding
                    tooltip_bg_surface = pygame.Surface((tooltip_width, tooltip_height))
                    tooltip_bg_surface.fill(TAN)
                    border_color = (
                        80,
                        80,
                        80,
                    )
                    pygame.draw.rect(
                        tooltip_bg_surface,
                        border_color,
                        (0, 0, tooltip_width, tooltip_height),
                        1,
                    )

                    tooltip_bg_surface.blit(tooltip_text_surface, (padding, padding))

                    tooltip_x_offset, tooltip_y_offset = 5, 15
                    tooltip_pos = (
                        mouse_pos[0] + tooltip_x_offset,
                        mouse_pos[1] + tooltip_y_offset,
                    )

                pygame.draw.rect(
                    self.inv_image,
                    OFF_BLACK,
                    (x, y, self.cell_size, self.cell_size),
                    1,
                )

        pygame.draw.rect(
            self.inv_image,
            BLACK,
            (
                x_offset + PADDING,
                y_offset,
                self.columns * self.cell_size,
                self.rows * self.cell_size,
            ),
            3,
        )

        screen.blit(self.inv_image, (self.inv_pos_x, self.inv_pos_y))

        if tooltip_bg_surface:
            screen.blit(tooltip_bg_surface, tooltip_pos)

    def get_inventory_rects(self):
        rects = []
        padding_top = 10
        title_surface = self.font.render("Inventory", True, BLACK)
        title_height = title_surface.get_height()

        y_offset = padding_top + title_height + 10
        grid_width = self.columns * self.cell_size
        x_offset = (self.inv_width - grid_width) // 2 + PADDING
        for row in range(self.rows):
            for col in range(self.columns):
                x = self.inv_pos_x + x_offset + col * self.cell_size
                y = self.inv_pos_y + y_offset + row * self.cell_size

                item_index = row * self.columns + col

                if item_index < len(self.items):
                    item_rect = pygame.Rect(x, y, self.cell_size, self.cell_size)
                    rects.append(item_rect)
        return rects

    def draw_equipment(self, screen, player):
        tooltip_bg_surface = None
        tooltip_pos = None
        mouse_pos = pygame.mouse.get_pos()
        self.equip_inv_image = pygame.Surface(
            (
                self.equip_inv_width + 2 * PADDING,
                self.equip_inv_height + 2 * PADDING,
            )
        )
        pygame.draw.rect(
            self.equip_inv_image,
            DARK_GREY,
            (
                0,
                0,
                self.equip_inv_width + 2 * PADDING,
                self.equip_inv_height + 2 * PADDING,
            ),
        )
        pygame.draw.rect(
            self.equip_inv_image,
            TAN,
            (
                PADDING,
                PADDING,
                self.equip_inv_width,
                self.equip_inv_height,
            ),
        )
        equip_title_surface = self.font.render("Equipment", True, BLACK)
        title_x = (
            self.equip_inv_width + 2 * PADDING - equip_title_surface.get_width()
        ) // 2
        title_y = self.padding_top + PADDING
        self.equip_inv_image.blit(
            equip_title_surface,
            (title_x, title_y),
        )
        layout = {
            "Artefact": (0.85, 0.65),
            "Feet": (0.5, 0.87),
            "Legs": (0.15, 0.65),
            "Torso": (0.15, 0.45),
            "Head": (0.5, 0.25),
            "Weapon": (0.85, 0.45),
        }
        for slot, position in layout.items():
            x, y = position
            x = int(self.equip_inv_width * x) + PADDING
            y = int(self.equip_inv_height * y) + PADDING
            centered_x = x - self.cell_size // 2
            centered_y = y - self.cell_size // 2

            rect = pygame.Rect(centered_x, centered_y, self.cell_size, self.cell_size)

            adjusted_mouse_pos = (
                mouse_pos[0] - self.equip_inv_pos_x,
                mouse_pos[1] - self.equip_inv_pos_y,
            )

            if rect.collidepoint(adjusted_mouse_pos):
                pygame.draw.rect(
                    self.equip_inv_image,
                    DARK_TAN,
                    (centered_x, centered_y, self.cell_size, self.cell_size),
                )
                pygame.draw.rect(
                    self.equip_inv_image,
                    OFF_BLACK,
                    (centered_x, centered_y, self.cell_size, self.cell_size),
                    2,
                )
                item = player.worn_equipment.get(slot)
                if item:
                    font = pygame.font.Font(None, 22)
                    tooltip_text_surface = font.render(item.name, True, BLACK)
                    text_width, text_height = tooltip_text_surface.get_size()
                    padding = 5
                    tooltip_width = text_width + 2 * padding
                    tooltip_height = text_height + 2 * padding
                    tooltip_bg_surface = pygame.Surface((tooltip_width, tooltip_height))
                    tooltip_bg_surface.fill(TAN)
                    border_color = DARK_GREY
                    pygame.draw.rect(
                        tooltip_bg_surface,
                        border_color,
                        (0, 0, tooltip_width, tooltip_height),
                        1,
                    )
                    tooltip_bg_surface.blit(tooltip_text_surface, (padding, padding))

                    tooltip_x_offset, tooltip_y_offset = 5, 15
                    tooltip_pos = (
                        mouse_pos[0] + tooltip_x_offset,
                        mouse_pos[1] + tooltip_y_offset,
                    )
            else:
                pygame.draw.rect(
                    self.equip_inv_image,
                    OFF_BLACK,
                    (centered_x, centered_y, self.cell_size, self.cell_size),
                    2,
                )

            item = player.worn_equipment.get(slot)
            if item:
                icon_width, icon_height = item.icon.get_size()
                icon_center_x = centered_x + (self.cell_size - icon_width) // 2
                icon_center_y = centered_y + (self.cell_size - icon_height) // 2
                self.equip_inv_image.blit(item.icon, (icon_center_x, icon_center_y))

        player_ui_x = (self.equip_inv_width // 2) + PADDING
        player_ui_y = (self.equip_inv_height // 2 + 10) + PADDING
        player_ui_size_x = int(self.equip_inv_width * 0.2)
        player_ui_size_y = int(self.equip_inv_height * 0.2)

        player.draw_for_ui(
            self.equip_inv_image,
            player_ui_x - player_ui_size_x // 2,
            player_ui_y - player_ui_size_y // 2,
            player_ui_size_x,
            player_ui_size_y,
        )

        screen.blit(self.equip_inv_image, (self.equip_inv_pos_x, self.equip_inv_pos_y))
        if tooltip_bg_surface:
            screen.blit(tooltip_bg_surface, tooltip_pos)

    def get_equipment_rects(self):
        layout = {
            "Artefact": (0.85, 0.65),
            "Feet": (0.5, 0.85),
            "Legs": (0.15, 0.65),
            "Torso": (0.15, 0.45),
            "Head": (0.5, 0.25),
            "Weapon": (0.85, 0.45),
        }
        equipment_rects = []
        for _, position in layout.items():
            x, y = position
            x = int(self.equip_inv_width * x) + self.equip_inv_pos_x + PADDING
            y = int(self.equip_inv_height * y) + self.equip_inv_pos_y + PADDING
            centered_x = x - self.cell_size // 2
            centered_y = y - self.cell_size // 2
            equipment_rects.append(
                pygame.Rect(centered_x, centered_y, self.cell_size, self.cell_size)
            )

        return equipment_rects
