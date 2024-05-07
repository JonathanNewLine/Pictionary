from typing import TYPE_CHECKING
from single_room import Room

if TYPE_CHECKING:
    from client import ClientThread

class Rooms:
    NOT_CONNECTED_TO_ROOM = None

    def __init__(self) -> None:
        self.rooms_list: dict[int: Room] = dict()


    def add_client(self, room_id: int, client: 'ClientThread') -> Room:
        room_requested: Room = self.rooms_list.get(room_id)

        if room_requested == None: # if id wasn't found
            return None

        room_requested.add_client(client)
        return room_requested
    

    def remove_client(self, client: 'ClientThread') -> None:
        current_id = client.current_room.get_room_id()
        room_connected_to: Room = self.rooms_list.get(current_id)

        # if player is last in the room, delete the room
        if (len(room_connected_to.get_client_list()) == 1):
            self.rooms_list.pop(current_id)
        else: # remove player from the room
            room_connected_to.remove_client(client)


    def generate_room_id(self) -> int:
        if len(self.rooms_list) == 0:
            new_room_id = 0 # create first room with id 0
        else:
            new_room_id = list(self.rooms_list.keys())[-1] + 1 # get last room id and add 1
        return new_room_id
    
    def create_new_room(self, manager: 'ClientThread') -> tuple[int, Room]:
        new_room_id = self.generate_room_id()
        self.rooms_list[new_room_id] = Room(manager, new_room_id)

        return new_room_id, self.rooms_list[new_room_id] # return the new room id and the room object
    
    def room_exists(self, room_id: int) -> bool:
        return room_id in self.rooms_list.keys()