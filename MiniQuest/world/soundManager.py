import pygame
import math
from utilities.resourcePath import resource_path


# This class is used to manage the sound effects and music in the game.
class SoundManager:
    _instance = None

    @staticmethod
    def getInstance():
        if SoundManager._instance is None:
            SoundManager()
        return SoundManager._instance

    @staticmethod
    def reset_instance():
        SoundManager._instance = None

    def __init__(self):
        if SoundManager._instance is not None:
            raise Exception("This class is a singleton!")
        else:
            SoundManager._instance = self
        self.current_music = None
        self.music_fading = False
        self.new_music = None
        self.music_volume = 0.2
        pygame.mixer.music.set_volume(self.music_volume)
        self.sfx_volume = 0.2
        self.deafening = False
        self.undeafening = False
        self.deafen_start_time = 0
        self.undeafen_start_time = 0
        self.sfx_channels = {}
        self.sound_events = []

    # This function is used to load new music. It will fade out the current music and fade in the new music. It will not do anything if the new music is the same as the current music.
    def load_new_music(self, music_file):
        if self.current_music != music_file:
            self.new_music = music_file
            pygame.mixer.music.fadeout(1000)
            self.music_fading = True

    # This function is used to play the new music. It will only play the new music if the current music has finished fading out.
    def play_new_music(self):
        if self.music_fading and not pygame.mixer.music.get_busy():
            pygame.mixer.music.load(resource_path("source/sound/" + self.new_music))
            pygame.mixer.music.play(loops=-1, fade_ms=2000)
            self.current_music = self.new_music
            self.music_fading = False
            self.new_music = None

    # This function is used to play a sound effect which will first fade out the music, the play sfx and then fade the music back in.
    def play_sfx_with_music_fade(self, sfx_file):
        self.start_deafen()
        self.sound_events.append(
            {
                "sfx_file": sfx_file,
                "action": "play_sfx",
                "start_time": pygame.time.get_ticks(),
            }
        )

    # This function updates the sound manager. It is called every frame.
    def update(self):
        current_time = pygame.time.get_ticks()

        if self.deafening:
            time_passed = current_time - self.deafen_start_time
            if time_passed >= 250:
                self.deafening = False
            else:
                new_volume = self.music_volume * (1 - time_passed / 250)
                pygame.mixer.music.set_volume(new_volume)

        if self.undeafening:
            time_passed = current_time - self.undeafen_start_time
            if time_passed >= 1000:
                self.undeafening = False
            else:
                new_volume = self.music_volume * (time_passed / 1000)
                pygame.mixer.music.set_volume(new_volume)

        new_events = []
        for event in self.sound_events:
            if event["action"] == "play_sfx" and not self.deafening:
                self.play_sfx(event["sfx_file"])
                event["action"] = "undeafen"
                new_events.append(event)
            elif event["action"] == "undeafen" and not self.deafening:
                if not self.is_sfx_playing(event["sfx_file"]):
                    self.start_undeafen()
                else:
                    new_events.append(event)
            else:
                new_events.append(event)

        self.sound_events = new_events

        if self.new_music and not self.undeafening and not self.deafening:
            self.play_new_music()

    # This function is used to start the deafening effect.
    def start_deafen(self):
        self.deafening = True
        self.deafen_start_time = pygame.time.get_ticks()

    # This function is used to start the undeafening effect.
    def start_undeafen(self):
        self.undeafening = True
        self.undeafen_start_time = pygame.time.get_ticks()

    # This function is used to play a sound effect. If listen_pos and source_pos are provided, the sound will be attenuated based on the distance between the two positions.
    def play_sfx(
        self, sfx_file, listen_pos=None, source_pos=None, max_distance=1000, volume=None
    ):
        sound = sfx_file

        if listen_pos and source_pos:
            distance = math.sqrt(
                (source_pos[0] - listen_pos[0]) ** 2
                + (source_pos[1] - listen_pos[1]) ** 2
            )
            attenuation = max(0, 1 - distance / max_distance)
            effective_volume = self.sfx_volume * attenuation
        else:
            effective_volume = self.sfx_volume

        if volume:
            effective_volume = volume * effective_volume

        sound.set_volume(effective_volume)

        channel = pygame.mixer.find_channel()
        if channel:
            channel.play(sound)
            self.sfx_channels[sfx_file] = channel

    # This function is used to check if a sound effect is playing.
    def is_sfx_playing(self, sfx_file):
        channel = self.sfx_channels.get(sfx_file)
        if channel:
            return channel.get_busy()
        return False

    # This function is used to stop a sound effect.
    def stop_sfx(self, sfx_file):
        channel = self.sfx_channels.get(sfx_file)
        if channel:
            channel.stop()
