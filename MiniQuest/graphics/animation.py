import pygame
from pygame.math import Vector2

from utilities.constants import WHITE


class Animation:
    def __init__(self, spritesheet, num_frames, x, y, width, height):
        self.frames = [
            spritesheet.get_image(x + i * width, y, width, height)
            for i in range(num_frames)
        ]
        self.current_frame = 0
        self.last_update = pygame.time.get_ticks()
        self.frame_rate = 200
        self.direction = "south"

    def update(self, dt, speed_scale):
        frame_rate = self.frame_rate / (1 + speed_scale)
        self.last_update += dt
        if self.last_update > (frame_rate / 1000):
            self.current_frame = (self.current_frame + 1) % len(self.frames)
            self.last_update = 0

    def draw(self, screen, camera, x, y, size_x, size_y, color=WHITE, scale=1):
        image = self.frames[self.current_frame].copy()

        if color is not None:
            image.fill(color, special_flags=pygame.BLEND_MULT)

        if scale != 1:
            image = pygame.transform.scale(
                image, (int(image.get_width() * scale), int(image.get_height() * scale))
            )

        image_rect = pygame.FRect(image.get_rect())

        if camera is not None:
            relative_pos = (x - camera.rect.left, y - camera.rect.top)
        else:
            relative_pos = (x, y)

        if size_x is not None and size_y is not None:
            image_rect.center = (
                round(relative_pos[0] + size_x // 2 * scale, 2),
                round(relative_pos[1] + size_y // 2 * scale, 2) - 5,
            )
        else:
            image_rect.center = (
                round(relative_pos[0], 2),
                round(relative_pos[1], 2) - 5,
            )

        screen.blit(image, (image_rect.topleft))

    def reset(self):
        self.current_frame = 0

    def is_finished(self):
        return self.current_frame == len(self.frames) - 1

    def get_current_image(self):
        return self.frames[self.current_frame].copy()
