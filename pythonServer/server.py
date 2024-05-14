import socket
from client import ClientThread
from rooms import Rooms

TCP_IP = '0.0.0.0'
TCP_PORT = 6969
INCOMING_CONNECTIONS = 5

class Server:
    """
    Server that handles client connections and manages rooms.
    """

    def __init__(self) -> None:
        """ intializes the server's rooms.
        """
        self.rooms = Rooms()

    def run(self) -> None:
        """ Runs the server and listens for incoming connections. if a
        connection is made, a new thread is created to handle the client.
        """
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as main_thread:
            main_thread.bind((TCP_IP, TCP_PORT)) 
            while True: 
                main_thread.listen(INCOMING_CONNECTIONS) 
                connection, (ip, port) = main_thread.accept()
                self.add_thread(connection, self.rooms)

    def add_thread(self, connection: socket, rooms: Rooms) -> None:
        """ Adds a new thread to the server to handle a client connection.

        Args:
            connection (socket): The connection to the client.
            rooms (Rooms): The rooms object of the server.
        """
        new_client = ClientThread(connection, rooms) 
        new_client.start()
        