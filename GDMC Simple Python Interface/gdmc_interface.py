import requests
from typing import List

def get_block(x: int, y: int, z: int):
    """

    :param x: 
    :param y: 
    :param z: 
    :return: 
    """
    url = f'http://localhost:9000/blocks?x={x}&y={y}&z={z}'
    return requests.get(url)


def set_block(x: int, y: int, z: int, block_id: str, do_block_updates: bool = True):
    """

    :param x:
    :param y:
    :param z:
    :param block_id:
    :param do_block_updates:
    :return:
    """
    url = f'http://localhost:9000/blocks?x={x}&y={y}&z={z}&doBlockUpdates={do_block_updates}'
    return requests.put(url, str(block_id))


def set_blocks(X: List[int], Y: List[int], Z: List[int], blockIds: List[str],
               root_x: int = 0, root_y: int = 0, root_z: int = 0, do_block_updates: bool = True):
    """

    :param blockIds:
    :param X:
    :param Y:
    :param Z:
    :param root_x:
    :param root_y:
    :param root_z:
    :param do_block_updates:
    :return:
    """
    body = str.join("\n", [f'~{x} ~{y} ~{z} {blockId}' for blockId, x, y, z in zip(blockIds, X, Y, Z)])
    return set_block(root_x, root_y, root_z, body, do_block_updates)


def run_command(command: str):
    """

    :param command: 
    :return: 
    """
    url = 'http://localhost:9000/command'
    return requests.post(url, bytes(str(command), "utf-8"))