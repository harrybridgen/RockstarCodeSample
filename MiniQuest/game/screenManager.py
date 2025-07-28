from sys import exit

import pygame
import time
import random
from game.game import Game
from utilities.resourcePath import resource_path


class ScreenManager:
    def __init__(self, screen_width, screen_height):
        self.screen_resolution = (screen_width, screen_height)
        self.screen_width = screen_width
        self.screen_height = screen_height
        self.is_fullscreen = False
        self.music_volume = 1.0  # Default music volume
        self.effects_volume = 1.0  # Default sound effects volume
        self.difficulty = "Medium"  # Default difficulty
        pygame.init()
        pygame.mixer.init()
        pygame.display.set_caption("MiniQuest")
        pygame.font.init()
        pygame.mouse.set_visible(False)
        pygame.display.set_icon(pygame.image.load(resource_path("source/img/mq.ico")))
        flags = pygame.DOUBLEBUF | pygame.HWSURFACE
        self.game_screen = pygame.display.set_mode(
            (self.screen_width, self.screen_height), flags, depth=16, vsync=1
        )
        self.cursor_img = pygame.image.load(
            resource_path("source/img/cursor.png")
        ).convert_alpha()

        self.game = None
        self.screen = "main_menu"
        self.music_fading = False
        self.music = "maintheme.mp3"
        pygame.mixer.music.load(resource_path("source/sound/" + self.music))
        pygame.mixer.music.play(loops=-1, fade_ms=2000)

        self.button_font = pygame.font.Font(
            resource_path("source/font/EagleLake.ttf"),
            30,
        )

    def update_music(self):
        if self.music and self.music_fading and not pygame.mixer.music.get_busy():
            pygame.mixer.music.load(resource_path("source/sound/" + self.music))
            pygame.mixer.music.play(loops=-1, fade_ms=2000)
            self.music_fading = False

    def run(self):
        while True:
            if self.screen == "game":
                if not self.game:
                    self.game = Game(
                        self.screen_width,
                        self.screen_height,
                        self.game_screen,
                    )
                    self.game.run()
                else:
                    if self.game.game_over:
                        self.screen = "death"
                        self.game = None
            elif self.screen == "main_menu":
                self.main_menu()
            elif self.screen == "options":
                self.options_screen()
            elif self.screen == "credits":
                self.show_credit_screen()
            elif self.screen == "death":
                self.death_screen()
            elif self.screen == "loading":
                self.show_loading_screen()

    def main_menu(self):
        running = True
        music_state = True
        clock = pygame.time.Clock()

        background = pygame.image.load(
            resource_path("source/img/background.png")
        ).convert_alpha()
        background = pygame.transform.scale(
            background, (self.screen_width, self.screen_height)
        )
        background_rect = background.get_rect(
            center=(self.screen_width / 2, self.screen_height / 2)
        )
        overlay = pygame.Surface(
            (self.screen_width, self.screen_height), pygame.SRCALPHA
        )
        overlay.fill((0, 0, 0, int(65 * 255 / 100)))
        overlay_rect = overlay.get_rect(
            center=(self.screen_width / 2, self.screen_height / 2)
        )
        logo = pygame.image.load(resource_path("source/img/logo.svg")).convert_alpha()

        logo_rect = logo.get_rect(
            center=(
                self.screen_width / 2,
                (self.screen_height / 2) - 200,
            )
        )

        button_gap = 20
        button_texts = ["Start Game", "Options", "Credits", "Exit"]

        buttons = [None] * len(button_texts)

        button_start_y = (
            self.screen_height / 2 - 0.5 * self.button_font.get_height() - button_gap
        )

        music_note = pygame.image.load(
            resource_path("source/img/music_note.png")
        ).convert_alpha()
        music_note_rect = music_note.get_rect(
            bottomright=(self.screen_width - 20, self.screen_height - 20)
        )
        stop_sign = pygame.image.load(
            resource_path("source/img/stop.png")
        ).convert_alpha()
        stop_sign_rect = stop_sign.get_rect(
            bottomright=(self.screen_width - 20, self.screen_height - 20)
        )
        for i, text in enumerate(button_texts):
            text_surface = self.button_font.render(text, True, (255, 255, 255))
            text_width = text_surface.get_width()
            text_height = text_surface.get_height()

            buttons[i] = pygame.Rect(
                (self.screen_width - text_width) / 2,
                button_start_y,
                text_width,
                text_height,
            )

            button_start_y += text_height + button_gap

        while running:
            self.game_screen.blit(background, background_rect)
            self.game_screen.blit(overlay, overlay_rect)
            self.game_screen.blit(logo, logo_rect)
            self.update_music()
            music_note_rect.center = stop_sign_rect.center = (
                self.screen_width - 50,
                self.screen_height - 50,
            )
            self.game_screen.blit(music_note, music_note_rect)
            if not music_state:
                self.game_screen.blit(stop_sign, stop_sign_rect)

            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    running = False

                if event.type == pygame.MOUSEBUTTONDOWN:
                    mouse_pos = event.pos
                    for i, button in enumerate(buttons):
                        if button.collidepoint(mouse_pos):
                            if button_texts[i] == "Start Game":
                                self.screen = "loading"
                                running = False

                            elif button_texts[i] == "Options":
                                self.screen = "options"
                                running = False
                            elif button_texts[i] == "Credits":
                                self.screen = "credits"
                                running = False
                            elif button_texts[i] == "Exit":
                                pygame.quit()
                                exit()
                    if music_note_rect.collidepoint(mouse_pos):
                        music_state = not music_state
                        if not music_state:
                            pygame.mixer.music.fadeout(2000)
                            self.music_fading = True
                            self.music = None
                        else:
                            self.music = "maintheme.mp3"

            mouse_pos = pygame.mouse.get_pos()

            for i, button in enumerate(buttons):
                if button.collidepoint(mouse_pos):
                    button_text = self.button_font.render(
                        button_texts[i], True, (0, 255, 0)
                    )
                else:
                    button_text = self.button_font.render(
                        button_texts[i], True, (255, 255, 255)
                    )
                button_text = pygame.transform.scale(
                    button_text,
                    (
                        button_text.get_rect().width,
                        button_text.get_rect().height,
                    ),
                )
                button_text_rect = button_text.get_rect(center=button.center)
                self.game_screen.blit(button_text, button_text_rect)

            fps = pygame.font.Font(None, 18).render(
                f"{round(clock.get_fps(), 1)} FPS", True, (255, 255, 255)
            )
            fps_rect = fps.get_rect(topleft=(15, 15))
            self.game_screen.blit(fps, fps_rect)

            version = pygame.font.Font(None, 18).render("INDEV", True, (255, 255, 255))
            version_rect = version.get_rect(bottomleft=(15, self.screen_height - 15))
            self.game_screen.blit(version, version_rect)

            self.update_screen()
            clock.tick(60)

    def show_credit_screen(self):
        running = True
        clock = pygame.time.Clock()
        y_offset = self.screen_height

        background = pygame.image.load(
            resource_path("source/img/background.png")
        ).convert_alpha()
        background = pygame.transform.scale(
            background, (self.screen_width, self.screen_height)
        )
        background_rect = background.get_rect(
            center=(self.screen_width / 2, self.screen_height / 2)
        )

        overlay = pygame.Surface(
            (self.screen_width, self.screen_height), pygame.SRCALPHA
        )
        overlay.fill((0, 0, 0, int(65 * 255 / 100)))
        overlay_rect = overlay.get_rect(
            center=(self.screen_width / 2, self.screen_height / 2)
        )

        credit_lines = [
            "Credits",
            "",
            "Lead Developer",
            "Harry Bridgen",
            "",
            "Programming",
            "Harry Bridgen",
            "",
            "Sound Design",
            "To do",
            "",
            "Artwork",
            "To do",
            "",
            "Lore",
            "JHarry Bridgen",
            "",
            "Special Thanks",
            "God",
            "",
            "Made Using",
            "Python",
            "Pygame",
            "Tiled",
            "PyTMX",
            "PyInstaller",
            "PyArmor",
            "",
            "",
            "Thanks for Playing",
        ]

        while running:
            self.game_screen.blit(background, background_rect)
            self.game_screen.blit(overlay, overlay_rect)

            for i, line in enumerate(credit_lines):
                rendered_text = self.button_font.render(line, True, (255, 255, 255))
                self.game_screen.blit(
                    rendered_text,
                    (
                        self.screen_width // 2 - rendered_text.get_width() // 2,
                        y_offset + i * 40,
                    ),
                )

            y_offset -= 1

            if y_offset + len(credit_lines) * 40 < 0:
                running = False

            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    running = False
                elif (
                    event.type == pygame.KEYDOWN or event.type == pygame.MOUSEBUTTONDOWN
                ):
                    running = False

            pygame.display.flip()
            clock.tick(30)

        self.screen = "main_menu"

    def show_loading_screen(self):
        pygame.mixer.music.fadeout(2000)
        clock = pygame.time.Clock()
        base_text = "Loading"
        max_dots = 3
        cur_dots = 0
        time_elapsed = 0
        loading_base_text = self.button_font.render(base_text, True, (255, 255, 255))
        x_position = (self.screen_width - loading_base_text.get_width()) // 2
        y_position = self.screen_height // 2

        background = pygame.image.load(
            resource_path("source/img/background.png")
        ).convert_alpha()
        background = pygame.transform.scale(
            background, (self.screen_width, self.screen_height)
        )
        background_rect = background.get_rect(
            center=(self.screen_width / 2, self.screen_height / 2)
        )

        overlay = pygame.Surface(
            (self.screen_width, self.screen_height), pygame.SRCALPHA
        )
        overlay.fill((0, 0, 0, int(65 * 255 / 100)))
        overlay_rect = overlay.get_rect(
            center=(self.screen_width / 2, self.screen_height / 2)
        )

        while True:
            self.game_screen.blit(background, background_rect)
            self.game_screen.blit(overlay, overlay_rect)

            cur_dots += 1
            if cur_dots > max_dots:
                cur_dots = 0

            loading_text = self.button_font.render(
                base_text + "." * cur_dots, True, (255, 255, 255)
            )
            self.game_screen.blit(loading_text, (x_position, y_position))

            pygame.display.flip()
            time_elapsed += clock.tick(60)

            time.sleep(1)

            if time_elapsed >= 2000:
                break

        self.screen = "game"

    def options_screen(self):
        running = True
        clock = pygame.time.Clock()

        background = pygame.image.load(resource_path("source/img/background.png")).convert_alpha()
        background = pygame.transform.scale(background, (self.screen_width, self.screen_height))
        background_rect = background.get_rect(center=(self.screen_width / 2, self.screen_height / 2))
        overlay = pygame.Surface((self.screen_width, self.screen_height), pygame.SRCALPHA)
        overlay.fill((0, 0, 0, int(65 * 255 / 100)))
        overlay_rect = overlay.get_rect(center=(self.screen_width / 2, self.screen_height / 2))

        # Define buttons and labels
        buttons = {}  # Store rects of buttons
        labels = {}   # Store labels

        button_start_y = self.screen_height / 2 - 150  # Adjust as needed

        # Fullscreen toggle button
        fullscreen_text = "Toggle Fullscreen"
        fullscreen_surface = self.button_font.render(fullscreen_text, True, (255, 255, 255))
        fullscreen_rect = fullscreen_surface.get_rect(center=(self.screen_width / 2, button_start_y))
        buttons['fullscreen'] = fullscreen_rect

        # Music Volume controls
        button_start_y += 60

        music_minus_surface = self.button_font.render("-", True, (255, 255, 255))
        music_plus_surface = self.button_font.render("+", True, (255, 255, 255))
        music_label_surface = self.button_font.render(f"Music Volume: {int(self.music_volume * 100)}%", True, (255, 255, 255))

        music_label_rect = music_label_surface.get_rect(center=(self.screen_width / 2, button_start_y))
        music_minus_rect = music_minus_surface.get_rect(right=music_label_rect.left - 20, centery=music_label_rect.centery)
        music_plus_rect = music_plus_surface.get_rect(left=music_label_rect.right + 20, centery=music_label_rect.centery)
        buttons['music_minus'] = music_minus_rect
        buttons['music_plus'] = music_plus_rect
        labels['music_label'] = music_label_rect

        # Sound Effects Volume controls
        button_start_y += 60

        effects_minus_surface = self.button_font.render("-", True, (255, 255, 255))
        effects_plus_surface = self.button_font.render("+", True, (255, 255, 255))
        effects_label_surface = self.button_font.render(f"Effects Volume: {int(self.effects_volume * 100)}%", True, (255, 255, 255))

        effects_label_rect = effects_label_surface.get_rect(center=(self.screen_width / 2, button_start_y))
        effects_minus_rect = effects_minus_surface.get_rect(right=effects_label_rect.left - 20, centery=effects_label_rect.centery)
        effects_plus_rect = effects_plus_surface.get_rect(left=effects_label_rect.right + 20, centery=effects_label_rect.centery)
        buttons['effects_minus'] = effects_minus_rect
        buttons['effects_plus'] = effects_plus_rect
        labels['effects_label'] = effects_label_rect

        # Difficulty setting
        button_start_y += 60

        difficulty_surface = self.button_font.render(f"Difficulty: {self.difficulty}", True, (255, 255, 255))
        difficulty_rect = difficulty_surface.get_rect(center=(self.screen_width / 2, button_start_y))
        buttons['difficulty'] = difficulty_rect

        # Reset to Defaults button
        button_start_y += 60

        reset_text = "Reset to Defaults"
        reset_surface = self.button_font.render(reset_text, True, (255, 255, 255))
        reset_rect = reset_surface.get_rect(center=(self.screen_width / 2, button_start_y))
        buttons['reset_defaults'] = reset_rect

        # Back to Main Menu button
        button_start_y += 60

        back_text = "Back to Main Menu"
        back_surface = self.button_font.render(back_text, True, (255, 255, 255))
        back_rect = back_surface.get_rect(center=(self.screen_width / 2, button_start_y))
        buttons['back'] = back_rect

        while running:
            self.game_screen.blit(background, background_rect)
            self.game_screen.blit(overlay, overlay_rect)

            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    self.handle_quit_event()
                if event.type == pygame.MOUSEBUTTONDOWN:
                    mouse_pos = event.pos
                    if buttons['fullscreen'].collidepoint(mouse_pos):
                        # Toggle fullscreen
                        self.is_fullscreen = not self.is_fullscreen
                        if self.is_fullscreen:
                            flags = pygame.FULLSCREEN | pygame.DOUBLEBUF | pygame.HWSURFACE
                        else:
                            flags = pygame.DOUBLEBUF | pygame.HWSURFACE
                        self.game_screen = pygame.display.set_mode((self.screen_width, self.screen_height), flags)
                    elif buttons['music_minus'].collidepoint(mouse_pos):
                        # Decrease music volume
                        self.music_volume = max(0.0, self.music_volume - 0.1)
                        pygame.mixer.music.set_volume(self.music_volume)
                    elif buttons['music_plus'].collidepoint(mouse_pos):
                        # Increase music volume
                        self.music_volume = min(1.0, self.music_volume + 0.1)
                        pygame.mixer.music.set_volume(self.music_volume)
                    elif buttons['effects_minus'].collidepoint(mouse_pos):
                        # Decrease effects volume
                        self.effects_volume = max(0.0, self.effects_volume - 0.1)
                    elif buttons['effects_plus'].collidepoint(mouse_pos):
                        # Increase effects volume
                        self.effects_volume = min(1.0, self.effects_volume + 0.1)
                    elif buttons['difficulty'].collidepoint(mouse_pos):
                        # Cycle difficulty
                        difficulties = ['Easy', 'Medium', 'Hard']
                        current_index = difficulties.index(self.difficulty)
                        current_index = (current_index + 1) % len(difficulties)
                        self.difficulty = difficulties[current_index]
                    elif buttons['reset_defaults'].collidepoint(mouse_pos):
                        # Reset to default settings
                        self.is_fullscreen = False
                        self.music_volume = 1.0
                        self.effects_volume = 1.0
                        self.difficulty = 'Medium'
                        pygame.mixer.music.set_volume(self.music_volume)
                        # Update screen mode
                        flags = pygame.DOUBLEBUF | pygame.HWSURFACE
                        self.game_screen = pygame.display.set_mode((self.screen_width, self.screen_height), flags)
                    elif buttons['back'].collidepoint(mouse_pos):
                        # Back to main menu
                        self.screen = 'main_menu'
                        running = False

            mouse_pos = pygame.mouse.get_pos()

            # Draw buttons and labels
            # Fullscreen button
            if buttons['fullscreen'].collidepoint(mouse_pos):
                fullscreen_surface = self.button_font.render(fullscreen_text, True, (0, 255, 0))
            else:
                fullscreen_surface = self.button_font.render(fullscreen_text, True, (255, 255, 255))
            self.game_screen.blit(fullscreen_surface, buttons['fullscreen'])

            # Music volume controls
            # Update music volume label
            music_label_surface = self.button_font.render(f"Music Volume: {int(self.music_volume * 100)}%", True, (255, 255, 255))
            self.game_screen.blit(music_label_surface, labels['music_label'])

            if buttons['music_minus'].collidepoint(mouse_pos):
                music_minus_surface = self.button_font.render("-", True, (0, 255, 0))
            else:
                music_minus_surface = self.button_font.render("-", True, (255, 255, 255))
            self.game_screen.blit(music_minus_surface, buttons['music_minus'])

            if buttons['music_plus'].collidepoint(mouse_pos):
                music_plus_surface = self.button_font.render("+", True, (0, 255, 0))
            else:
                music_plus_surface = self.button_font.render("+", True, (255, 255, 255))
            self.game_screen.blit(music_plus_surface, buttons['music_plus'])

            # Effects volume controls
            # Update effects volume label
            effects_label_surface = self.button_font.render(f"Effects Volume: {int(self.effects_volume * 100)}%", True, (255, 255, 255))
            self.game_screen.blit(effects_label_surface, labels['effects_label'])

            if buttons['effects_minus'].collidepoint(mouse_pos):
                effects_minus_surface = self.button_font.render("-", True, (0, 255, 0))
            else:
                effects_minus_surface = self.button_font.render("-", True, (255, 255, 255))
            self.game_screen.blit(effects_minus_surface, buttons['effects_minus'])

            if buttons['effects_plus'].collidepoint(mouse_pos):
                effects_plus_surface = self.button_font.render("+", True, (0, 255, 0))
            else:
                effects_plus_surface = self.button_font.render("+", True, (255, 255, 255))
            self.game_screen.blit(effects_plus_surface, buttons['effects_plus'])

            # Difficulty setting
            # Update difficulty label
            if buttons['difficulty'].collidepoint(mouse_pos):
                difficulty_surface = self.button_font.render(f"Difficulty: {self.difficulty}", True, (0, 255, 0))
            else:
                difficulty_surface = self.button_font.render(f"Difficulty: {self.difficulty}", True, (255, 255, 255))
            self.game_screen.blit(difficulty_surface, buttons['difficulty'])

            # Reset to Defaults button
            if buttons['reset_defaults'].collidepoint(mouse_pos):
                reset_surface = self.button_font.render(reset_text, True, (0, 255, 0))
            else:
                reset_surface = self.button_font.render(reset_text, True, (255, 255, 255))
            self.game_screen.blit(reset_surface, buttons['reset_defaults'])

            # Back to Main Menu button
            if buttons['back'].collidepoint(mouse_pos):
                back_surface = self.button_font.render(back_text, True, (0, 255, 0))
            else:
                back_surface = self.button_font.render(back_text, True, (255, 255, 255))
            self.game_screen.blit(back_surface, buttons['back'])

            self.update_screen()
            clock.tick(60)
    def death_screen(self):
        clock = pygame.time.Clock()

        while True:
            mx, my = pygame.mouse.get_pos()

            game_over_font = pygame.font.Font(None, 50)
            game_over_text = game_over_font.render("You Died.", True, (150, 0, 0))
            self.game_screen.fill((0, 0, 0))
            text_rect = game_over_text.get_rect(
                center=(self.screen_width / 2, self.screen_height / 2)
            )
            self.game_screen.blit(game_over_text, text_rect)

            respawn_font = pygame.font.Font(None, 30)
            respawn_color = (150, 0, 0)

            respawn_rect = pygame.Rect(
                self.screen_width / 2 - 60, self.screen_height / 2 + 100, 120, 40
            )

            if respawn_rect.collidepoint((mx, my)):
                respawn_color = (100, 0, 0)

            respawn_text = respawn_font.render("Main Menu", True, respawn_color)
            text_rect = respawn_text.get_rect(center=respawn_rect.center)
            self.game_screen.blit(respawn_text, text_rect)

            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    self.handle_quit_event()

                if event.type == pygame.MOUSEBUTTONDOWN:
                    if respawn_rect.collidepoint((mx, my)):
                        self.screen = "main_menu"
                        self.game = None
                        pygame.mixer.music.fadeout(2000)
                        self.music_fading = True
                        return

            self.update_screen()
            clock.tick(60)

    def update_screen(self):
        self.update_mouse()
        pygame.display.flip()

    def handle_quit_event(self):
        pygame.quit()
        quit()

    def update_mouse(self):
        mx, my = pygame.mouse.get_pos()
        self.game_screen.blit(self.cursor_img, (mx, my))
