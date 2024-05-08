import socket
from threading import Thread
from single_room import Room
from rooms import Rooms
from client_info import ClientInfo


BUFFER_SIZE = 8192 
NUM_OF_ROUNDS = 3
ROUND_TIME_OFFSET = 2
ROUND_TIME = 60 + ROUND_TIME_OFFSET

DRAWING_PLAYER_SCORE_MULTIPLIER = 30

class ClientThread(Thread):

    def __init__(self, connection: socket.socket, all_rooms: Rooms) -> None: 
        Thread.__init__(self)
        self.all_rooms: Rooms = all_rooms
        self.client_info: ClientInfo = ClientInfo()
        self.current_room: Room = Rooms.NOT_CONNECTED_TO_ROOM
        self.connection: socket.socket = connection


    def join_room(self, id:int) -> bool:
        room_requested = self.all_rooms.add_client(id, self)
        if room_requested is None:
            self.send_message("no")
            return False
        else:
            self.current_room = room_requested
            self.send_message("yes")
            self.send_waiting_room_start_time()
            return True


    def create_new_room(self) -> None:
        new_id, self.current_room = self.all_rooms.create_new_room(self)
        self.send_message(str(new_id))
        self.update_waiting_room_start_time()


    def remove_client_from_room(self) -> None:
        self.client_info.init_statistic_data()
        
        if self.current_room is None:
            self.current_room = Rooms.NOT_CONNECTED_TO_ROOM
            return
        
        prev_manager = self.current_room.get_room_manager() # get the manager before removing the client
        self.all_rooms.remove_client(self)

        if not self.all_rooms.room_exists(self.current_room.get_room_id()):
            self.current_room = Rooms.NOT_CONNECTED_TO_ROOM
            return

        new_client_list = self.current_room.get_client_list()
        if prev_manager == self:
            new_client_list[0].set_new_manager()

        # if the only one left in the game, send alone message
        if len(new_client_list) == 1 and self.current_room.is_ingame():
            new_client_list[0].announce_alone_in_game()
        else:
            self.notify_all_on_connected_users_changed()

        self.current_room = Rooms.NOT_CONNECTED_TO_ROOM

    
    def set_new_manager(self) -> None:
        self.send_message("manager")


    def announce_alone_in_game(self) -> None:
        self.send_message("alone")

    
    def announce_is_drawing(self) -> None:
        self.send_message("draw")


    def send_guesser_guessed_correctly(self) -> None:
        self.send_message("correct")


    def send_guesser_guessed_incorrectly(self) -> None:
        self.send_message("wrong")


    def announce_is_guessing(self, drawing_player_name: str) -> None:
        self.send_message("guess " + drawing_player_name)

    
    def send_drawing_player_word(self) -> None:
        self.send_message("word: " + self.current_room.get_secret_word())

    
    def send_statistics_to_user(self) -> None:
        self.send_message("statistics: " + self.client_info.to_json())


    def send_all_continue_next_round(self, secret_word:str) -> None:
        self.send_all("continue " + secret_word, True)


    def send_guessing_player_clue(self, secret_word:str) -> None:
        self.send_message("clue: " + self.generate_clue_for_guessers(secret_word))


    def send_all_statistics(self) -> None:
        for client in self.current_room.get_client_list():
            client.send_statistics_to_user()


    def init_all_statistics_data(self) -> None:
        for client in self.current_room.get_client_list():
            client.client_info.init_statistic_data()


    def send_waiting_room_start_time(self) -> None:
        self.send_message("time: " + str(self.current_room.get_waiting_room_start_time()))


    def send_all_waiting_room_start_time(self) -> None:
        self.send_all("time: " + str(self.current_room.get_waiting_room_start_time()), True)


    def notify_connected_users_changed(self) -> None:
        users_json = self.current_room.get_users_json()
        self.send_message("users: " + users_json)


    def notify_all_on_connected_users_changed(self) -> None:
        for client in self.current_room.get_client_list(): 
            client.notify_connected_users_changed()


    def send_all_drawing(self, drawing_bytes: str, length: int) -> None:
        composed_drawing_message = str(length) + "dataBytes:" + drawing_bytes
        self.send_all(composed_drawing_message, False)


    def send_all_winner(self) -> None:
        self.send_all(f"winner: {self.get_winner_name_and_points()}", True)


    def send_all_to_waiting_room(self) -> None:
        self.send_all("waiting", True)

    def run(self) -> None:
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
            except AloneInGameError:
                self.handle_alone_in_room()

    
    def handle_client(self) -> None:
        if self.current_room == Rooms.NOT_CONNECTED_TO_ROOM:
            self.connect_to_room()
        while True:
            self.handle_waiting_room()
            self.handle_drawing_room()


    def get_username(self) -> bool:
        try:
            name_sent = self.receive_message()
            self.client_info.set_name(name_sent)
            return True
        except ConnectionError:
            self.handle_player_connection_aborted()
            return False


    def wait_for_room_join(self) -> None:
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
        self.wait_for_room_join()
        self.notify_all_on_connected_users_changed()


    def handle_waiting_room(self) -> None:
        if self.current_room.is_ingame():
            self.join_room_while_ingame()
        else:
            if self.is_manager():
                self.start_from_manager()
            else:
                self.wait_for_game_beggining()


    def join_room_while_ingame(self) -> None:
        self.send_message("start")
        self.wait_for_game_beggining() # wait for client to accept game start


    def start_from_manager(self) -> None:
        self.receive_until("start")
        # for first round, initiate the data
        self.init_game_data(True)
        self.start_game_for_all()
        self.receive_until("start ok")


    def update_waiting_room_start_time(self) -> None:
        self.current_room.set_waiting_room_start_time()
        self.send_all_waiting_room_start_time()


    def start_game_for_all(self) -> None:
        # update statistics for all users
        for client in self.current_room.get_client_list():
            client.client_info.add_to_games_played(1)

        self.send_all("start", True)

        # send all users the the user list for the game room
        self.notify_all_on_connected_users_changed()

        self.current_room.set_ingame(True)


    def wait_for_game_beggining(self) -> None:
        received_message = self.receive_until("start ok", "manager ok")

        if received_message == "manager ok":
            self.start_from_manager()


    def handle_drawing_room(self) -> None:
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
        # choose word for round
        self.current_room.generate_secret_word()
        self.set_next_drawer_name(is_first)
        self.current_room.init_correct_guesses()
        self.current_room.set_game_start_time()


    def finish_game(self) -> None:
        if self.is_manager():
            self.manager_go_to_winner_screen()

        self.receive_until("waiting ok")
        # get the users list to the waiting room
        self.notify_connected_users_changed()
        # get the time elapsed to the waiting room
        self.send_waiting_room_start_time()


    def manager_go_to_winner_screen(self) -> None:
        self.send_all_winner()
        self.send_all_to_waiting_room()
        self.send_all_statistics()
        self.reset_game_data()
        


    def reset_game_data(self) -> None:
        self.init_all_statistics_data()
        self.current_room.set_ingame(False)
        self.current_room.set_waiting_room_start_time()


    def get_winner_name_and_points(self) -> str:
        winner = max(self.current_room.get_client_list(), key=lambda client: client.client_info.get_points())
        winner.client_info.add_to_games_won(1)
        return f"{winner.client_info.get_name()},{str(winner.client_info.get_points())}"
    

    def handle_drawing_player(self) -> None:
        round_secret_word = self.current_room.get_secret_word()
        self.announce_is_drawing()
        self.send_drawing_player_word()

        self.update_drawings_at_guessers()

        self.award_points_for_drawing_player()
        self.init_game_data(False)

        self.send_all_continue_next_round(round_secret_word)
        self.receive_until("continue ok")

        
    def set_next_drawer_name(self, is_first: bool) -> None:
        client_list = self.current_room.get_client_list()

        if is_first:
            next_drawer_index = 0
        else:
            next_drawer_index = (client_list.index(self) + 1) % len(client_list)
            
        next_drawer_name = client_list[next_drawer_index].client_info.get_name()
        self.current_room.set_drawing_player_name(next_drawer_name)

        
    def handle_guessing_player(self) -> None:
        drawer_name = self.current_room.get_drawing_player_name()
        secret_word = self.current_room.get_secret_word()
        self.announce_is_guessing(drawer_name)
        self.send_guessing_player_clue(secret_word)
        self.process_guesses()


    def process_guesses(self) -> None:
        guessed_correctly = False
        try:
            while not self.is_game_ended() or guessed_correctly:
                message = self.receive_message()
                if message == "continue ok":
                    return

                guessed_correctly = self.process_message_from_guesser(message)

            self.receive_until("continue ok")
        except ExitRoomError:
            # if the player exited the room, remove the correct guess he made
            if guessed_correctly:
                self.current_room.sub_correct_guess()
            raise ExitRoomError
        

    def process_message_from_guesser(self, message: str) -> bool:
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
        self.client_info.add_to_correct_guesses(1)
        self.client_info.add_to_points(self.get_points_for_guess())
        self.notify_all_on_connected_users_changed() # update points for guessing player for everyone
        self.current_room.add_correct_guess()


    def award_points_for_drawing_player(self) -> None:
        # if the drawing player won because it is alone in the game, don't give points
        if len(self.current_room.get_client_list()) > 1:
            self.client_info.add_to_points(self.get_points_for_drawing())
            # update points for drawing player for everyone
            self.notify_all_on_connected_users_changed()
        
    
    def update_drawings_at_guessers(self) -> None:
        while not self.is_game_ended():
            length, drawing_bytes = self.receive_drawing()
            if (length <= 0):
                continue
            # received drawing, send it to all guessers
            self.send_all_drawing(drawing_bytes, length)
        

    def is_game_ended(self) -> bool:
        is_everyone_guessed = self.current_room.is_everyone_guessed_correctly()
        is_time_ended = self.current_room.get_game_time_elapsed() >= ROUND_TIME
        return is_everyone_guessed or is_time_ended
    
    
    def generate_clue_for_guessers(self, secret_word:str) -> str:
        return ' '.join(' ' if char.isspace() else '_' for char in secret_word)
    

    def get_points_for_guess(self) -> int:
        return ROUND_TIME - self.current_room.get_game_time_elapsed() - ROUND_TIME_OFFSET
    

    def get_points_for_drawing(self) -> int:
        return self.current_room.get_correct_guesses()*DRAWING_PLAYER_SCORE_MULTIPLIER
        

    def send_message(self, message: str) -> None:
        self.connection.send((message + "\n").encode())
        if len(message) < 20 or "{" in message:
            print(f"\n-- send: '{message}' to {self.client_info.get_name()}")


    def receive_drawing(self) -> tuple[int, str]:
        try:
            self.connection.settimeout(0.5)
            drawing_player_response = self.receive_message()
            length, initial_data = tuple(drawing_player_response.split("dataBytes:", 1))
            return self.get_rest_of_drawing(int(length), initial_data)
        
        except (ValueError, TimeoutError):
            return 0, ""
        except (ExitRoomError, AloneInGameError) as exception:
            raise exception
        finally:
            self.connection.settimeout(None)

    
    def get_rest_of_drawing(self, length: int, initial_data: str) -> tuple[int, str]:
        data = initial_data
        while len(data) < length:
            data += self.receive_message()
        return length, data


    def receive_message(self) -> str:
        message = self.connection.recv(BUFFER_SIZE).decode()
        if len(message) < 20 or "{" in message:
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
        return self.client_info.get_name()
    

    def send_all(self, message: str, include_self: bool) -> None:
        for player in self.current_room.get_client_list():
            if not include_self and player == self:
                continue
            player.send_message(message)
    

    def receive_until(self, expected1: str, expected2: str=None) -> str:
        received_message = self.receive_message()
        while received_message != expected1 and received_message != expected2:
            received_message = self.receive_message()

        return received_message
    

    def is_correct_guess(self, guess: str) -> bool:
        return guess.lower().strip() == self.current_room.get_secret_word().lower()


    def update_room_alone(self) -> None:
        self.send_message("alone")


    def handle_player_exit_room(self) -> None:
        self.remove_client_from_room()
        self.send_message("exit ok")


    def handle_player_connection_aborted(self) -> None:
        self.remove_client_from_room()
        self.connection.close()


    def handle_alone_in_room(self) -> None:
        self.finish_game()

    
    def is_manager(self) -> bool:
        return self.current_room.get_room_manager() == self


class ExitRoomError(Exception):
    pass

class AloneInGameError(Exception):
    pass