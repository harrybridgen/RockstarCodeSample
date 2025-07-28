import pygame
from pygame.math import Vector2
import random


class Camera:
    def __init__(self, player_rect_centre, width, height, map_width, map_height):
        self.target_pos = Vector2(player_rect_centre)
        self.rect = pygame.FRect(
            self.target_pos.x - width / 2,
            self.target_pos.y - height / 2,
            width,
            height,
        )
        self.shake_intensity = 0
        self.shake_duration = 0
        self.shake_timer = 0
        self.map_dims = Vector2(map_width, map_height)
        self.lerp_speed = 0.5

    def shake(self, duration, intensity):
        self.shake_intensity = intensity
        self.shake_duration = duration
        self.shake_timer = 0

    def update(self, dt, player_rect_centre):
        self.target_pos = Vector2(player_rect_centre)

        threshold_distance = 1
        distance_to_target = self.target_pos.distance_to(self.rect.center)

        if distance_to_target > threshold_distance:
            lerp_pos = self.lerp(self.rect.center, self.target_pos, self.lerp_speed)

            self.rect.centerx = round(
                max(
                    self.rect.width / 2,
                    min(self.map_dims.x - self.rect.width / 2, lerp_pos.x),
                )
            )
            self.rect.centery = round(
                max(
                    self.rect.height / 2,
                    min(self.map_dims.y - self.rect.height / 2, lerp_pos.y),
                )
            )

        if self.shake_timer < self.shake_duration:
            self.shake_timer += dt
            t = self.shake_timer / self.shake_duration
            self.rect.center = self.smootherstep(
                self.target_pos, self.rect.center, t
            ) + Vector2(
                self.shake_intensity * (0.5 - random.random()),
                self.shake_intensity * (0.5 - random.random()),
            )

    def teleport_to_player(self, player_rect_centre):
        self.target_pos = Vector2(player_rect_centre)
        self.rect.center = self.target_pos

    def change_map(self, map_width, map_height):
        self.map_dims = Vector2(map_width, map_height)

    @staticmethod
    def lerp(v0, v1, t):
        return (1 - t) * Vector2(v0) + t * Vector2(v1)

    @staticmethod
    def smootherstep(v0, v1, t):
        t = max(0, min(1, t))
        t = t * t * t * (t * (t * 6 - 15) + 10)
        return (1 - t) * Vector2(v0) + t * Vector2(v1)
