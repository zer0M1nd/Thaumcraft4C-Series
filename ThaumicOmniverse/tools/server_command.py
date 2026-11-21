import pathlib,socket,struct,sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
profile,command=sys.argv[1],sys.argv[2]
assert profile in ('run-empty','run-compat','run-full')
props=dict(line.split('=',1) for line in (ROOT/profile/'server.properties').read_text().splitlines() if '=' in line)
def packet(sock,id,kind,value):
 raw=struct.pack('<ii',id,kind)+value.encode()+b'\0\0';sock.sendall(struct.pack('<i',len(raw))+raw)
def read(sock):
 def n(count):
  data=b''
  while len(data)<count:
   chunk=sock.recv(count-len(data))
   if not chunk:raise EOFError('RCON closed')
   data+=chunk
  return data
 length=struct.unpack('<i',n(4))[0];data=n(length);return struct.unpack('<ii',data[:8]),data[8:-2].decode(errors='replace')
with socket.create_connection(('127.0.0.1',int(props['rcon.port'])),timeout=15) as sock:
 packet(sock,1,3,props['rcon.password']);header,text=read(sock)
 if header[1]==0:header,text=read(sock)
 if header[0]!=1:raise RuntimeError('RCON auth failed')
 packet(sock,2,2,command)
 try:print(read(sock)[1])
 except EOFError:
  if command!='stop':raise
