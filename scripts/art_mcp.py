"""Small HTTP client for the configured local Blockbench MCP authoring session."""
import json
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / '.tooling/visual-redesign-reference/mcp-session.txt'
URL = 'http://localhost:3000/bb-mcp'

def rpc(method, params, session=None):
    headers = {'Content-Type': 'application/json', 'Accept': 'application/json, text/event-stream'}
    if session:
        headers['mcp-session-id'] = session
    request = urllib.request.Request(URL, json.dumps({'jsonrpc':'2.0','id':1,'method':method,'params':params}).encode(), headers)
    with urllib.request.urlopen(request, timeout=60) as response:
        data = json.load(response)
        return data, response.headers.get('mcp-session-id')

def call(tool_name, **arguments):
    session = STATE.read_text().strip() if STATE.exists() else None
    if not session:
        _, session = rpc('initialize', {'protocolVersion':'2024-11-05','capabilities':{},'clientInfo':{'name':'TE-resource-authoring','version':'1.0'}})
        STATE.write_text(session)
    data, _ = rpc('tools/call', {'name': tool_name, 'arguments': arguments}, session)
    if 'error' in data or data.get('result',{}).get('isError'):
        raise RuntimeError(json.dumps(data))
    return data['result']

if __name__ == '__main__':
    import sys
    if sys.argv[1] == 'list':
        data, _ = rpc('tools/list', {}, STATE.read_text().strip())
        (ROOT / '.tooling/visual-redesign-reference/blockbench-tools.json').write_text(json.dumps(data,indent=2), encoding='utf8')
        print('\n'.join(t['name'] for t in data['result']['tools']))
    else:
        print(json.dumps(call(sys.argv[1], **json.loads(sys.argv[2] if len(sys.argv)>2 else '{}')), ensure_ascii=False))
