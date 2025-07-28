import pygame

from graphics.spritesheet import Spritesheet


class HealthBar:
    def __init__(self, player, x, y):
        self.x = x
        self.y = y
        self.player = player
        self.sprite_sheet = Spritesheet("source/img/hearts.png")
        self.heart_full = pygame.transform.scale(
            self.sprite_sheet.get_image(0, 0, 17, 17), (34, 34)
        )
        self.heart_half = pygame.transform.scale(
            self.sprite_sheet.get_image(17, 0, 17, 17), (34, 34)
        )
        self.heart_empty = pygame.transform.scale(
            self.sprite_sheet.get_image(34, 0, 17, 17), (34, 34)
        )

    def draw(self, screen):
        full_hearts = self.player.hp // 2
        half_heart = self.player.hp % 2
        empty_hearts = ((self.player.max_hp + 1) // 2) - full_hearts - \
                                                            half_heart

        for i in range(full_hearts):
            screen.blit(self.heart_full, (self.x + i * 40, self.y))

        if half_heart:
            screen.blit(self.heart_half, (self.x + full_hearts * 40, self.y))

        for i in range(empty_hearts):
            screen.blit(
                self.heart_empty, (self.x + (full_hearts + half_heart + i) * 
                                   40, self.y)
            )
