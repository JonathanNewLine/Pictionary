import socket
from client import ClientThread
from rooms import Rooms

TCP_IP = '0.0.0.0'
TCP_PORT = 6969
INCOMING_CONNECTIONS = 5

class Server:
    def __init__(self) -> None:
        self.rooms = Rooms()

    def run(self) -> None:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as main_thread:
            main_thread.bind((TCP_IP, TCP_PORT)) 
            while True: 
                main_thread.listen(INCOMING_CONNECTIONS) 
                connection, (ip, port) = main_thread.accept()
                self.add_thread(connection, self.rooms)

    def add_thread(self, connection: socket, rooms: Rooms) -> None:
        new_client = ClientThread(connection, rooms) 
        new_client.start()
        