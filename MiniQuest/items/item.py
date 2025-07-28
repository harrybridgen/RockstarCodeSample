import pygame
from utilities.resourcePath import resource_path


class Item:
    def __init__(self, name):
        self.name = name
        self.class_name = self.__class__.__name__


class Bones(Item):
    def __init__(self):
        super().__init__("Bones")
        self.icon = pygame.image.load(resource_path("source/img/bones_icon.png"))


class Logs(Item):
    def __init__(self):
        super().__init__("Logs")
        self.icon = pygame.image.load(resource_path("source/img/logs_icon.png"))


class Flour(Item):
    def __init__(self):
        super().__init__("Flour")
        self.icon = pygame.image.load(resource_path("source/img/flour_icon.png"))


class Bucket(Item):
    def __init__(self):
        super().__init__("Bucket")
        self.icon = pygame.image.load(resource_path("source/img/bucket_icon.png"))


class BucketOfMilk(Item):
    def __init__(self):
        super().__init__("Bucket of Milk")
        self.icon = pygame.image.load(
            resource_path("source/img/bucket_of_milk_icon.png")
        )


class Egg(Item):
    def __init__(self):
        super().__init__("Egg")
        self.icon = pygame.image.load(resource_path("source/img/egg_icon.png"))


class Pot(Item):
    def __init__(self):
        super().__init__("Pot")
        self.icon = pygame.image.load(resource_path("source/img/pot_icon.png"))


class RareGem(Item):
    def __init__(self):
        super().__init__("Rare Gem")
        self.icon = pygame.image.load(resource_path("source/img/rare_gem_icon.png"))


class EnchantedHerb(Item):
    def __init__(self):
        super().__init__("Enchanted Herb")
        self.icon = pygame.image.load(
            resource_path("source/img/enchanted_herb_icon.png")
        )
