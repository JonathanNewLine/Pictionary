from typing import TYPE_CHECKING
import random
import time
import json

if TYPE_CHECKING:
    from client import ClientThread


def get_curr_time() -> int:
    """ gets the current time in epoch

    Returns:
        int: the current epoch time
    """
    return int(time.time())


class Room:
    """
    Represents a room in a multiplayer game.
    """

    def __init__(self, manager: 'ClientThread', room_id: int) -> None:
        """ Initializes the room with a manager and a room ID.

        Args:
            manager (ClientThread): The manager of the room.
            room_id (int): The ID of the room.
        """
        # all the players in the room
        self.waiting_room_start_time = None
        self.game_start_time = None
        self.players: list['ClientThread'] = list()
        # the room id
        self.room_id: int = room_id
        # the secret word for the game
        self.secret_word: str = ""
        # whether the game is in progress
        self.ingame: bool = False
        # the time the waiting room started
        self.waiting_room_start_time: int
        # the time the game started
        self.game_start_time: int
        # the number of correct guesses
        self.correct_guesses_num: int = 0
        # the name of the player drawing
        self.drawer_name: str = ""
        # add the manager to the room
        self.players.append(manager)

    def add_client(self, client: 'ClientThread') -> None:
        """ Adds a client to the room.

        Args:
            client (ClientThread): The client to add to the room.
        """
        self.players.append(client)

    def remove_client(self, client: 'ClientThread') -> None:
        """ Removes a client from the room.

            Args:
                client (ClientThread): The client to be removed.
            """
        self.players.remove(client)

    def get_client_list(self) -> list['ClientThread']:
        """ Returns the list of clients in the room.

        Returns:
            list[ClientThread]: _description_
        """
        return self.players

    def generate_secret_word(self) -> None:
        """ Generates a secret word for the room from a list of words.
        """
        with open('word_list.json', 'r') as file:
            data = json.load(file)
        words = [entry['word'] for entry in data]
        self.secret_word = random.choice(words)

    def get_room_manager(self) -> 'ClientThread':
        """ Returns the manager of the room.

        Returns:
            ClientThread: The manager of the room.
        """
        return self.players[0]

    def get_secret_word(self) -> str:
        """ Returns the secret word for the room.

        Returns:
            str: The secret word.
        """
        return self.secret_word

    def get_users_json(self) -> str:
        """ Returns the list of users in the room as a JSON string.

        Returns:
            str: The list of users in the room as a JSON string.
        """
        client_data = []
        for client_thread in self.players:
            client_info = {
                "username": client_thread.client_info.get_name(),
                "points": client_thread.client_info.get_points()
            }
            client_data.append(client_info)
        return json.dumps(client_data)

    def is_ingame(self) -> bool:
        """ Returns whether the room is in a game.

        Returns:
            bool: Whether the room is in a game.
        """
        return self.ingame

    def set_ingame(self, is_ingame: bool) -> None:
        """ Sets whether the room is in a game.

        Args:
            is_ingame (bool): Whether the room is in a game.
        """
        self.ingame = is_ingame

    def set_waiting_room_start_time(self) -> None:
        """ Sets the time the waiting room started.
        """
        self.waiting_room_start_time = get_curr_time()

    def get_waiting_room_start_time(self) -> int:
        """ Returns the time the waiting room started.

        Returns:
            int: The time the waiting room started.
        """
        return self.waiting_room_start_time

    def add_correct_guess(self) -> None:
        """ Adds a correct guess to the room.
        """
        self.correct_guesses_num += 1

    def init_correct_guesses(self) -> None:
        """ Initializes the correct guesses for the room.
        """
        self.correct_guesses_num = 0

    def is_everyone_guessed_correctly(self) -> bool:
        """ Returns whether everyone has guessed correctly.

        Returns:
            bool: Whether everyone has guessed correctly.
        """
        return self.correct_guesses_num == len(self.players) - 1 and len(self.players) > 1

    def get_correct_guesses(self) -> int:
        """ Returns the number of correct guesses.

        Returns:
            int: The number of correct guesses.
        """
        return self.correct_guesses_num

    def sub_correct_guess(self) -> None:
        """ Subtracts a correct guess from the room.
        """
        self.correct_guesses_num -= 1

    def get_drawing_player_name(self) -> str:
        """ Returns the name of the player drawing.

        Returns:
            str: The name of the player drawing.
        """
        return self.drawer_name

    def set_drawing_player_name(self, name: str) -> None:
        """ Sets the name of the player drawing.

        Args:
            name (str): The name of the player drawing.
        """
        self.drawer_name = name

    def get_game_time_elapsed(self) -> int:
        """ Returns the time elapsed since the game started.

        Returns:
            int: The epoch time elapsed since the game started.
        """
        return get_curr_time() - self.game_start_time

    def get_game_start_time(self) -> int:
        """ Returns the time the game started.

        Returns:
            int: The time the game started.
        """
        return self.game_start_time

    def set_game_start_time(self) -> None:
        """ Sets the time the game started.
        """
        self.game_start_time = get_curr_time()

    def get_room_id(self) -> int:
        """ Returns the ID of the room.

        Returns:
            int: The ID of the room.
        """
        return self.room_id
