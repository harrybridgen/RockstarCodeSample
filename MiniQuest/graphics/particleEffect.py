import math
import pygame
import pytmx
import random

from graphics.particle import Particle


class ParticleEffect:
    def update(self, dt):
        for particle in list(self.particles):
            if particle.update(dt):
                self.particles.remove(particle)

        return len(self.particles) == 0


class ArrowExplosion(ParticleEffect):
    def __init__(self, x, y, size):
        self.particles = [
            Particle(
                x,
                y,
                (random.uniform(-120, 120)),
                (random.uniform(-120, 120)),
                (
                    random.randint(100, 150),
                    random.randint(100, 150),
                    random.randint(100, 150),
                ),
                size,
            )
            for _ in range(20)
        ]


class FireBallExplosion(ParticleEffect):
    def __init__(self, x, y, size):
        self.particles = [
            Particle(
                x,
                y,
                (random.uniform(-200, 200)),
                (random.uniform(-200, 200)),
                (
                    random.randint(200, 255),
                    random.randint(50, 100),
                    0,
                ),
                size,
            )
            for _ in range(20)
        ]
