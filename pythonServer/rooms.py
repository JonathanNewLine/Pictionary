from typing import TYPE_CHECKING
from single_room import Room

if TYPE_CHECKING:
    from client import ClientThread

class Rooms:
    """
    Representing a collection of rooms in a server.
    """
    
    # constants
    # not connected to room - id field filler
    NOT_CONNECTED_TO_ROOM = None

    def __init__(self) -> None:
        """ Initializes the rooms list
        """
        # dictionary that maps room IDs to Room objects.
        self.rooms_list: dict[int: Room] = dict()

    def add_client(self, room_id: int, client: 'ClientThread') -> Room:
        """ Adds a client to a room.

        Args:
            room_id (int): The ID of the room to add the client to.
            client (ClientThread): The client to add to the room.

        Returns:
            Room: The room the client was added to.
        """
        room_requested: Room = self.rooms_list.get(room_id)

        if room_requested == None: # if id wasn't found
            return None

        room_requested.add_client(client)
        return room_requested

    def remove_client(self, client: 'ClientThread') -> None:
        """ Removes a client from the room they are currently in.

        Args:
            client (ClientThread): The client to remove from the room.
        """
        current_id = client.current_room.get_room_id()
        room_connected_to: Room = self.rooms_list.get(current_id)

        # if player is last in the room, delete the room
        if (len(room_connected_to.get_client_list()) == 1):
            self.rooms_list.pop(current_id)
        else: # remove player from the room
            room_connected_to.remove_client(client)

    def generate_room_id(self) -> int:
        """ Generates a new room ID.

        Returns:
            int: the new room id
        """
        if len(self.rooms_list) == 0:
            new_room_id = 0 # create first room with id 0
        else:
            new_room_id = list(self.rooms_list.keys())[-1] + 1 # get last room id and add 1
        return new_room_id

    def create_new_room(self, manager: 'ClientThread') -> tuple[int, Room]:
        """ Creates a new room with the specified manager.

        Args:
            manager (ClientThread): The client that will manage the room.

        Returns:
            tuple[int, Room]: The ID of the new room and the room object.
        """
        new_room_id = self.generate_room_id()
        self.rooms_list[new_room_id] = Room(manager, new_room_id)

        # return the new room id and the room object
        return new_room_id, self.rooms_list[new_room_id] 

    def room_exists(self, room_id: int) -> bool:
        """ Checks if a room with the specified ID exists.

        Args:
            room_id (int): The ID of the room to check.

        Returns:
            bool: True if the room exists, False otherwise.
        """
        return room_id in self.rooms_list.keys()