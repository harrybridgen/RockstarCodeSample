import math
import pygame
import pytmx
import random
import math


class Particle:
    def __init__(self, start_x, start_y, velocity_x, velocity_y, color, size):
        self.image = pygame.Surface((size, size))
        self.image.fill(color)
        self.rect = pygame.FRect(start_x, start_y, size, size)
        self.rect.center = (start_x, start_y)
        self.velocity_x = velocity_x
        self.velocity_y = velocity_y
        self.lifetime = 0

    def update(self, dt, *args):
        self.rect.x += self.velocity_x * dt
        self.rect.y += self.velocity_y * dt

        self.lifetime += 120 * dt
        alpha = max(255 - self.lifetime * 5, 0)
        self.image.set_alpha(alpha)
        if alpha <= 0:
            return True
        return False


class OrbitParticle(Particle):
    def __init__(
        self, center_x, center_y, speed, size, color, base_radius, radius_offset
    ):
        self.center = pygame.Vector2(center_x, center_y)
        self.base_radius = base_radius
        self.radius_offset = radius_offset
        self.angle = random.uniform(0, 2 * math.pi)
        self.speed = speed
        x, y = self.calculate_position()

        self.image = pygame.Surface((size, size), pygame.SRCALPHA)

        self.image.fill(color)

        self.rect = pygame.FRect(x, y, size, size)
        self.rect.center = (x, y)
        self.velocity_x = 0
        self.velocity_y = 0
        self.lifetime = 0

    def calculate_position(self):
        radius = self.base_radius + self.radius_offset * math.sin(2 * self.angle)
        x = self.center.x + radius * math.cos(self.angle)
        y = self.center.y + radius * math.sin(self.angle)
        return x, y

    def update(self, dt, *args):
        self.angle += self.speed * dt
        self.rect.center = self.calculate_position()
        return False


class walkParticle(Particle):
    def __init__(self, entity):
        center_x = int(entity.collision_rect.centerx)
        center_y = int(entity.collision_rect.centery)

        offset_x = int(entity.collision_rect.width // 4)
        offset_y = int(entity.collision_rect.height // 4)

        x = random.randint(center_x - offset_x, center_x + offset_x)
        y = random.randint(center_y - offset_y, center_y + offset_y)

        velocity_x = random.uniform(-12, 12)
        velocity_y = -10
        color = (
            random.randint(100, 165),
            random.randint(50, 115),
            random.randint(10, 45),
        )
        super().__init__(x, y, velocity_x, velocity_y, color, random.randint(2, 4))


class FireBallParticle(Particle):
    def __init__(self, start_x, start_y, size):
        velocity_x = random.uniform(-60, 60)
        velocity_y = random.uniform(-60, 60)

        color = (255, random.randint(0, 100), 0)

        super().__init__(start_x, start_y, velocity_x, velocity_y, color, size)


class ArrowParticle(Particle):
    def __init__(self, start_x, start_y):
        velocity_x = random.uniform(-40, 40)
        velocity_y = random.uniform(-40, 20)

        color = (
            random.randint(200, 255),
            random.randint(200, 255),
            random.randint(200, 255),
        )

        size = random.randint(2, 5)

        super().__init__(start_x, start_y, velocity_x, velocity_y, color, size)


class TeleportParticle(Particle):
    def __init__(self, start_x, start_y):
        angle = random.uniform(0, 2 * math.pi)
        speed = random.uniform(10, 30)
        velocity_x = speed * math.cos(angle)
        velocity_y = speed * math.sin(angle)

        intensity = random.randint(50, 200)
        color = (intensity, intensity, intensity)

        size = random.randint(3, 5)

        super().__init__(start_x, start_y, velocity_x, velocity_y, color, size)

    def update(self, dt, *args):
        self.velocity_y -= 10 * dt

        self.rect.x += self.velocity_x * dt
        self.rect.y += self.velocity_y * dt

        self.lifetime += 15 * dt
        alpha = max(255 - self.lifetime * 9, 0)
        self.image.set_alpha(alpha)

        if alpha <= 0:
            return True
        return False


class HealingParticle(Particle):
    def __init__(self, player_rect):
        self.color = (
            random.randint(150, 200),
            random.randint(0, 25),
            random.randint(0, 25),
        )
        self.speed = random.randint(50, 100)
        self.relative_target_pos = pygame.Vector2(
            random.randint(0, player_rect.width), random.randint(0, player_rect.height)
        )
        self.target_pos = pygame.Vector2(player_rect.topleft) + self.relative_target_pos

        self.radius = random.randint(20, 60)
        self.angle = random.uniform(0, 2 * math.pi)
        self.position = self.target_pos + pygame.Vector2(
            self.radius * math.cos(self.angle), self.radius * math.sin(self.angle)
        )
        self.size = random.randint(3, 5)
        self.image = pygame.Surface((self.size, self.size))
        self.image.fill(self.color)
        self.rect = pygame.Rect(self.position.x, self.position.y, self.size, self.size)

        self.swirl_angle = random.uniform(0, 2 * math.pi)
        self.swirl_speed = random.choice([-10, 10])

    def update(self, dt, player_rect):
        self.target_pos = pygame.Vector2(player_rect.topleft) + self.relative_target_pos

        direction_vector = self.target_pos - self.position
        if direction_vector.length() != 0:
            direction_vector = direction_vector.normalize() * 2

        self.swirl_angle += self.swirl_speed
        swirl_vector = pygame.Vector2(
            math.cos(self.swirl_angle), math.sin(self.swirl_angle)
        )
        swirl_vector = pygame.Vector2(-swirl_vector.y, swirl_vector.x)

        direction_vector += swirl_vector
        direction_vector = direction_vector.normalize()

        self.position += direction_vector * (self.speed * dt)
        self.rect.topleft = self.position

        distance = self.target_pos.distance_to(self.position)
        if distance <= self.speed * dt:
            return True
        else:
            self.speed += 150 * dt
            self.swirl_speed = self.swirl_speed * 0.9 + random.uniform(-10, 10) * dt
        return False
