"""Local acceptance-server console; no third-party Python dependencies."""
import argparse
import socket
import struct


def command(text, port=25577):
    def receive(stream):
        def exact(size):
            chunks = bytearray()
            while len(chunks) < size:
                part = stream.recv(size - len(chunks))
                if not part:
                    raise ConnectionError("RCON closed the connection")
                chunks.extend(part)
            return bytes(chunks)
        size = struct.unpack("<i", exact(4))[0]
        if not 10 <= size <= 4 * 1024 * 1024:
            raise ValueError("Invalid RCON packet size")
        packet = exact(size)
        return struct.unpack("<ii", packet[:8]), packet[8:-2].decode("utf-8", errors="replace")

    def send(stream, ident, kind, body):
        payload = struct.pack("<ii", ident, kind) + body.encode("utf-8") + b"\0\0"
        stream.sendall(struct.pack("<i", len(payload)) + payload)

    with socket.create_connection(("127.0.0.1", port), timeout=30) as stream:
        send(stream, 1, 3, "tt-local-acceptance")
        header, _ = receive(stream)
        if header != (1, 2):
            raise PermissionError("RCON authentication failed")
        send(stream, 2, 2, text)
        header, response = receive(stream)
        if header[0] != 2:
            raise ConnectionError("Unexpected RCON response")
        return response


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", nargs="+")
    parser.add_argument("--port", type=int, default=25577)
    args = parser.parse_args()
    print(command(" ".join(args.command), args.port))
