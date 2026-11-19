"""Commands for the isolated loopback-only all-addons validation server."""
import socket
import struct
import sys

def receive(s):
    def read(n):
        data = b''
        while len(data) < n:
            chunk = s.recv(n - len(data))
            if not chunk:
                raise EOFError('RCON disconnected')
            data += chunk
        return data
    size = struct.unpack('<i', read(4))[0]
    packet = read(size)
    return struct.unpack('<ii', packet[:8]), packet[8:-2].decode('utf-8')

def send(s, ident, kind, value):
    packet = struct.pack('<ii', ident, kind) + value.encode('utf-8') + b'\0\0'
    s.sendall(struct.pack('<i', len(packet)) + packet)

with socket.create_connection(('127.0.0.1', 25618), timeout=30) as s:
    send(s, 1, 3, 'local-addon-validation')
    identity, _ = receive(s)
    if identity[0] == -1:
        raise RuntimeError('RCON authentication failed')
    for i, command in enumerate(sys.argv[1:], 2):
        send(s, i, 2, command)
        _, output = receive(s)
        print(command + '\n' + output, flush=True)
