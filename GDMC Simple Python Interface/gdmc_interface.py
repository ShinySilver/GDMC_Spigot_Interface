import requests


def get_block(x: int, y: int, z: int):
    """

    :param x: 
    :param y: 
    :param z: 
    :return: 
    """
    url = f'http://localhost:4567/block?x={x}&y={y}&z={z}'
    return requests.get(url)


def set_block(x: int, y: int, z: int, block: str):
    """

    :param x:
    :param y:
    :param z:
    :param block:
    :return:
    """
    url = f'http://localhost:4567/block?x={x}&y={y}&z={z}&block={block}'
    return requests.put(url)


def commit(commit_name: str = "Unnamed_Commit"):
    """

    :param commit_name:
    :return:
    """
    url = f'http://localhost:4567/commit?commit_name={commit_name}'
    return requests.post(url)


def sync(timeout: int = 300):
    """

    :return:
    """
    url = f'http://localhost:4567/sync?timeout={timeout}'
    return requests.post(url)


def status():
    """

    :return:
    """
    url = f'http://localhost:4567/status'
    return requests.post(url)
