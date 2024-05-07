import json

class ClientInfo:
    def __init__(self) -> None:
        self._name: str = ""
        self._points: int = 0
        self._guesses: int = 0
        self._correct_guesses: int = 0
        self._games_played: int = 0
        self._games_won: int = 0

    def get_name(self) -> str:
        return self._name

    def get_points(self) -> int:
        return self._points

    def get_guesses(self) -> int:
        return self._guesses

    def get_correct_guesses(self) -> int:
        return self._correct_guesses

    def get_games_played(self) -> int:
        return self._games_played

    def get_games_won(self) -> int:
        return self._games_won

    def set_name(self, name: str) -> None:
        self._name = name

    def add_to_points(self, points: int) -> None:
        self._points += points

    def add_to_guesses(self, guesses: int) -> None:
        self._guesses += guesses

    def add_to_correct_guesses(self, correct_guesses: int) -> None:
        self._correct_guesses += correct_guesses

    def add_to_games_played(self, games_played: int) -> None:
        self._games_played += games_played

    def add_to_games_won(self, games_won: int) -> None:
        self._games_won += games_won

    def init_statistic_data(self) -> None:
        self._points = 0
        self._guesses = 0
        self._correct_guesses = 0
        self._games_played = 0
        self._games_won = 0

    def to_json(self):
        return json.dumps({
            "guesses": self._guesses,
            "correctGuesses": self._correct_guesses,
            "gamesPlayed": self._games_played,
            "gamesWon": self._games_won
        })
