import random
import os

import math
import pickle
import pygame
import pytmx
import tempfile

class DayNightCycle:
    """
    Manages the progression of time and calculates lighting conditions based on the time of day.
    """

    def __init__(self, initial_time=7.0):
        """
        Initializes the day-night cycle.

        Args:
            initial_time (float): The starting time of day in hours (0.0 to 24.0).
        """
        self.time_of_day = initial_time  # Time in hours
        self.death_counter = 2.0  # Used if the player is dying
        self.initial_light_level = None  # Set when the player starts dying

    def update(self, dt):
        """
        Updates the time of day based on the elapsed time.

        Args:
            dt (float): Delta time since the last update in seconds.
        """
        self.time_of_day += 0.01 * dt
        self.time_of_day %= 24.0  # Wrap around after 24 hours

    def get_time_string(self):
        """
        Returns the current time as a formatted string.

        Returns:
            str: The time in HH:MM format.
        """
        hours = int(self.time_of_day)
        minutes = int((self.time_of_day - hours) * 60)
        return "{:02d}:{:02d}".format(hours, minutes)

    def get_time_period(self):
        """
        Determines the period of the day based on the current time.

        Returns:
            str: The time period ("Morning", "Afternoon", "Evening", "Night").
        """
        if 6 <= self.time_of_day < 12:
            return "Morning"
        elif 12 <= self.time_of_day < 18:
            return "Afternoon"
        elif 18 <= self.time_of_day < 24:
            return "Evening"
        else:
            return "Night"

    def get_alpha(self, is_interior, map_light_level, player_dying=False, death_counter=2.0):
        """
        Calculates the alpha value for the darkness overlay based on time of day and conditions.

        Args:
            is_interior (bool): Whether the player is inside a building.
            map_light_level (int): The light level of the current map.
            player_dying (bool): Whether the player is dying.
            death_counter (float): The death countdown timer.

        Returns:
            int: The alpha value for the darkness overlay (0 to 255).
        """
        sunrise_start, sunrise_end = 5, 9  # Sunrise from 5:00 to 9:00
        sunset_start, sunset_end = 16, 22  # Sunset from 16:00 to 22:00
        max_night_alpha = 210  # Maximum darkness outside of full blackout

        if not is_interior:
            # Calculate alpha for outdoor areas
            if sunrise_start <= self.time_of_day < sunrise_end:
                progress = (self.time_of_day - sunrise_start) / (sunrise_end - sunrise_start)
                overlay_alpha = max_night_alpha * (1 - progress)
            elif sunset_start <= self.time_of_day < sunset_end:
                progress = (self.time_of_day - sunset_start) / (sunset_end - sunset_start)
                overlay_alpha = max_night_alpha * progress
            elif 7 <= self.time_of_day <= 17:
                overlay_alpha = 0  # Full daylight
            else:
                overlay_alpha = max_night_alpha
        else:
            # Interior spaces use map's light level
            overlay_alpha = 255 * (1 - map_light_level / 10)

        if player_dying:
            # Increase darkness as the player dies
            additional_darkness = (1 - death_counter / 2.0) * (255 - overlay_alpha)
            overlay_alpha += additional_darkness
            overlay_alpha = min(overlay_alpha, 255)

        return self.clamp(overlay_alpha, 0, 255)

    @staticmethod
    def clamp(value, min_value, max_value):
        """
        Clamps a value between a minimum and maximum value.

        Args:
            value (float): The value to clamp.
            min_value (float): The minimum allowable value.
            max_value (float): The maximum allowable value.

        Returns:
            float: The clamped value.
        """
        return max(min_value, min(value, max_value))

    def set_time(self, time):
        """
        Sets the time of day to a specific value.

        Args:
            time (float): The new time of day in hours (0.0 to 24.0).
        """
        self.time_of_day = time
