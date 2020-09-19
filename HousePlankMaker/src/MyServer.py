import socket
from datetime import datetime


def __init__():
    pass


# AF_INET = IPV4, SOCK_STREAM = TCP
soc = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
port = 7132
queueSize = 5  # amount of connections to listen to at once
bufferSize = 1024

# dataTimeObj = datatime.now()

soc.bind(("localhost", port))
soc.listen(queueSize)
print("Server running on port " + str(port))

while True:
    conn, address = soc.accept()  # connect to socket client
    receivedMessage = conn.recv(bufferSize).decode("utf-8")
    # timestamp =
    print(receivedMessage)

    conn.close()

