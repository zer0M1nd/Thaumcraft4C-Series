"""Send a command to this repository's localhost validation server only."""
from pathlib import Path
import socket, struct, sys

root = Path(__file__).resolve().parents[1]
properties = dict(line.split('=', 1) for line in (root / 'run-server/server.properties').read_text().splitlines()
                  if '=' in line and not line.startswith('#'))
if properties.get('server-ip') != '127.0.0.1':
    raise SystemExit('This helper is limited to the localhost validation server.')

def exact(stream, size):
    data = b''
    while len(data) < size:
        part = stream.recv(size - len(data))
        if not part:
            raise ConnectionError('RCON connection closed')
        data += part
    return data

def exchange(stream, request_id, kind, text):
    payload = struct.pack('<ii', request_id, kind) + text.encode('utf-8') + b'\0\0'
    stream.sendall(struct.pack('<i', len(payload)) + payload)
    length, = struct.unpack('<i', exact(stream, 4))
    if length < 10 or length > 4 * 1024 * 1024:
        raise ValueError('Invalid RCON response size')
    reply = exact(stream, length)
    response_id, _ = struct.unpack('<ii', reply[:8])
    if response_id != request_id:
        raise PermissionError('RCON authentication or request failed')
    return reply[8:-2].decode('utf-8', errors='replace')

with socket.create_connection(('127.0.0.1', int(properties['rcon.port'])), timeout=20) as stream:
    exchange(stream, 1, 3, properties['rcon.password'])
    print(exchange(stream, 2, 2, ' '.join(sys.argv[1:])))
