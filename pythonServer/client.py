import socket
from threading import Thread
from single_room import Room
from rooms import Rooms
from client_info import ClientInfo

"""constants"""
# buffer size for receiving messages
BUFFER_SIZE = 8192 
# number of rounds in a game
NUM_OF_ROUNDS = 3
# time offset for round time
ROUND_TIME_OFFSET = 2
# round time in seconds
ROUND_TIME = 60 + ROUND_TIME_OFFSET

# points multiplier for drawing player
GUESSING_PLAYER_SCORE_MULTIPLIER = 5
# points multiplier for guessing player
DRAWING_PLAYER_SCORE_MULTIPLIER = 80

class ClientThread(Thread):
    """ A thread that handles a client connection to the server.
    """

    def __init__(self, connection: socket.socket, all_rooms: Rooms) -> None: 
        """ Initializes the client thread with the given connection,
        all rooms, the client information.

        Args:
            connection (socket): the connection to the client
            all_rooms (Rooms): the rooms object that contains all the rooms
        """
        # call the parent constructor
        Thread.__init__(self)
        # all the rooms in the server
        self.all_rooms: Rooms = all_rooms
        # the client's information
        self.client_info: ClientInfo = ClientInfo()
        # the room the client is connected to
        self.current_room: Room = Rooms.NOT_CONNECTED_TO_ROOM
        # the connection to the client
        self.connection: socket.socket = connection


    def join_room(self, id:int) -> bool:
        """ join the room with the given id

        Args:
            id (int): the id of the room to join

        Returns:
            bool: True if the room was joined successfully, False otherwise
        """
        room_requested = self.all_rooms.add_client(id, self)
        # if the room doesn't exist
        if room_requested is None:
            self.send_message("no")
            return False
        elif room_requested.is_ingame():
            self.send_already_ingame()
            return False
        else:
            # if the room exists
            self.current_room = room_requested
            self.send_message("yes")
            self.send_waiting_room_start_time()
            return True


    def create_new_room(self) -> None:
        """ create a new room and connect to it
        """
        new_id, self.current_room = self.all_rooms.create_new_room(self)
        self.send_message(str(new_id))
        self.update_waiting_room_start_time()


    def remove_client_from_room(self) -> None:
        """ remove the client from the current room
        """
        self.client_info.init_statistic_data()
        
        if self.current_room is None:
            self.current_room = Rooms.NOT_CONNECTED_TO_ROOM
            return
        
        prev_manager = self.current_room.get_room_manager() # get the manager before removing the client
        self.all_rooms.remove_client(self)
        if not self.all_rooms.room_exists(self.current_room.get_room_id()):
            self.current_room = Rooms.NOT_CONNECTED_TO_ROOM
            return
        
        self.replace_manager(prev_manager)
        self.send_to_waiting_room_alone()
        self.current_room = Rooms.NOT_CONNECTED_TO_ROOM
        

    def replace_manager(self, prev_manager: 'ClientThread') -> None:
        """ replace the manager of the room

        Args:
            prev_manager (ClientThread): the previous manager of the room
        """
        client_list = self.current_room.get_client_list()
        if prev_manager == self:
            client_list[0].set_new_manager()


    def send_to_waiting_room_alone(self) -> None:
        """ send the client to the waiting room if he is alone in the game
        """
        client_list = self.current_room.get_client_list()
        # if the only one left in the game, send alone message
        if len(client_list) == 1 and self.current_room.is_ingame():
            client_list[0].announce_alone_in_game()
        else:
            self.notify_all_on_connected_users_changed()

    
    def set_new_manager(self) -> None:
        """ update user that he is the manager
        """
        self.send_message("manager")


    def announce_alone_in_game(self) -> None:
        """ announce to the user that he is alone in the game
        """
        self.send_message("alone")

    
    def announce_is_drawing(self) -> None:
        """ announce to the user that he is drawing
        """
        self.send_message("draw: " + self.current_room.get_secret_word())


    def send_guesser_guessed_correctly(self) -> None:
        """ send a message to the user that he guessed correctly
        """
        self.send_message("correct")


    def send_guesser_guessed_incorrectly(self) -> None:
        """ send a message to the user that he guessed incorrectly
        """
        self.send_message("wrong")


    def send_already_ingame(self) -> None:
        """ send a message to the user that the game he's trying to join is ongoing
        """
        self.send_message("ingame")


    def announce_is_guessing(self, drawing_player_name: str, secret_word: str) -> None:
        """ announce to the user that he is guessing

        Args:
            drawing_player_name (str): the name of the drawing player
            secret_word (str): the secret word of the round
        """
        self.send_message("guess: " + drawing_player_name + "," + self.generate_clue_for_guessers(secret_word))

    
    def send_statistics_to_user(self) -> None:
        """ send the statistics of the user to the user
        """
        self.send_message("statistics: " + self.client_info.to_json())


    def send_all_continue_next_round(self, secret_word:str, include_self:bool) -> None:
        """ send a message to all users to continue to the next round

        Args:
            secret_word (str): the secret word of the round
            include_self (bool): should the message be sent to the user itself
        """
        self.send_all("continue: " + secret_word, include_self)


    def send_all_statistics(self) -> None:
        """ send the statistics to all users
        """
        for client in self.current_room.get_client_list():
            client.send_statistics_to_user()


    def init_all_statistics_data(self) -> None:
        """ initialize the statistics data for all users
        """
        for client in self.current_room.get_client_list():
            client.client_info.init_statistic_data()


    def send_waiting_room_start_time(self) -> None:
        """ send the waiting room start time to the user
        """
        self.send_message("time: " + str(self.current_room.get_waiting_room_start_time()))


    def send_all_waiting_room_start_time(self) -> None:
        """ send the waiting room start time to all users
        """
        self.send_all("time: " + str(self.current_room.get_waiting_room_start_time()), True)


    def notify_connected_users_changed(self) -> None:
        """ notify user in the room that the connected users or their points changed
        """
        users_json = self.current_room.get_users_json()
        self.send_message("users: " + users_json)


    def notify_all_on_connected_users_changed(self) -> None:
        """ notify all users in the room that the connected users, or their points changed
        """
        for client in self.current_room.get_client_list(): 
            client.notify_connected_users_changed()


    def send_all_drawing(self, drawing_bytes: str, length: int) -> None:
        """ send the drawing to all users in the room

        Args:
            drawing_bytes (str): the drawing bytes
            length (int): the length of the drawing bytes
        """
        composed_drawing_message = str(length) + "dataBytes:" + drawing_bytes
        self.send_all(composed_drawing_message, False)


    def send_all_to_waiting_room(self) -> None:
        """ send all users to the waiting room
        """
        self.send_all(f"waiting: {self.get_winner_name_and_points()}", True)

    def run(self) -> None:
        """ the main loop of the client thread
        """
        # if obtaining username fails, return
        if not self.get_username():
            return
        
        while True:
            try:
                self.handle_client()
            except ConnectionError:
                self.handle_player_connection_aborted()
                return
            except ExitRoomError:
                self.handle_player_exit_room()
                continue
            except AloneInGameError:
                self.handle_alone_in_room()
                continue

    
    def handle_client(self) -> None:
        """ handle the client's actions
        """
        if self.current_room == Rooms.NOT_CONNECTED_TO_ROOM:
            self.connect_to_room()
        while True:
            self.handle_waiting_room()
            self.handle_drawing_room()


    def get_username(self) -> bool:
        """ get the username of the client

        Returns:
            bool: True if the username was received successfully, False otherwise
        """
        try:
            name_sent = self.receive_message()
            self.client_info.set_name(name_sent)
            return True
        except ConnectionError:
            return False


    def wait_for_room_join(self) -> None:
        """ wait for the client to join a room
        """
        joining_room_successful = False

        while not joining_room_successful:
            player_response = self.receive_message()

            if player_response == "new room":
                self.create_new_room()
                joining_room_successful = True
            elif player_response.startswith("find room"):
                room_id_request = int(player_response.split("find room ")[1])
                joining_room_successful = self.join_room(room_id_request)


    def connect_to_room(self) -> None:
        """ connect the client to a room
        """
        self.wait_for_room_join()
        # after connected to room, update all on connected users changed
        self.notify_all_on_connected_users_changed()


    def handle_waiting_room(self) -> None:
        """ handle the waiting room actions
        """
        if self.is_manager():
            self.start_from_manager()
        else:
            self.wait_for_game_beggining()

        
    def start_from_manager(self) -> None:
        """ wait for the manager to start the game, and then start it
        """
        self.receive_until("start")
        # for first round, initiate the data
        self.init_game_data(True)
        self.start_game_for_all()
        self.receive_until("start ok")


    def update_waiting_room_start_time(self) -> None:
        """ update the waiting room start time
        """
        self.current_room.set_waiting_room_start_time()
        # update all on waiting room start time changed
        self.send_all_waiting_room_start_time()


    def start_game_for_all(self) -> None:
        """ update everyone that the game has started
        """
        # update statistics for all users
        for client in self.current_room.get_client_list():
            client.client_info.add_to_games_played(1)

        self.send_all("start", True)

        # send all users the the user list for the game room
        self.notify_all_on_connected_users_changed()

        self.current_room.set_ingame(True)


    def wait_for_game_beggining(self) -> None:
        """ wait for the game to start by the manager
        """
        received_message = self.receive_until("start ok", "manager ok")

        # if player has been chosen as the manager, do the manager actions
        if received_message == "manager ok":
            self.start_from_manager()


    def handle_drawing_room(self) -> None:
        """ handle the drawing room actions
        """
        # rounds loop
        for round in range(NUM_OF_ROUNDS):
            # pick a drawing player
            for drawing_player in self.current_room.get_client_list():
                if self == drawing_player:
                    self.handle_drawing_player()
                else:
                    self.handle_guessing_player()

        self.finish_game()
    
    
    def init_game_data(self, is_first: bool) -> None:
        """ initialize the game data for the round

        Args:
            is_first (bool): is it the first round
        """
        # choose word for round
        self.current_room.generate_secret_word()
        self.set_next_drawer_name(is_first)
        self.current_room.init_correct_guesses()
        self.current_room.set_game_start_time()


    def finish_game(self) -> None:
        """ finish the game
        """
        if self.is_manager():
            # with the manager, send everyone to the waiting room
            self.manager_go_to_winner_screen()

        self.receive_until("waiting ok")
        # get the users list to the waiting room
        self.notify_connected_users_changed()
        # get the time elapsed to the waiting room
        self.send_waiting_room_start_time()


    def manager_go_to_winner_screen(self) -> None:
        """ send everyone to the winner screen and the waiting room
        """
        self.send_all_to_waiting_room()
        self.send_all_statistics()
        self.reset_game_data()
        

    def reset_game_data(self) -> None:
        """ reset the game data, and prepare the waiting room
        """
        self.init_all_statistics_data()
        self.current_room.set_ingame(False)
        self.current_room.set_waiting_room_start_time()


    def get_winner_name_and_points(self) -> str:
        """ get the winner name and points

        Returns:
            str: the winner's name and points
        """
        winner = max(self.current_room.get_client_list(), key=lambda client: client.client_info.get_points())
        winner.client_info.add_to_games_won(1)
        return f"{winner.client_info.get_name()},{str(winner.client_info.get_points())}"
    

    def handle_drawing_player(self) -> None:
        """ handle the drawing player's actions

        Raises:
            exception: if the drawing player exits the room
        """
        try:
            round_secret_word = self.current_room.get_secret_word()
            self.announce_is_drawing()

            self.update_drawings_at_guessers()
            self.award_points_for_drawing_player()

            # update on game change
            self.init_game_data(False)
            self.send_all_continue_next_round(round_secret_word, True)
            self.receive_until("continue ok")
        except (ExitRoomError, ConnectionAbortedError) as exception:
            # if there's more then one player left in the room
            if len(self.current_room.get_client_list()) > 2:
                # update on game change
                self.init_game_data(False)
                self.send_all_continue_next_round(round_secret_word, False)
            raise exception
        
        
    def set_next_drawer_name(self, is_first: bool) -> None:
        """ set the next drawer name

        Args:
            is_first (bool): is it the first round
        """
        client_list = self.current_room.get_client_list()

        # update the next drawer index
        if is_first:
            next_drawer_index = 0
        else:
            next_drawer_index = (client_list.index(self) + 1) % len(client_list)
        
        # get the next drawer name
        next_drawer_name = client_list[next_drawer_index].client_info.get_name()
        self.current_room.set_drawing_player_name(next_drawer_name)

        
    def handle_guessing_player(self) -> None:
        """ handle the guessing player's actions
        """
        drawer_name = self.current_room.get_drawing_player_name()
        secret_word = self.current_room.get_secret_word()
        self.announce_is_guessing(drawer_name, secret_word)
        self.process_guesses()


    def process_guesses(self) -> None:
        """ process the guesses of the guessers

        Raises:
            exception: if the player exits the room while in-game
        """
        guessed_correctly = False
        try:
            while not self.is_game_ended() or guessed_correctly:
                message = self.receive_message()
                if message == "continue ok":
                    return

                guessed_correctly = self.process_message_from_guesser(message)

            self.receive_until("continue ok")
        except (ExitRoomError, ConnectionAbortedError) as exception:
            # if the player exited the room, remove the correct guess he made
            if guessed_correctly:
                self.current_room.sub_correct_guess()
            raise exception
        

    def process_message_from_guesser(self, message: str) -> bool:
        """ process the message from the guesser

        Args:
            message (str): the message from the guesser

        Returns:
            bool: True if the guess was correct, False otherwise
        """
        if message == "manager ok":
            return False
        
        self.client_info.add_to_guesses(1)

        if self.is_correct_guess(message):
            self.send_guesser_guessed_correctly()
            self.register_correct_guess()
            return True
        else:
            self.send_guesser_guessed_incorrectly()
            return False
        

    def register_correct_guess(self) -> None:
        """ register a correct guess in their statistics and the room
        """
        self.client_info.add_to_correct_guesses(1)
        self.client_info.add_to_points(self.get_points_for_guess())
        self.notify_all_on_connected_users_changed() # update points for guessing player for everyone
        self.current_room.add_correct_guess()


    def award_points_for_drawing_player(self) -> None:
        """ award points for the drawing player
        """
        # if the drawing player won because it is alone in the game, don't give points
        if len(self.current_room.get_client_list()) > 1:
            self.client_info.add_to_points(self.get_points_for_drawing())
            # update points for drawing player for everyone
            self.notify_all_on_connected_users_changed()
        
    
    def update_drawings_at_guessers(self) -> None:
        """ update the drawings at the guessers' screen
        """
        while not self.is_game_ended():
            length, drawing_bytes = self.receive_drawing()
            if (length <= 0):
                continue
            # received drawing, send it to all guessers
            self.send_all_drawing(drawing_bytes, length)
        

    def is_game_ended(self) -> bool:
        """ check if the game has ended

        Returns:
            bool: True if the game has ended, False otherwise
        """
        is_everyone_guessed = self.current_room.is_everyone_guessed_correctly()
        is_time_ended = self.current_room.get_game_time_elapsed() >= ROUND_TIME
        return is_everyone_guessed or is_time_ended
    
    
    def generate_clue_for_guessers(self, secret_word:str) -> str:
        """ generate a clue for the guessers

        Args:
            secret_word (str): the secret word of the round

        Returns:
            str: the clue for the guessers
        """
        return ' '.join(' ' if char.isspace() else '_' for char in secret_word)
    

    def get_points_for_guess(self) -> int:
        """ get the points for the guessing player on their guess

        Returns:
            int: the points for the guessing player
        """
        return (ROUND_TIME - self.current_room.get_game_time_elapsed() - ROUND_TIME_OFFSET)*GUESSING_PLAYER_SCORE_MULTIPLIER
    

    def get_points_for_drawing(self) -> int:
        """ get the points for the drawing player

        Returns:
            int: the points for the drawing player
        """
        return self.current_room.get_correct_guesses()*DRAWING_PLAYER_SCORE_MULTIPLIER
        

    def send_message(self, message: str) -> None:
        """ send a message to the client

        Args:
            message (str): the message to send to the client
        """
        try:
            self.connection.send((message + "\n").encode())
        except BrokenPipeError:
            raise ConnectionAbortedError
            
        if len(message) < 30 or "{" in message:
            print(f"\n-- send: '{message}' to {self.client_info.get_name()}")


    def receive_drawing(self) -> tuple[int, str]:
        """ receive the drawing from the client

        Raises:
            exception: if the drawing player exits the room

        Returns:
            tuple[int, str]: the length of the drawing, and the drawing bytes, or 0,"" if the data
            is malformed
        """
        try:
            self.connection.settimeout(0.5)
            drawing_player_response = self.receive_message()
            length, initial_data = tuple(drawing_player_response.split("dataBytes:", 1))
            return self.get_rest_of_drawing(int(length), initial_data)
        
        except (ValueError, TimeoutError):
            return 0, ""
        except (ExitRoomError, ConnectionAbortedError, AloneInGameError) as exception:
            raise exception
        finally:
            self.connection.settimeout(None)

    
    def get_rest_of_drawing(self, length: int, initial_data: str) -> tuple[int, str]:
        """ get the rest of the drawing with the begging message

        Args:
            length (int): the length of the drawing to be received
            initial_data (str): initial message of the drawing

        Returns:
            tuple[int, str]: the length of the drawing, and the drawing bytes
        """
        data = initial_data
        while len(data) < length:
            data += self.receive_message()
        return length, data


    def receive_message(self) -> str:
        """ receive a message from the client

        Raises:
            ConnectionAbortedError: on connection aborted with client
            ExitRoomError: on player exiting room
            AloneInGameError: if player is alone in the game

        Returns:
            str: the message received from the client
        """
        message = self.connection.recv(BUFFER_SIZE).decode()
        if len(message) < 30 or "{" in message:
            print(f"\n-- receive: '{message}' from {self.client_info.get_name()}")

        if message == "":
            raise ConnectionAbortedError
        elif message == "exit":
            raise ExitRoomError
        elif message == "alone ok":
            raise AloneInGameError
        else:
            return message
                

    def __str__(self) -> str:
        """ get the string representation of the client

        Returns:
            str: the string representation of the client
        """
        return self.client_info.get_name()
    

    def send_all(self, message: str, include_self: bool) -> None:
        """ send a message to all players in the room

        Args:
            message (str): the message to send to everyone
            include_self (bool): should the message be sent to the user itself
        """
        for player in self.current_room.get_client_list():
            if not include_self and player == self:
                continue
            player.send_message(message)
    

    def receive_until(self, expected1: str, expected2: str=None) -> str:
        """ receive messages until one of the expected messages is received

        Args:
            expected1 (str): the first expected message
            expected2 (str, optional): the second expected message. Defaults to None.

        Returns:
            str: _description_
        """
        received_message = self.receive_message()
        while received_message != expected1 and received_message != expected2:
            received_message = self.receive_message()

        return received_message
    

    def is_correct_guess(self, guess: str) -> bool:
        """ check if the guess is correct

        Args:
            guess (str): the guess of the player

        Returns:
            bool: True if the guess is correct, False otherwise
        """
        return guess.lower().strip() == self.current_room.get_secret_word().lower()


    def handle_player_exit_room(self) -> None:
        """ handle the player exiting the room
        """
        self.remove_client_from_room()
        self.send_message("exit ok")


    def handle_player_connection_aborted(self) -> None:
        """ handle the player's connection being aborted
        """
        self.remove_client_from_room()
        self.connection.close()


    def handle_alone_in_room(self) -> None:
        """ handle the player being alone in the room
        """
        self.finish_game()

    
    def is_manager(self) -> bool:
        """ check if the player is the manager of the room

        Returns:
            bool: True if the player is the manager, False otherwise
        """
        return self.current_room.get_room_manager() == self


class ExitRoomError(Exception):
    """
    Exception raised when a player exits the room
    """
    pass

class AloneInGameError(Exception):
    """
    Exception raised when a player is alone in the game
    """
    pass