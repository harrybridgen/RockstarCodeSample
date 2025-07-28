import random
import os

import math
import pickle
import pygame
import pytmx
import tempfile

from utilities.constants import START_MAP
from mechanics.chest import Chest
from mechanics.enemy import Archer, Dragon, Enemy
from graphics.lighting import LightSource
from mechanics.npc import Amrit, Bob, Cow, NPC
from graphics.particle import Particle, walkParticle, OrbitParticle
from graphics.particleEffect import ArrowExplosion, FireBallExplosion
from character.player import Player
from mechanics.portal import Portal
from mechanics.projectile import Arrow, FireBall, Meteor
from mechanics.quest import QuestListener
from utilities.resourcePath import resource_path
from items.gameObject import (
    EnchantedHerbObject,
    SignPostObject,
    PileLogsObject,
    RareGemObject,
    WorldMapObject,
    BucketObject,
)
from world.soundManager import SoundManager


class Map:
    def __init__(self, screen_width, temp_dir, quest_listener):
        self.temp_dir = temp_dir
        self.map_file = START_MAP
        self.map_data = pytmx.load_pygame(resource_path("source/tile/" + self.map_file))
        self.layers = {layer.name: layer for layer in self.map_data.layers}
        self.quest_listener = quest_listener

        self.sound_manager = SoundManager.getInstance()
        self.sound_manager.current_music = self.map_data.properties.get("music")
        pygame.mixer.music.load(
            resource_path("source/sound/" + self.sound_manager.current_music)
        )
        pygame.mixer.music.play(loops=-1, fade_ms=2000)

        self.screen_width = screen_width
        self.game_objects = []
        self.enemies = []
        self.npcs = []
        self.projectiles = []
        self.portals = []
        self.ground_particles = []
        self.above_ground_particles = []
        self.particles = []
        self.explosions = []
        self.collision_rects = []
        self.entity_collision_rects = []
        self.chests = []
        self.dead_npcs = []
        self.set_light_level()
        self.object_setup()
        self.spawn_chests()
        self.spawn_enemies()
        self.spawn_npcs()
        self.width = self.map_data.width * self.map_data.tilewidth
        self.height = self.map_data.height * self.map_data.tileheight
        self.tile_cache = {}
        self.load_tiles_into_cache("floor")
        self.load_tiles_into_cache("ground")
        self.load_tiles_into_cache("above_ground")
        self.is_interior = self.map_data.properties.get("interior", False)

    def set_light_level(self):
        self.light_level = int(self.map_data.properties.get("light_level", 10))

    def spawn_npcs(self):
        npc_objects = self.map_data.get_layer_by_name("npcs")
        for obj in npc_objects:
            try:
                npc_class = globals()[obj.name]
                npc = npc_class(obj.x, obj.y, obj.hp, obj.respawns, obj.respawn_time)
                self.add_npc(npc)
            except KeyError:
                print(f"Warning: Unknown npc type {obj.name}")

    def spawn_enemies(self):
        self.enemy_objects = self.map_data.get_layer_by_name("enemies")
        for obj in self.enemy_objects:
            try:
                enemy_class = globals()[obj.name]
                enemy = enemy_class(
                    obj.x, obj.y, obj.hp, obj.respawns, obj.respawn_time
                )
                self.add_enemy(enemy)
            except KeyError:
                print(f"Warning: Unknown enemy type {obj.name}")

    def spawn_chests(self):
        chest_objects = self.map_data.get_layer_by_name("chests")
        for obj in chest_objects:
            chest = Chest(
                obj.x,
                obj.y,
                obj.width,
                obj.height,
                obj.items,
                self.screen_width,
                obj.name,
            )
            self.add_chest(chest)

    def add_chest(self, chest):
        self.chests.append(chest)

    def add_enemy(self, enemy):
        self.entity_collision_rects.append(enemy.collision_rect)
        self.enemies.append(enemy)

    def add_particle(self, particle):
        self.particles.append(particle)

    def add_ground_particle(self, particle):
        self.ground_particles.append(particle)

    def add_projectile(self, projectile):
        self.projectiles.append(projectile)

    def add_explosion(self, explosion):
        self.explosions.append(explosion)

    def add_npc(self, npc):
        self.entity_collision_rects.append(npc.collision_rect)
        self.npcs.append(npc)

    def remove_npc(self, npc):
        if npc in self.npcs:
            self.npcs.remove(npc)

    def remove_enemy(self, enemy):
        if enemy in self.enemies:
            self.enemies.remove(enemy)

    def remove_projectile(self, projectile):
        if projectile in self.projectiles:
            self.projectiles.remove(projectile)

    def remove_particle(self, particle):
        if particle in self.particles:
            self.particles.remove(particle)

    def remove_below_particle(self, particle):
        if particle in self.below_particles:
            self.below_particles.remove(particle)

    def remove_explosion(self, explosion):
        if explosion in self.particles:
            self.explosions.remove(explosion)

    def update(self, player, camera, dt):
        self.update_enemies(player, dt)
        self.update_npcs(player, dt)
        self.update_game_objects(player, dt)
        self.update_portals(player, camera)
        self.update_projectiles(player, dt)
        self.update_explosions(dt)
        self.update_particles(player, dt)
        self.respawn_npcs()

    def update_portals(self, player, camera):
        for portal in self.portals:
            if (
                player.collision_rect.colliderect(portal.rect)
                and not player.in_dialogue
            ):
                if portal.has_quest_requirements(
                    player
                ) and portal.has_quest_complete_requirements(player):
                    self.change_map(
                        portal.map_file + ".tmx",
                        player,
                        camera,
                        portal.destination[0],
                        portal.destination[1],
                    )
                    break

    def update_game_objects(self, player, dt):
        for object in self.game_objects:
            if object.update(self.ground_particles):
                if hasattr(object, "collision_rect"):
                    self.collision_rects.remove(object.collision_rect)
                self.game_objects.remove(object)

    def respawn_npcs(self):
        for entity in self.dead_npcs:
            entity.respawn_counter -= 1
            if entity.respawn_counter <= 0:
                entity.dead = False
                entity.hp = entity.start_hp
                entity.rect.x = entity.start_x
                entity.rect.y = entity.start_y
                entity.collision_rect.midbottom = entity.rect.midbottom
                entity.respawn()
                entity.tint = (255, 255, 255)
                entity.current_animation = entity.standing_animation_south

                if isinstance(entity, NPC):
                    self.add_npc(entity)

                elif isinstance(entity, Enemy):
                    self.add_enemy(entity)

                self.dead_npcs.remove(entity)

    def update_npcs(self, player, dt):
        for npc in self.npcs:
            all_collision_rects = self.collision_rects + self.entity_collision_rects
            all_collision_rects = [
                rect for rect in all_collision_rects if rect != npc.collision_rect
            ]

            npc.update(
                dt,
                all_collision_rects,
                self.width,
                self.height,
                player.rect.midbottom,
            )
            if npc.moved:
                if random.random() < 0.3:
                    walk_particle = walkParticle(npc)
                    self.add_ground_particle(walk_particle)
            for projectile in self.projectiles:
                if npc.rect.colliderect(projectile.collision_rect) and isinstance(
                    projectile.owner, Player
                ):
                    if not npc.hit and projectile.can_hit:
                        npc.take_damage(projectile.damage)
                        self.remove_projectile(projectile)

            if npc.hp <= 0:
                self.entity_collision_rects.remove(npc.collision_rect)
                if npc.respawns:
                    self.dead_npcs.append(npc)
                self.remove_npc(npc)
                self.quest_listener.process_event(npc.__class__.__name__)

    def update_enemies(self, player, dt):
        for enemy in self.enemies:
            all_collision_rects = self.collision_rects + self.entity_collision_rects
            all_collision_rects = [
                rect for rect in all_collision_rects if rect != enemy.collision_rect
            ]
            if enemy.ai_move(
                all_collision_rects,
                self.width,
                self.height,
                dt,
                player.rect.centerx,
                player.rect.centery,
            ):
                if random.random() < 0.3:
                    walk_particle = walkParticle(enemy)
                    self.add_ground_particle(walk_particle)

            if enemy.attack(player, self.collision_rects):
                self.add_projectile(enemy.projectile)

            for projectile in self.projectiles:
                if enemy.rect.colliderect(projectile.collision_rect) and isinstance(
                    projectile.owner, Player
                ):
                    if not enemy.hit and projectile.can_hit:
                        enemy.take_damage(projectile.damage)
                        self.remove_projectile(projectile)

            if enemy.hp <= 0:
                self.entity_collision_rects.remove(enemy.collision_rect)
                if enemy.respawns:
                    self.dead_npcs.append(enemy)
                self.remove_enemy(enemy)
                self.quest_listener.process_event(enemy.__class__.__name__)

    def update_projectiles(self, player, dt):
        for projectile in self.projectiles:
            if (
                player.teleporting is False
                and player.rect.colliderect(projectile.collision_rect)
                and projectile.owner is not player
                and projectile.can_hit
            ):
                if not player.hit and not player.god:
                    player.hit_by_projectile(projectile.damage)
                self.remove_projectile(projectile)

            if projectile.update(
                self.collision_rects,
                self.width,
                self.height,
                dt,
                self.enemies + self.npcs + [player],
            ):
                if projectile.explosion is not None:
                    self.add_explosion(projectile.explosion)
                self.remove_projectile(projectile)

    def draw_projectiles(self, camera, surface):
        for projectile in self.projectiles:
            if projectile.above_ground is False:
                rect = projectile.rect
                self.add_particle(projectile.particle)
                if camera.rect.colliderect(rect):
                    surface.blit(
                        projectile.image,
                        (rect.x - camera.rect.x, rect.y - camera.rect.y),
                    )

    def draw_projectile_above_ground(self, camera, surface):
        for projectile in self.projectiles:
            if projectile.above_ground:
                rect = projectile.rect
                self.above_ground_particles.append(projectile.particle)
                if camera.rect.colliderect(rect):
                    surface.blit(
                        projectile.image,
                        (rect.x - camera.rect.x, rect.y - camera.rect.y),
                    )

    def update_particles(self, player, dt):
        for particle in self.above_ground_particles:
            if particle.update(dt, player.rect):
                self.above_ground_particles.remove(particle)

        for particle in self.particles:
            if particle.update(dt, player.rect):
                self.particles.remove(particle)

        for particle in self.ground_particles:
            if particle.update(dt, player.rect):
                self.ground_particles.remove(particle)

    def draw_particles(self, particle_list, camera, surface):
        for particle in particle_list:
            rect = particle.rect
            if camera.rect.colliderect(rect):
                offset_x, offset_y = rect.x - camera.rect.x, rect.y - camera.rect.y
                surface.blit(particle.image, (offset_x, offset_y))

    def update_explosions(self, dt):
        for explosion in self.explosions:
            if explosion.update(dt):
                self.remove_explosion(explosion)

    def draw_explosions(self, camera, surface):
        for explosion in self.explosions:
            for particle in explosion.particles:
                if camera.rect.colliderect(particle.rect):
                    offset_x, offset_y = (
                        particle.rect.x - camera.rect.x,
                        particle.rect.y - camera.rect.y,
                    )
                    surface.blit(particle.image, (offset_x, offset_y))

    def change_map(self, new_map_file, player, camera, teleport_x, teleport_y):
        self.save_state(self.map_file + ".pkl")

        self.map_file = new_map_file
        self.map_data = pytmx.load_pygame(resource_path("source/tile/" + new_map_file))
        self.layers = {layer.name: layer for layer in self.map_data.layers}
        self.sound_manager.load_new_music(self.map_data.properties.get("music"))

        self.width = self.map_data.width * self.map_data.tilewidth
        self.height = self.map_data.height * self.map_data.tileheight
        camera.change_map(self.width, self.height)
        player.teleport(teleport_x, teleport_y)
        camera.teleport_to_player(player.rect.center)
        self.set_light_level()
        self.tile_cache = {}
        self.load_tiles_into_cache("floor")
        self.load_tiles_into_cache("ground")
        self.load_tiles_into_cache("above_ground")
        self.projectiles.clear()
        self.particles.clear()
        self.ground_particles.clear()
        self.above_ground_particles.clear()
        self.explosions.clear()
        self.enemies.clear()
        self.chests.clear()
        self.npcs.clear()
        self.dead_npcs.clear()
        self.object_setup()
        self.entity_collision_rects.clear()
        self.entity_collision_rects.append(player.collision_rect)
        self.is_interior = self.map_data.properties.get("interior", False)
        self.dir = self.temp_dir.name + "/" + new_map_file + ".pkl"
        if not os.path.exists(self.dir) or os.path.getsize(self.dir) == 0:
            print(f"State file {self.dir} not found.")
            self.spawn_chests()
            self.spawn_enemies()
            self.spawn_npcs()
        else:
            self.load_state(self.dir)
            print(f"State loaded from {self.dir}")

    def object_setup(self):
        self.collision_rects = []
        self.portals = []
        self.game_objects = []

        for layer in self.map_data.visible_layers:
            if isinstance(layer, pytmx.TiledObjectGroup):
                if layer.name == "game_objects":
                    for obj in layer:
                        if obj.name is not None and obj.name != "EnchantedHerbObject":
                            object_class = globals()[obj.name]
                            object_instance = object_class(
                                obj.x, obj.y, obj.width, obj.height
                            )
                            self.game_objects.append(object_instance)
                            if hasattr(object_instance, "collision_rect"):
                                self.collision_rects.append(
                                    object_instance.collision_rect
                                )

                        elif obj.name == "EnchantedHerbObject":
                            x, y = self.random_spawn(obj.width, obj.height)
                            herb = EnchantedHerbObject(x, y, obj.width, obj.height)
                            self.game_objects.append(herb)
                            for _ in range(random.randint(10, 30)):
                                particle = OrbitParticle(
                                    herb.rect.centerx,
                                    herb.rect.centery,
                                    random.randint(1, 2),
                                    random.randint(2, 4),
                                    (
                                        random.randint(165, 255),
                                        0,
                                        random.randint(128, 255),
                                        random.randint(100, 200),
                                    ),
                                    random.randint(10, 25),
                                    random.uniform(-10, 10),
                                )
                                self.add_ground_particle(particle)

                if layer.name == "portal":
                    for obj in layer:
                        if obj.name is not None:
                            portal_rect = pygame.FRect(
                                obj.x, obj.y, obj.width, obj.height
                            )
                            portal_info = obj.name.split(",")
                            map_file = portal_info[0]
                            destination = tuple(int(i) for i in portal_info[1:])
                            quest_requirements = (
                                obj.properties.get("quest_requirements", "").split(",")
                                if "quest_requirements" in obj.properties
                                else []
                            )
                            quest_complete_requirements = (
                                obj.properties.get("quest_complete_requirements").split(
                                    ","
                                )
                                if "quest_complete_requirements" in obj.properties
                                else []
                            )
                            self.portals.append(
                                Portal(
                                    portal_rect,
                                    map_file,
                                    destination,
                                    quest_requirements,
                                    quest_complete_requirements,
                                )
                            )
                if layer.name == "collision":
                    for obj in layer:
                        collision_rect = pygame.FRect(
                            obj.x, obj.y, obj.width, obj.height
                        )
                        self.collision_rects.append(collision_rect)

    def load_tiles_into_cache(self, layer_name):
        tile_cache = self.tile_cache
        get_tile_image_by_gid = self.map_data.get_tile_image_by_gid
        layers = self.layers

        matching_layers = [
            layer for name, layer in layers.items() if name.startswith(layer_name)
        ]

        for layer in matching_layers:
            for row in layer.data:
                for gid in row:
                    if gid == 0:
                        continue

                    if gid not in tile_cache:
                        tile = get_tile_image_by_gid(gid).convert_alpha()
                        tile_cache[gid] = tile

    def draw_layer(self, layer_name, camera, surface):
        tile_width, tile_height = self.map_data.tilewidth, self.map_data.tileheight
        camera_x, camera_y, camera_width, camera_height = camera.rect
        start_x = max(0, int(camera_x // tile_width))
        end_x = min(
            self.map_data.width, int((camera_x + camera_width) // tile_width + 1)
        )
        start_y = max(0, int(camera_y // tile_height))
        end_y = min(
            self.map_data.height, int((camera_y + camera_height) // tile_height + 1)
        )

        blit = surface.blit
        tile_cache = self.tile_cache
        layers = self.layers

        matching_layers = [
            layer for name, layer in layers.items() if name.startswith(layer_name)
        ]

        for layer in matching_layers:
            layer_data = [row[start_x:end_x] for row in layer.data[start_y:end_y]]

            for y, row in enumerate(layer_data):
                world_y = (start_y + y) * tile_height - camera_y
                for x, gid in enumerate(row):
                    if gid == 0:
                        continue

                    world_x = (start_x + x) * tile_width - camera_x
                    blit(tile_cache[gid], (world_x, world_y))

    def save_state(self, state_filename):
        state_filename = os.path.join(self.temp_dir.name, state_filename)
        state = {
            "chests": [
                (
                    chest.rect.x,
                    chest.rect.y,
                    chest.rect.width,
                    chest.rect.height,
                    [item.class_name for item in chest.items],
                    chest.name,
                )
                for chest in self.chests
            ],
            "enemies": [
                (
                    type(enemy).__name__,
                    enemy.rect.x,
                    enemy.rect.y,
                    enemy.hp,
                    enemy.respawns,
                    enemy.respawn_time,
                )
                for enemy in self.enemies
            ],
            "npcs": [
                (
                    type(npc).__name__,
                    npc.rect.x,
                    npc.rect.y,
                    npc.hp,
                    npc.respawns,
                    npc.respawn_time,
                )
                for npc in self.npcs
            ],
            "dead_npcs": [
                (
                    type(npc).__name__,
                    npc.start_x,
                    npc.start_y,
                    npc.start_hp,
                    npc.respawns,
                    npc.respawn_time,
                    npc.respawn_counter,
                )
                for npc in self.dead_npcs
            ],
        }
        with open(state_filename, "wb") as f:
            pickle.dump(state, f)
        print(f"State saved in {state_filename}")

    def draw_projectile_shadows(self, camera_rect, game_screen):
        for projectile in self.projectiles:
            if projectile.has_shadow:
                self.draw_shadow(projectile, camera_rect, game_screen)

    def draw_shadow(self, entity, camera_rect, game_screen):
        shadow_pos_x = (
            entity.shadow_rect.centerx
            - camera_rect.x
            - entity.shadow_surface.get_width() // 2
        )
        shadow_pos_y = (
            entity.shadow_rect.centery
            - camera_rect.y
            - entity.shadow_surface.get_height() // 2
        )
        game_screen.blit(entity.shadow_surface, (shadow_pos_x, shadow_pos_y))

    def load_state(self, state_filename):
        state_filename = os.path.join(self.temp_dir.name, state_filename)
        if not os.path.exists(state_filename) or os.path.getsize(state_filename) == 0:
            print(f"File {state_filename} not found or empty.")
            return

        with open(state_filename, "rb") as f:
            state = pickle.load(f)

        self.load_enemies_state(state)
        self.load_chests_state(state)
        self.load_npcs_state(state)
        self.load_dead_npcs_state(state)

    def load_dead_npcs_state(self, state):
        self.dead_npcs = []
        for npc_info in state["dead_npcs"]:
            class_name, x, y, hp, respawns, respawn_time, respawn_counter = npc_info
            npc_class = globals()[class_name]
            npc = npc_class(x, y, hp, respawns, respawn_time)
            npc.respawn_counter = respawn_counter
            self.dead_npcs.append(npc)

    def load_enemies_state(self, state):
        self.enemies = []
        for enemy_info in state["enemies"]:
            class_name, x, y, hp, respawns, respawn_time = enemy_info
            enemy_class = globals()[class_name]
            enemy = enemy_class(x, y, hp, respawns, respawn_time)
            self.entity_collision_rects.append(enemy.collision_rect)
            self.enemies.append(enemy)

    def load_npcs_state(self, state):
        self.npcs = []
        for npc_info in state["npcs"]:
            class_name, x, y, hp, respawns, respawn_time = npc_info
            npc_class = globals()[class_name]
            npc = npc_class(x, y, hp, respawns, respawn_time)
            self.entity_collision_rects.append(npc.collision_rect)
            self.npcs.append(npc)

    def load_chests_state(self, state):
        self.chests = []
        for chest_info in state["chests"]:
            x, y, width, height, items, name = chest_info
            items_string = ",".join(items)
            chest = Chest(x, y, width, height, items_string, self.screen_width, name)
            self.chests.append(chest)

    def random_spawn(self, obj_width, obj_height):
        while True:
            x = random.randint(
                0, int(self.map_data.width * self.map_data.tilewidth - obj_width)
            )
            y = random.randint(
                0, int(self.map_data.height * self.map_data.tileheight - obj_height)
            )

            spawn_rect = pygame.Rect(x, y, obj_width, obj_height)

            if any(
                spawn_rect.colliderect(rect)
                for rect in self.collision_rects
                + self.entity_collision_rects
                + [chest.rect for chest in self.chests]
                + [npc.collision_rect for npc in self.npcs]
                + [game_object.rect for game_object in self.game_objects]
            ):
                continue

            if self.above_ground_tile_at(spawn_rect):
                continue

            return x, y

    def above_ground_tile_at(self, spawn_rect):
        layers = self.layers

        matching_layers = [
            layer for name, layer in layers.items() if name.startswith("above_ground")
        ]

        for layer in matching_layers:
            for y, row in enumerate(layer.data):
                for x, gid in enumerate(row):
                    if gid == 0:
                        continue

                    tile_left = x * self.map_data.tilewidth
                    tile_top = y * self.map_data.tileheight
                    tile_rect = pygame.Rect(
                        tile_left,
                        tile_top,
                        self.map_data.tilewidth,
                        self.map_data.tileheight,
                    )

                    if spawn_rect.colliderect(tile_rect):
                        return True

        return False
