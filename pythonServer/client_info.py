import json

import json

class ClientInfo:
    """
    Represents client information including name, points, guesses, correct guesses, games played, and games won.
    """

    def __init__(self) -> None:
        """
        Initializes a new instance of the ClientInfo class.
        """
        # client's name
        self.name: str = ""
        # client's total points
        self.points: int = 0
        # number of guesses made by the client
        self.guesses: int = 0
        # number of correct guesses made by the client
        self.correct_guesses: int = 0
        # number of games played by the client
        self.games_played: int = 0
        # number of games won by the client
        self.games_won: int = 0

    def get_name(self) -> str:
        """ get the name of the client

        Returns:
            str: name of the client
        """
        return self.name

    def get_points(self) -> int:
        """ get the points of the client

        Returns:
            int: points of the client
        """
        return self.points

    def get_guesses(self) -> int:
        """ get the number of guesses made by the client

        Returns:
            int: the number of guesses made by the client
        """
        return self.guesses

    def get_correct_guesses(self) -> int:
        """ get the number of correct guesses made by the client
        
        Returns:
            int: number of correct guesses made by the client
        """
        return self.correct_guesses

    def get_games_played(self) -> int:
        """get the number of games played by the client

        Returns:
            int: number of games played by the client
        """
        return self.games_played

    def get_games_won(self) -> int:
        """ get the number of games won by the client

        Returns:
            int: number of games won by the client
        """
        return self.games_won

    def set_name(self, name: str) -> None:
        """ set the name of the client

        Args:
            name (str): the name of the client
        """
        self.name = name

    def add_to_points(self, points: int) -> None:
        """ add points to the client

        Args:
            points (int): the points to be added
        """
        self.points += points

    def add_to_guesses(self, guesses: int) -> None:
        """ add the number of guesses made by the client

        Args:
            guesses (int): the number of guesses to be added
        """
        self.guesses += guesses

    def add_to_correct_guesses(self, correct_guesses: int) -> None:
        """ add the number of correct guesses made by the client

        Args:
            correct_guesses (int): the number of correct guesses to be added
        """
        self.correct_guesses += correct_guesses

    def add_to_games_played(self, games_played: int) -> None:
        """ add the number of games played by the client

        Args:
            games_played (int): the number of games played to be added
        """
        self.games_played += games_played

    def add_to_games_won(self, games_won: int) -> None:
        """ add the number of games won by the client

        Args:
            games_won (int): the number of games won to be added
        """
        self.games_won += games_won

    def init_statistic_data(self) -> None:
        """ initialize the statistic data of the client
        """
        self.points = 0
        self.guesses = 0
        self.correct_guesses = 0
        self.games_played = 0
        self.games_won = 0

    def to_json(self) -> str:
        """ convert the client information to json format

        Returns:
            str: the client information in json format
        """
        return json.dumps({
            "guesses": self.guesses,
            "correctGuesses": self.correct_guesses,
            "gamesPlayed": self.games_played,
            "gamesWon": self.games_won
        })
