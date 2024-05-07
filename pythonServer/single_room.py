from typing import TYPE_CHECKING
import random
import time
import json

if TYPE_CHECKING:
    from client import ClientThread

class Room:

    def __init__(self, manager: 'ClientThread', room_id: int) -> None:
        self.players: list['ClientThread'] = list()
        self.room_id: int = room_id
        self.secret_word: str = ""
        self.ingame: bool = False
        self.waiting_room_start_time: int
        self.game_start_time: int
        self.correct_guesses_num: int = 0
        self.drawer_name: str = ""
        self.players.append(manager)

    
    def add_client(self, client: 'ClientThread') -> None:
        self.players.append(client)


    def remove_client(self, client: 'ClientThread') -> None:
        self.players.remove(client)


    def get_client_list(self) -> list['ClientThread']:
        return self.players
    
    
    def generate_secret_word(self) -> None:
        with open('word_list.json', 'r') as file:
            data = json.load(file)
        words = [entry['word'] for entry in data]
        self.secret_word = random.choice(words)

    
    def get_room_manager(self) -> 'ClientThread':
        return self.players[0]
    
    
    def get_secret_word(self) -> str:
        return self.secret_word
    
    
    def get_users_json(self) -> str:
        client_data = []
        for client_thread in self.players:
            client_info = {
                "username": client_thread.client_info.get_name(),
                "points": client_thread.client_info.get_points()
            }
            client_data.append(client_info)
        return json.dumps(client_data)
    
    
    def is_ingame(self) -> bool:
        return self.ingame
    
    
    def set_ingame(self, is_ingame: bool) -> None:
        self.ingame = is_ingame


    def set_waiting_room_start_time(self) -> None:
        self.waiting_room_start_time = self.get_curr_time()


    def get_curr_time(self) -> int:
        return int(time.time())


    def get_waiting_room_start_time(self) -> int:
        return self.waiting_room_start_time
    
    
    def add_correct_guess(self) -> None:
        self.correct_guesses_num += 1


    def init_correct_guesses(self) -> None:
        self.correct_guesses_num = 0


    def is_everyone_guessed_correctly(self) -> bool:
        return self.correct_guesses_num == len(self.players) - 1 and len(self.players) > 1
    

    def get_correct_guesses(self) -> int:
        return self.correct_guesses_num
    
    
    def sub_correct_guess(self) -> None:
        self.correct_guesses_num -= 1

    
    def get_drawing_player_name(self) -> str:
        return self.drawer_name
    
    
    def set_drawing_player_name(self, name:str) -> None:
        self.drawer_name = name


    def get_game_time_elapsed(self) -> int:
        return self.get_curr_time() - self.game_start_time


    def get_game_start_time(self) -> int:
        return self.game_start_time
    
    
    def set_game_start_time(self) -> None:
        self.game_start_time = self.get_curr_time()


    def get_room_id(self) -> int:
        return self.room_id