import pygame
import random

from graphics.animation import Animation
from graphics.particle import TeleportParticle, HealingParticle
from mechanics.projectile import Arrow, FireBall, Meteor
from utilities.resourcePath import resource_path
from graphics.spritesheet import Spritesheet
from world.soundManager import SoundManager


class Equipment:
    def __init__(self, spritesheet):
        self.icon = pygame.image.load(resource_path("source/img/erroricon.png"))
        self.walk_animation_north = Animation(spritesheet, 8, 64, 512, 64, 64)
        self.walk_animation_east = Animation(spritesheet, 8, 64, 704, 64, 64)
        self.walk_animation_south = Animation(spritesheet, 8, 64, 640, 64, 64)
        self.walk_animation_west = Animation(spritesheet, 8, 64, 576, 64, 64)

        self.stand_animation_north = Animation(spritesheet, 1, 0, 512, 64, 64)
        self.stand_animation_east = Animation(spritesheet, 1, 0, 704, 64, 64)
        self.stand_animation_south = Animation(spritesheet, 1, 0, 640, 64, 64)
        self.stand_animation_west = Animation(spritesheet, 1, 0, 576, 64, 64)
        self.death_animation = Animation(spritesheet, 5, 64, 1280, 64, 64)

        self.directions = {
            "north": (self.walk_animation_north, self.stand_animation_north),
            "south": (self.walk_animation_south, self.stand_animation_south),
            "west": (self.walk_animation_west, self.stand_animation_west),
            "east": (self.walk_animation_east, self.stand_animation_east),
        }

        self.current_direction = "south"
        self.name = "Undefined"
        self.has_buff = False

    def death_animation_sync(self, player_animation):
        self.death_animation.current_frame = player_animation.current_frame
        self.death_animation.last_update = player_animation.last_update

    def update(self, direction, is_moving, dt, speed_scale):
        self.current_direction = direction
        for dir, (animation, standing_animation) in self.directions.items():
            if dir == self.current_direction:
                if is_moving:
                    animation.update(dt, speed_scale)
                else:
                    standing_animation.update(dt, 0)

    def draw(self, screen, camera, x, y, size_x, size_y, moved, tint):
        if moved:
            self.directions[self.current_direction][0].draw(
                screen, camera, x, y, size_x, size_y, tint
            )
        else:
            self.directions[self.current_direction][1].draw(
                screen, camera, x, y, size_x, size_y, tint
            )


class Weapon(Equipment):
    def __init__(self, spritesheet, projectile_type, life, speed, cooldown, damage):
        super().__init__(spritesheet)
        self.name = "Undefined Weapon"
        self.life = life
        self.speed = speed
        self.projectile_type = projectile_type
        self.cooldown = cooldown
        self.damage = damage

    def attack(self, player, mouse_x, mouse_y, camera_rect):
        projectile = self.projectile_type(
            mouse_x, mouse_y, self.life, self.speed, self.damage, player, camera_rect
        )
        return projectile


class Armour(Equipment):
    def __init__(self, spritesheet, hp_buff=0, speed_buff=0):
        super().__init__(spritesheet)
        self.hp_buff = hp_buff
        self.speed_buff = speed_buff
        self.name = "Undefined Armour"
        self.has_buff = True

    def apply_buff(self, player):
        player.max_hp += self.hp_buff
        player.max_speed += self.speed_buff

    def remove_buff(self, player):
        player.max_hp -= self.hp_buff
        if player.hp > player.max_hp:
            player.hp = player.max_hp
        player.max_speed -= self.speed_buff


class Artefact(Equipment):
    def __init__(self, spritesheet):
        super().__init__(spritesheet)
        self.name = "Undefined Artefact"

    def activate_effect(self, player, mouse_x, mouse_y, collision_rects):
        pass


class Shortbow(Weapon):
    PROJECTILE = Arrow
    LIFE = 25
    SPEED = 600
    COOLDOWN = 1000
    DAMAGE = 1

    def __init__(self):
        super().__init__(
            Spritesheet("source/img/shortbow.png"),
            self.PROJECTILE,
            self.LIFE,
            self.SPEED,
            self.COOLDOWN,
            self.DAMAGE,
        )
        self.class_name = self.__class__.__name__
        self.name = "Shortbow"
        self.equipment_slot = "Weapon"
        self.icon = pygame.image.load(resource_path("source/img/shortbow_icon.png"))


class Brimstone(Weapon):
    PROJECTILE = Meteor
    LIFE = 30
    SPEED = 600
    COOLDOWN = 2000
    DAMAGE = 4

    def __init__(self):
        super().__init__(
            Spritesheet("source/img/brimstone.png"),
            self.PROJECTILE,
            self.LIFE,
            self.SPEED,
            self.COOLDOWN,
            self.DAMAGE,
        )
        self.class_name = self.__class__.__name__
        self.name = "Brimstone Staff"
        self.equipment_slot = "Weapon"
        self.icon = pygame.image.load(resource_path("source/img/brimstone_icon.png"))


class FireStaff(Weapon):
    PROJECTILE = FireBall
    LIFE = 30
    SPEED = 450
    COOLDOWN = 800
    DAMAGE = 2

    def __init__(self):
        super().__init__(
            Spritesheet("source/img/firestaff.png"),
            self.PROJECTILE,
            self.LIFE,
            self.SPEED,
            self.COOLDOWN,
            self.DAMAGE,
        )
        self.class_name = self.__class__.__name__
        self.name = "Fire Staff"
        self.equipment_slot = "Weapon"
        self.icon = pygame.image.load(resource_path("source/img/fire_staff_icon.png"))


class LeatherPants(Armour):
    def __init__(self):
        super().__init__(Spritesheet("source/img/leatherpants.png"), hp_buff=1)
        self.class_name = self.__class__.__name__
        self.name = "Leather Pants"
        self.equipment_slot = "Legs"
        self.icon = pygame.image.load(
            resource_path("source/img/leather_pants_icon.png")
        )


class BlackBoots(Armour):
    def __init__(self):
        super().__init__(Spritesheet("source/img/blackboots.png"), speed_buff=20)
        self.class_name = self.__class__.__name__
        self.name = "Black Boots"
        self.equipment_slot = "Feet"
        self.icon = pygame.image.load(resource_path("source/img/black_boots_icon.png"))


class Chainmail(Armour):
    def __init__(self):
        super().__init__(Spritesheet("source/img/chainmail.png"), hp_buff=3)
        self.class_name = self.__class__.__name__
        self.name = "Chainmail"
        self.equipment_slot = "Torso"
        self.icon = pygame.image.load(resource_path("source/img/chainmail_icon.png"))


class TeleportScroll(Artefact):
    COOLDOWN = 1500
    TELEPORT_DELAY = 400

    def __init__(self):
        super().__init__(Spritesheet("source/img/teleportscroll.png"))
        self.class_name = self.__class__.__name__
        self.name = "Teleport Scroll"
        self.equipment_slot = "Artefact"
        self.icon = pygame.image.load(
            resource_path("source/img/teleport_scroll_icon.png")
        )
        self.teleport_radius = 300
        self.last_activation = 0

    def activate_effect(self, player, mouse_x, mouse_y, collision_rects, map):
        current_time = pygame.time.get_ticks()
        if current_time - self.last_activation < self.COOLDOWN:
            return False

        if not self.is_in_radius(player.rect.center, (mouse_x, mouse_y)):
            return False

        potential_rect = pygame.Rect(
            0, 0, player.collision_rect.width, player.collision_rect.height
        )
        potential_rect.midbottom = (mouse_x, mouse_y)

        if (
            potential_rect.top < 0
            or potential_rect.bottom > map.height
            or potential_rect.left < 0
            or potential_rect.right > map.width
        ):
            return False

        if self.is_collision(potential_rect, collision_rects):
            return False
        pygame.time.set_timer(pygame.USEREVENT + 1, self.TELEPORT_DELAY)
        self.last_activation = current_time
        self.add_smoke_effect(player.rect, map)

        player.teleport(mouse_x, mouse_y)
        player.targetable = False
        player.teleporting = True
        player.canmove = False
        player.canattack = False

        return True

    def add_smoke_effect(self, rect, map):
        for _ in range(50):
            x = random.uniform(rect.left, rect.right)
            y = random.uniform(rect.top, rect.bottom)
            map.add_particle(TeleportParticle(int(x), int(y)))

    def is_in_radius(self, center, point):
        dx = center[0] - point[0]
        dy = center[1] - point[1]
        distance = (dx**2 + dy**2) ** 0.5
        return distance <= self.teleport_radius

    def is_collision(self, rect, collision_rects):
        return rect.collidelist(collision_rects) != -1


class HealingNecklace(Artefact):
    COOLDOWN = 15000

    def __init__(self):
        super().__init__(Spritesheet("source/img/chainmail.png"))
        self.class_name = self.__class__.__name__
        self.name = "Healing Necklace"
        self.equipment_slot = "Artefact"
        self.icon = pygame.image.load(
            resource_path("source/img/healing_necklace_icon.png")
        )
        self.last_activation = 0
        self.activation_sound = pygame.mixer.Sound(
            resource_path("source/sound/heal.mp3")
        )
        self.activation_sound.set_volume(0.6)

    def activate_effect(self, player, mouse_x, mouse_y, collision_rects, map):
        current_time = pygame.time.get_ticks()
        if current_time - self.last_activation < self.COOLDOWN:
            return False

        player.heal_max()
        self.add_healing_particles(player.rect, map)

        self.last_activation = current_time
        sound_manager = SoundManager.getInstance()
        sound_manager.play_sfx(self.activation_sound)
        return True

    def add_healing_particles(self, rect, map):
        for _ in range(50):
            map.add_particle(HealingParticle(rect))
