"""Local isolated development-server console; never changes a player installation."""
import argparse, socket, struct
from pathlib import Path

def recv_exact(sock, length):
    data = bytearray()
    while len(data) < length:
        part = sock.recv(length - len(data))
        if not part: raise ConnectionError("RCON connection closed")
        data.extend(part)
    return bytes(data)

def packet(sock, request, kind, payload):
    body = struct.pack('<ii', request, kind) + payload.encode() + b'\0\0'
    sock.sendall(struct.pack('<i', len(body)) + body)
    length = struct.unpack('<i', recv_exact(sock, 4))[0]
    reply = recv_exact(sock, length)
    ident, response_type = struct.unpack('<ii', reply[:8])
    if ident == -1: raise PermissionError('RCON authentication failed')
    return reply[8:-2].decode('utf-8', 'replace')

if __name__ == '__main__':
    parser=argparse.ArgumentParser(); parser.add_argument('project');parser.add_argument('commands', nargs='+'); args=parser.parse_args()
    project=Path(args.project).resolve(); settings={}
    for line in (project/'run-server/server.properties').read_text().splitlines():
        if '=' in line and not line.startswith('#'):
            key,value=line.split('=',1);settings[key]=value
    with socket.create_connection(('127.0.0.1',int(settings['rcon.port'])),timeout=20) as sock:
        packet(sock,1,3,settings['rcon.password'])
        for index, command in enumerate(args.commands,2):
            print(command);print(packet(sock,index,2,command))
