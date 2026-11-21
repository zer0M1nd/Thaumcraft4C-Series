from pathlib import Path
import json, socket, struct, sys, time
ROOT=Path(__file__).resolve().parents[1]
SERVER=ROOT/'run-server'
props=dict(line.split('=',1) for line in (SERVER/'server.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
def command(text):
    def read(sock):
        def exact(n):
            data=b''
            while len(data)<n:
                part=sock.recv(n-len(data))
                if not part: raise EOFError()
                data+=part
            return data
        size=struct.unpack('<i',exact(4))[0]
        data=exact(size)
        return struct.unpack('<ii',data[:8]),data[8:-2].decode('utf8')
    def send(sock,identity,kind,body):
        data=struct.pack('<ii',identity,kind)+body.encode()+b'\0\0'
        sock.sendall(struct.pack('<i',len(data))+data)
    with socket.create_connection(('127.0.0.1',int(props['rcon.port'])),30) as sock:
        send(sock,1,3,props['rcon.password']);header,_=read(sock)
        if header!=(1,2):raise RuntimeError('Local test RCON authentication failed')
        send(sock,2,2,text)
        return read(sock)[1]
if len(sys.argv)>1:
    for text in sys.argv[1:]: print(command(text))
    raise SystemExit()
names=['nodalOriginalMatrixAndAllAttunementSizes','nodalDynamicInfusionAndNetworkRoundTrip',
       'nodalCraftResumesWithoutRerollOrDuplication','nodalRealAltarCallsCraftHookAndPersistsRandomNode']
result=[];log=SERVER/'logs/latest.log'
for index,method in enumerate(names):
    offset=log.stat().st_size
    reply=command(f'execute positioned {1000+index*80} 100 1000 run test run {method.lower()}')
    print(reply,flush=True)
    started=time.monotonic();tail='';status='TIMEOUT'
    while time.monotonic()-started<45:
        with log.open('rb') as stream:
            stream.seek(offset);tail=stream.read().decode('utf8',errors='replace')
        if 'NODAL_PASS' in tail: status='PASS';break
        if ' failed' in tail.lower() or 'test failed' in tail.lower() or 'No such test' in reply:status='FAIL';break
        time.sleep(.2)
    result.append({'test':method,'status':status,'response':reply,'evidence':tail})
    (ROOT/'docs').mkdir(exist_ok=True)
    (ROOT/'docs/server-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
    print(method,status,flush=True)
raise SystemExit(0 if all(row['status']=='PASS' for row in result) else 1)
