import random
import pygame
from utilities.resourcePath import resource_path
import pytmx
from items.item import EnchantedHerb, Logs, RareGem, Bucket
from graphics.particle import OrbitParticle
from graphics.animation import Animation
from graphics.spritesheet import Spritesheet


class GameObject:
    def __init__(self, x, y, size_x, size_y):
        self.x = x
        self.y = y
        self.size_x = size_x
        self.size_y = size_y
        self.rect = pygame.Rect(0, 0, size_x, size_y)
        self.rect.midbottom = (self.x + size_x // 2, self.y + size_y)

    def interact(self):
        raise NotImplementedError

    def update(self):
        raise NotImplementedError

    def draw(self, screen, camera):
        if camera.rect.colliderect(self.rect):
            offset_x, offset_y = (
                self.rect.midbottom[0] - camera.rect.x,
                self.rect.midbottom[1] - camera.rect.y,
            )

            draw_x = offset_x - self.image.get_width() // 2
            draw_y = offset_y - self.image.get_height()

            screen.blit(self.image, (draw_x, draw_y))


class PileLogsObject(GameObject):
    def __init__(self, x, y, size_x, size_y):
        super().__init__(x, y, size_x, size_y)
        self.image = pygame.image.load(
            resource_path("source/img/logs_object.png")
        ).convert_alpha()
        self.rect.width = self.image.get_width()
        self.rect.height = self.image.get_height()
        self.rect.midbottom = (self.x + self.rect.width // 2, self.y + self.rect.height)
        self.collision_rect = pygame.Rect(
            0, 0, self.rect.width * 0.8, self.rect.height * 0.6
        )
        self.collision_rect.midbottom = (self.rect.midbottom[0], self.rect.midbottom[1])
        self.despawn = False

    def interact(self, player, game_screen):
        self.despawn = True
        if player.inventory.add_item(Logs()):
            return True
        return False

    def update(self, particle_list):
        if self.despawn:
            return True


class BucketObject(GameObject):
    def __init__(self, x, y, size_x, size_y):
        super().__init__(x, y, size_x, size_y)
        self.image = pygame.image.load(
            resource_path("source/img/bucket_object.png")
        ).convert_alpha()
        self.rect.width = self.image.get_width()
        self.rect.height = self.image.get_height()
        self.rect.midbottom = (self.x + self.rect.width // 2, self.y + self.rect.height)
        self.collision_rect = pygame.Rect(
            0, 0, self.rect.width * 0.8, self.rect.height * 0.6
        )
        self.collision_rect.midbottom = (self.rect.midbottom[0], self.rect.midbottom[1])
        self.despawn = False

    def interact(self, player, game_screen):
        self.despawn = True
        if player.inventory.add_item(Bucket()):
            return True
        return False

    def update(self, particle_list):
        if self.despawn:
            return True


class RareGemObject(GameObject):
    def __init__(self, x, y, size_x, size_y):
        super().__init__(x, y, size_x, size_y)
        self.spritesheet = Spritesheet("source/img/rare_gem_object.png")
        self.animation = Animation(self.spritesheet, 7, 0, 0, 22, 15)
        self.current_animation = self.animation
        self.image = self.current_animation.get_current_image()
        self.size_x = self.image.get_width() * 2
        self.size_y = self.image.get_height() * 2
        self.image = pygame.transform.scale(self.image, (self.size_x, self.size_y))
        self.current_animation.current_frame = random.randint(0, 5)
        self.current_animation.frame_rate = 10000
        self.rect.width = self.image.get_width()
        self.rect.height = self.image.get_height()
        self.rect.midbottom = (self.x + self.rect.width // 2, self.y + self.rect.height)
        self.collision_rect = pygame.Rect(
            0, 0, self.rect.width * 0.8, self.rect.height * 0.6
        )
        self.collision_rect.midbottom = (self.rect.midbottom[0], self.rect.midbottom[1])
        self.despawn = False
        self.play_animation = False
        self.animation_timer = 0
        self.animation_cooldown = 0

    def interact(self, player, game_screen):
        self.despawn = True
        if player.inventory.add_item(RareGem()):
            return True
        return False

    def update(self, particle_list):
        if self.despawn:
            return True

        self.animation_timer += 1

        if self.animation_timer > self.animation_cooldown:
            self.play_animation = True
            self.animation_timer = 0
            self.animation_cooldown = random.randint(100, 300)

        if self.play_animation:
            self.current_animation.update(1, 0)
            self.image = self.current_animation.get_current_image()
            if self.current_animation.is_finished():
                self.play_animation = False
                self.current_animation.reset()
        else:
            self.image = self.animation.frames[0]

        self.image = pygame.transform.scale(self.image, (self.size_x, self.size_y))


class EnchantedHerbObject(GameObject):
    def __init__(self, x, y, size_x, size_y):
        super().__init__(x, y, size_x, size_y)
        self.spritesheet = Spritesheet("source/img/enchanted_herb_object.png")
        self.animation = Animation(self.spritesheet, 5, 0, 0, 22, 15)
        self.current_animation = self.animation
        self.image = self.current_animation.get_current_image()
        self.size_x = self.image.get_width() * 1.5
        self.size_y = self.image.get_height() * 1.5
        self.image = pygame.transform.scale(self.image, (self.size_x, self.size_y))
        self.current_animation.current_frame = random.randint(0, 5)
        self.current_animation.frame_rate = 50000

        self.rect.width = self.image.get_width()
        self.rect.height = self.image.get_height()
        self.rect.midbottom = (self.x + self.rect.width // 2, self.y + self.rect.height)
        self.despawn = False

    def interact(self, player, game_screen):
        self.despawn = True
        player.inventory.add_item(EnchantedHerb())

    def draw(self, screen, camera):
        return super().draw(screen, camera)

    def update(self, particle_list):
        if self.despawn:
            particles_to_remove = [
                particle
                for particle in particle_list
                if isinstance(particle, OrbitParticle)
            ]
            for particle in particles_to_remove:
                particle_list.remove(particle)
            return True
        self.current_animation.update(1, 0)
        self.image = self.current_animation.get_current_image()
        self.image = pygame.transform.scale(self.image, (self.size_x, self.size_y))

    def valid_spawn_location(rect, collision_rects, map_data):
        matching_layers = [
            layer
            for name, layer in map_data.layers.items()
            if name.startswith("above_ground")
        ]
        for col_rect in collision_rects:
            if rect.colliderect(col_rect):
                return False

        for layer in matching_layers:
            if isinstance(layer, pytmx.TiledTileLayer):
                for x, y, gid in layer:
                    tile = map_data.get_tile_image_by_gid(gid)
                    if tile:
                        tile_rect = pygame.Rect(
                            x * map_data.tilewidth,
                            y * map_data.tileheight,
                            map_data.tilewidth,
                            map_data.tileheight,
                        )
                        if rect.colliderect(tile_rect):
                            return False

        return True


class SignPostObject(GameObject):
    def __init__(self, x, y, size_x, size_y):
        super().__init__(x, y, size_x, size_y)
        self.image = pygame.image.load(
            resource_path("source/img/sign_post_object.png")
        ).convert_alpha()

        self.rect.width = self.image.get_width()
        self.rect.height = self.image.get_height()

        self.collision_rect = pygame.Rect(
            0, 0, self.rect.width * 0.5, self.rect.height * 0.2
        )
        self.rect.midbottom = (x + self.rect.width // 2, y + self.rect.height)
        self.collision_rect.midbottom = (self.rect.midbottom[0], self.rect.midbottom[1])
        self.wood_texture = pygame.image.load(
            resource_path("source/img/wood.png")
        ).convert_alpha()
        self.ui_width, self.ui_height = 350, 250
        self.wood_texture = pygame.transform.scale(
            self.wood_texture, (self.ui_width, self.ui_height)
        )
        self.compass = pygame.image.load(
            resource_path("source/img/compass.png")
        ).convert_alpha()

        self.UI_open = False

    def update(self, particle_list):
        return False

    def interact(self, player, game_screen):
        pass

    def display_message(self, game_screen):
        rect_surface = pygame.Surface((self.ui_width, self.ui_height), pygame.SRCALPHA)

        rect_surface.blit(self.wood_texture, (0, 0))

        image_x = (self.ui_width - self.compass.get_width()) // 2
        image_y = (self.ui_height - self.compass.get_height()) // 2

        rect_surface.blit(self.compass, (image_x, image_y))

        self.rect_x = (game_screen.get_width() - self.ui_width) // 2
        self.rect_y = (game_screen.get_height() - self.ui_height) // 2

        game_screen.blit(rect_surface, (self.rect_x, self.rect_y))


class WorldMapObject(GameObject):
    def __init__(self, x, y, size_x, size_y):
        super().__init__(x, y, size_x, size_y)

        self.ui_width, self.ui_height = 603, 423
        self.border_size = 15

        self.image = pygame.image.load(
            resource_path("source/img/world_map_object.png")
        ).convert_alpha()

        self.rect.width = self.image.get_width()
        self.rect.height = self.image.get_height()
        self.rect.midbottom = (x + self.rect.width // 2, y + self.rect.height)

        self.world_map = pygame.image.load(
            resource_path("source/img/world_map.png")
        ).convert_alpha()
        self.world_map = pygame.transform.scale(
            self.world_map,
            (self.ui_width - self.border_size, self.ui_height - self.border_size),
        )

        self.wood_texture = pygame.image.load(
            resource_path("source/img/wood.png")
        ).convert_alpha()
        self.wood_texture = pygame.transform.scale(
            self.wood_texture,
            (self.ui_width + self.border_size, self.ui_height + self.border_size),
        )
        self.UI_open = False

    def update(self, particle_list):
        return False

    def interact(self, player, game_screen):
        pass

    def display_message(self, game_screen):
        surface_width = self.ui_width + self.border_size
        surface_height = self.ui_height + self.border_size

        surface = pygame.Surface((surface_width, surface_height), pygame.SRCALPHA)

        surface.blit(self.wood_texture, (0, 0))

        map_x = (surface_width - (self.ui_width - self.border_size)) / 2
        map_y = (surface_height - (self.ui_height - self.border_size)) / 2
        surface.blit(self.world_map, (map_x, map_y))

        self.rect_x = (game_screen.get_width() - surface_width) / 2
        self.rect_y = (game_screen.get_height() - surface_height) / 2

        game_screen.blit(surface, (self.rect_x, self.rect_y))
