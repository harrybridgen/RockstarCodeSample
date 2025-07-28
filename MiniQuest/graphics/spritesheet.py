import pygame

from utilities.resourcePath import resource_path


class Spritesheet:
    def __init__(self, filename):
        self.spritesheet = pygame.image.load(resource_path(filename)).convert_alpha()

    def get_image(self, x, y, width, height):
        image = pygame.Surface((width, height), pygame.SRCALPHA)
        image.blit(self.spritesheet, (0, 0), (x, y, width, height))
        image = pygame.transform.scale(image, (width, height))
        return image
