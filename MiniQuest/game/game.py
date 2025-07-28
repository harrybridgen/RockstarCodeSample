import os
import sys
from sys import exit

import pygame
from pygame.math import Vector2
import random
import tempfile
import math

from world.camera import Camera
from utilities.constants import RED, GREEN, BLUE, PADDING
from graphics.healthBar import HealthBar
from mechanics.enemy import *
from items.equipment import *
from world.map import Map
from world.soundManager import SoundManager
from mechanics.npc import NPC
from graphics.particle import walkParticle
from mechanics.projectile import *
from character.player import Player
from mechanics.quest import QuestListener
from utilities.resourcePath import resource_path
from items.gameObject import GameObject
from utilities.constants import PLAYER_START_X, PLAYER_START_Y
from items.item import *
from utilities.console import GameConsole
from world.daynightcycle import DayNightCycle


class Game:
    def __init__(self, screen_width, screen_height, game_screen):
        self.sound_manager = SoundManager()
        self.console = GameConsole()
        self.temp_dir = tempfile.TemporaryDirectory(None, "miniquest-")
        self.screen_width = screen_width
        self.screen_height = screen_height
        self.game_screen = game_screen
        self.last_shot = 0
        self.game_over = False
        self.PLAYER_TELEPORT_ARTEFACT = pygame.USEREVENT + 1
        self.clock_object = pygame.time.Clock()
        self.dt = 0
        self.day_night_cycle = DayNightCycle(initial_time=7.0)
        self.death_counter = 2.0
        self.debug_mode = False
        self.player = Player(PLAYER_START_X, PLAYER_START_Y, "Player")
        self.player.equip_item(LeatherPants())
        self.quest_listener = QuestListener(self.player)
        self.map = Map(self.screen_width, self.temp_dir, self.quest_listener)
        self.camera = Camera(
            self.player.rect.center,
            screen_width,
            screen_height,
            self.map.width,
            self.map.height,
        )
        self.darknessdict = {}
        self.store_overlay(self.camera.rect.width, self.camera.rect.height)

        self.health_bar = HealthBar(self.player, 5, 5)

        self.map.entity_collision_rects.append(self.player.collision_rect)

        self.darkness_overlay = pygame.Surface(
            (self.camera.rect.width, self.camera.rect.height)
        )
        self.cursor_img = pygame.image.load(
            resource_path("source/img/cursor.png")
        ).convert_alpha()
        self.time_of_day = 7

    def run(self):
        while not self.game_over:
            self.update()
            self.draw()
            pygame.display.flip()

    def update(self):
        self.dt = self.clock_object.tick(64) / 1000
        self.day_night_cycle.update(self.dt)
        self.handle_events()
        self.map.update(self.player, self.camera, self.dt)
        self.update_player()
        self.camera.update(self.dt, self.player.rect.center)
        self.sound_manager.update()
        self.check_dying_player()


    def draw(self):
        self.game_screen.fill((48, 44, 45, 255))

        self.map.draw_layer("floor", self.camera, self.game_screen)
        self.map.draw_layer("ground", self.camera, self.game_screen)

        self.map.draw_projectile_shadows(self.camera.rect, self.game_screen)

        self.map.draw_particles(
            self.map.ground_particles, self.camera, self.game_screen
        )

        self.map.draw_projectiles(self.camera, self.game_screen)
        self.map.draw_explosions(self.camera, self.game_screen)
        self.draw_dynamic_objects()
        self.map.draw_particles(self.map.particles, self.camera, self.game_screen)
        self.map.draw_layer("above_ground", self.camera, self.game_screen)
        self.map.draw_particles(
            self.map.above_ground_particles, self.camera, self.game_screen
        )
        self.map.draw_projectile_above_ground(self.camera, self.game_screen)
        alpha = int(self.day_night_cycle.get_alpha(
            is_interior=self.map.is_interior,
            map_light_level=self.map.light_level,
            player_dying=self.player.dying,
            death_counter=self.death_counter
        ))
        self.game_screen.blit(self.darknessdict[alpha], (0, 0))
        self.draw_ui()

    def draw_ui(self):
        if self.player.dying:
            return

        self.draw_game_object_ui()
        self.draw_inventory_ui()
        self.draw_chest_ui()
        self.draw_npc_speech_box()
        self.health_bar.draw(self.game_screen)
        self.display_fps()
        self.display_time()

        if self.debug_mode:
            self.draw_debug()

        if self.console.active:
            self.console.draw(self.game_screen, self.screen_height, self.screen_width)
            
        self.draw_mouse()


    def toggle_debug(self):
        self.debug_mode = not self.debug_mode


    def draw_debug(self):
        self.debug_collision()
        self.debug_entitiy()
        self.debug_projectiles()
        self.debug_quests()
        self.debug_map_player()
        self.debug_entities_and_particles()
        self.debug_projectile()

    def debug_projectiles(self):
        for projectile in self.map.projectiles:
            rect = projectile.rect
            collision_rect = projectile.collision_rect
            relative_pos_rect = Vector2(rect.x, rect.y) - self.camera.rect.topleft
            relative_pos_collision_rect = Vector2(
                collision_rect.x, collision_rect.y
            ) - (self.camera.rect.topleft)
            rect = pygame.FRect(relative_pos_rect, rect.size)
            collision_rect = pygame.FRect(
                relative_pos_collision_rect, collision_rect.size
            )
            pygame.draw.rect(self.game_screen, BLUE, rect, 1)
            pygame.draw.rect(self.game_screen, GREEN, collision_rect, 1)

    def debug_entities_and_particles(self):
        enemy_count_string = f"Enemy Count: {len(self.map.enemies)}"
        enemy_count_text = pygame.font.SysFont("Arial", 20).render(
            enemy_count_string, True, pygame.Color("white")
        )
        self.game_screen.blit(enemy_count_text, (50, 240))

        npc_count_string = f"NPC Count: {len(self.map.npcs)}"
        npc_count_text = pygame.font.SysFont("Arial", 20).render(
            npc_count_string, True, pygame.Color("white")
        )
        self.game_screen.blit(npc_count_text, (50, 280))
        explosion_particles = 0
        for explosion in self.map.explosions:
            explosion_particles += len(explosion.particles)

        particle_count_string = f"Particle Count: {len(self.map.particles) + len(self.map.ground_particles) + len(self.map.above_ground_particles) + explosion_particles}"
        particle_count_text = pygame.font.SysFont("Arial", 20).render(
            particle_count_string, True, pygame.Color("white")
        )
        self.game_screen.blit(particle_count_text, (50, 320))

    def debug_quests(self):
        quest_string = "Active Quests: "
        for quest in self.player.quest_log.quests:
            quest_string += f"{quest.name}, "
        quest_string += " | Completed Quests: "
        for quest in self.player.quest_log.completed_quests:
            quest_string += f"{quest.name}, "

        quest_text = pygame.font.SysFont("Arial", 20).render(
            quest_string, True, pygame.Color("white")
        )
        self.game_screen.blit(quest_text, (50, 120))

    def debug_map_player(self):
        map_string = f"Current Map: {self.map.map_file}"
        map_text = pygame.font.SysFont("Arial", 20).render(
            map_string, True, pygame.Color("white")
        )
        self.game_screen.blit(map_text, (50, 160))

        player_pos_string = f"Player Position: {round(self.player.rect.x,2)}, {round(self.player.rect.y,2)}"
        player_pos_text = pygame.font.SysFont("Arial", 20).render(
            player_pos_string, True, pygame.Color("white")
        )
        self.game_screen.blit(player_pos_text, (50, 200))

    def debug_collision(self):
        for rect in self.map.collision_rects:
            relative_pos = Vector2(rect.x, rect.y) - self.camera.rect.topleft
            rect = pygame.FRect(relative_pos, rect.size)
            pygame.draw.rect(self.game_screen, RED, rect, 1)

    def debug_entitiy(self):
        for entitiy in self.map.npcs + self.map.enemies + [self.player]:
            rect = entitiy.rect
            relative_pos = Vector2(rect.x, rect.y) - self.camera.rect.topleft
            rect = pygame.FRect(relative_pos, rect.size)
            pygame.draw.rect(self.game_screen, BLUE, rect, 1)

            collision_rect = entitiy.collision_rect
            relative_pos = (
                Vector2(collision_rect.x, collision_rect.y) - self.camera.rect.topleft
            )
            collision_rect = pygame.Rect(relative_pos, collision_rect.size)
            pygame.draw.rect(self.game_screen, GREEN, collision_rect, 1)

    def display_fps(self):
        if not self.player.dying:
            fps = str(int(self.clock_object.get_fps()))
            fps_text = pygame.font.SysFont("Arial", 20).render(
                fps, True, pygame.Color("white")
            )
            self.game_screen.blit(fps_text, (50, 50))

    def display_time(self):
        if not self.player.dying:
            time_string = self.day_night_cycle.get_time_string()
            time_text = pygame.font.SysFont("Arial", 20).render(
                time_string, True, pygame.Color("white")
            )
            self.game_screen.blit(time_text, (50, 70))

            time_of_day_string = self.day_night_cycle.get_time_period()
            time_of_day_text = pygame.font.SysFont("Arial", 20).render(
                time_of_day_string, True, pygame.Color("white")
            )
            self.game_screen.blit(time_of_day_text, (50, 90))

    def debug_projectile(self):
        projectile_count_string = f"Projectile Count: {len(self.map.projectiles)}"
        projectile_count_text = pygame.font.SysFont("Arial", 20).render(
            projectile_count_string, True, pygame.Color("white")
        )
        self.game_screen.blit(projectile_count_text, (50, 360))

    def update_player(self):
        if not self.player.dying:
            if not self.player.in_quest_ui and not self.console.active:
                collision_rects = (
                    self.map.collision_rects + self.map.entity_collision_rects
                )
                collision_rects = [
                    rect
                    for rect in collision_rects
                    if rect != self.player.collision_rect
                ]
                if self.player.movement(
                    collision_rects,
                    self.map.width,
                    self.map.height,
                    self.dt,
                ):
                    if random.random() < 0.4 and self.player.boost_active:
                        walk_particle = walkParticle(self.player)
                        self.map.add_ground_particle(walk_particle)
        self.player.update(self.dt)

    def check_dying_player(self):
        if self.player.dying:
            if self.day_night_cycle.initial_light_level is None:
                self.day_night_cycle.initial_light_level = self.map.light_level

            self.death_counter -= self.dt

            light_decrease_rate = self.day_night_cycle.initial_light_level / 2.0
            self.map.light_level -= light_decrease_rate * self.dt

            if self.death_counter < 0:
                self.delete_save_files()
                self.sound_manager.reset_instance()
                self.game_over = True


    def draw_dynamic_objects(self):
        entities_to_sort = (
            self.map.enemies + self.map.npcs + [self.player] + self.map.game_objects
        )
        sorted_entities = sorted(
            entities_to_sort, key=lambda entity: entity.rect.midbottom[1]
        )
        for entity in sorted_entities:
            entity.draw(self.game_screen, self.camera)

    def draw_game_object_ui(self):
        for game_object in self.map.game_objects:
            if self.player.rect.colliderect(game_object.rect) and hasattr(
                game_object, "UI_open"
            ):
                if game_object.UI_open is True:
                    game_object.display_message(self.game_screen)
                    if self.player.inventory_open:
                        self.player.inventory_open = False
            else:
                game_object.UI_open = False

    def draw_inventory_ui(self):
        if self.player.inventory_open:
            self.player.inventory.draw_inventory(self.game_screen)
            self.player.inventory.draw_equipment(self.game_screen, self.player)

    def draw_chest_ui(self):
        for chest in self.map.chests:
            if self.player.current_chest == chest and self.player.rect.colliderect(
                chest.rect
            ):
                chest.draw_inventory(self.game_screen)
            else:
                if self.player.current_chest == chest:
                    self.player.current_chest = None

    def draw_npc_speech_box(self):
        for npc in self.map.npcs:
            if npc.current_quest_ui is not None:
                npc.current_quest_ui.draw(self.game_screen)
                self.player.in_quest_ui = True

                if npc.speech_box:
                    npc.speech_box = None
                    self.player.in_dialogue = False
            else:
                self.player.in_quest_ui = False

            if npc.speech_box:
                npc.speech_box.update()

                if not self.player.rect.colliderect(npc.rect):
                    npc.speech_box = None
                    self.player.in_dialogue = False
                else:
                    npc.speech_box.draw(self.game_screen)

    def handle_events(self):
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                self.delete_save_files()
                pygame.quit()
                exit()

            elif event.type == pygame.KEYDOWN:
                self.handle_keydown_event(event)
            elif event.type == pygame.MOUSEBUTTONDOWN:
                self.handle_mouse_button_down_event(event)
            elif event.type == pygame.MOUSEMOTION:
                self.handle_mouse_motion_event()
            if event.type == self.PLAYER_TELEPORT_ARTEFACT:
                self.handle_teleport_artefact()
            self.player.inventory.handle_event(event)
            self.console.update(event, self.camera, self.map, self.player, self)

    def handle_mouse_motion_event(self):
        mouse_position = pygame.mouse.get_pos()
        for npc in self.map.npcs:
            if npc.speech_box:
                npc.speech_box.handle_mouse_movement(mouse_position)

    def handle_teleport_artefact(self):
        if self.player.teleporting:
            self.player.canattack = True
            self.player.canmove = True
            self.player.teleporting = False
            self.player.targetable = True
            self.player.worn_equipment["Artefact"].add_smoke_effect(
                self.player.rect, self.map
            )

    def delete_save_files(self):
        for file in os.listdir(self.temp_dir.name):
            if file.endswith(".pkl"):
                os.remove(os.path.join(self.temp_dir.name, file))
                print("Deleted save file: " + self.temp_dir.name + "/" + file)

    def handle_mouse_button_down_event(self, event):
        if self.player.in_quest_ui:
            for npc in self.map.npcs:
                if npc.current_quest_ui is not None:
                    action = npc.current_quest_ui.check_button_press(event)
                    if action == "reject" or action == "close":
                        npc.current_quest_ui = None
                        self.player.in_quest_ui = False

                    if action == "accept":
                        self.player.quest_log.add_quest(
                            npc.current_quest_ui.get_quest()
                        )
                        npc.current_quest_ui = None
                        self.player.in_quest_ui = False

            return
        if event.button not in [1, 3]:
            pass
        else:
            if not self.player.dead:
                if event.button == 1:
                    if not self.player.in_dialogue:
                        self.handle_player_attack(event)
                elif event.button == 3:
                    self.handle_artefact_activation(event)
            for npc in self.map.npcs:
                if npc.speech_box:
                    npc.speech_box.handle_click(event.pos)

            self.handle_inventory_click(event)
            self.handle_equipment_click(event)
            self.handle_chest_click(event)

    def handle_artefact_activation(self, event):
        if (
            self.player.worn_equipment["Artefact"] is not None
            and self.player.current_chest is None
        ):
            mouse_x, mouse_y = pygame.mouse.get_pos()
            mouse_x += self.camera.rect.x
            mouse_y += self.camera.rect.y
            all_collision_rects = (
                self.map.collision_rects + self.map.entity_collision_rects
            )
            all_collision_rects = [
                rect
                for rect in all_collision_rects
                if rect != self.player.collision_rect
            ]
            self.player.worn_equipment["Artefact"].activate_effect(
                self.player, mouse_x, mouse_y, all_collision_rects, self.map
            )

    def handle_player_attack(self, event):
        current_time = pygame.time.get_ticks()
        mouse_x, mouse_y = pygame.mouse.get_pos()
        inv_rect = pygame.Rect(
            self.player.inventory.inv_pos_x,
            self.player.inventory.inv_pos_y,
            self.player.inventory.inv_width + 2 * PADDING,
            self.player.inventory.inv_height + 2 * PADDING,
        )
        equipment_rect = pygame.Rect(
            self.player.inventory.equip_inv_pos_x,
            self.player.inventory.equip_inv_pos_y,
            self.player.inventory.equip_inv_width + 2 * PADDING,
            self.player.inventory.equip_inv_height + 2 * PADDING,
        )
        if (
            (
                not inv_rect.collidepoint(mouse_x, mouse_y)
                or not self.player.inventory_open
            )
            and (
                not equipment_rect.collidepoint(mouse_x, mouse_y)
                or not self.player.inventory_open
            )
            and self.player.worn_equipment["Weapon"] is not None
            and current_time - self.last_shot
            >= self.player.worn_equipment["Weapon"].cooldown
            and self.player.canattack
            and not self.player.in_dialogue
            and not self.player.boost_active
            and self.player.current_chest is None
        ):
            mouse_x += self.camera.rect.x
            mouse_y += self.camera.rect.y
            self.map.add_projectile(
                self.player.worn_equipment["Weapon"].attack(
                    self.player, mouse_x, mouse_y, self.camera.rect
                )
            )
            self.last_shot = current_time

    def handle_inventory_click(self, event):
        if not self.player.inventory_open:
            return

        click_pos = event.pos
        inv_pos_x, inv_pos_y = (0, 0)

        for i, item_rect in enumerate(self.player.inventory.get_inventory_rects()):
            item_rect.move_ip(inv_pos_x, inv_pos_y)
            if item_rect.collidepoint(click_pos):
                item = self.player.inventory.items[i]
                if self.player.current_chest:
                    self.player.inventory.remove_item(item)
                    self.player.current_chest.add_item(item)
                else:
                    if issubclass(item.__class__, Equipment):
                        if self.player.worn_equipment[item.equipment_slot]:
                            self.player.inventory.items.insert(
                                i, self.player.worn_equipment[item.equipment_slot]
                            )
                            self.player.unequip_item(item.equipment_slot)
                        self.player.equip_item(item)
                        self.player.inventory.remove_item(item)

    def handle_equipment_click(self, event):
        if not self.player.inventory_open:
            return

        click_pos = event.pos

        for i, item_rect in enumerate(self.player.inventory.get_equipment_rects()):
            if item_rect.collidepoint(click_pos):
                slot, item = list(self.player.worn_equipment.items())[i]
                if item:
                    self.player.unequip_item(slot)
                    self.player.inventory.add_item(item)

    def handle_chest_click(self, event):
        click_pos = event.pos

        if self.player.current_chest:
            for i, item_rect in enumerate(self.player.current_chest.get_item_rects()):
                if item_rect.collidepoint(click_pos):
                    item = self.player.current_chest.items[i]
                    self.player.current_chest.remove_item(item)
                    self.player.inventory.add_item(item)

    def handle_keydown_event(self, event):
        if not self.console.active:
            if self.player.in_quest_ui:
                return
            if event.key == pygame.K_TAB:
                self.player.toggle_inventory()
            elif event.key == pygame.K_e:
                self.handle_keydown_e()
            

    def handle_keydown_e(self):
        for chest in self.map.chests:
            if self.player.rect.colliderect(chest.rect):
                if self.player.current_chest is None:
                    self.player.open_chest(chest)
                else:
                    self.player.close_chest()

        for npc in self.map.npcs:
            if self.player.rect.colliderect(npc.rect):
                if npc.speech_box is None:
                    npc.interact(self.screen_width, self.screen_height, self.player)
                elif npc.speech_box:
                    npc.speech_box = None
                    self.player.in_dialogue = False

        for game_object in self.map.game_objects:
            if self.player.rect.colliderect(game_object.rect):
                game_object.interact(self.player, self.game_screen)

                if hasattr(game_object, "UI_open"):
                    if game_object.UI_open is False:
                        game_object.UI_open = True
                    elif game_object.UI_open is True:
                        game_object.UI_open = False
            else:
                if hasattr(game_object, "UI_open"):
                    game_object.UI_open = False

    def draw_mouse(self):
        mx, my = pygame.mouse.get_pos()
        self.game_screen.blit(self.cursor_img, (mx, my))

    def store_overlay(self, width, height):
        self.darknessdict = {}

        for i in range(0, 256):
            overlay = pygame.Surface((width, height), pygame.SRCALPHA)
            overlay.fill((0, 0, 0, i))
            self.darknessdict[i] = overlay

