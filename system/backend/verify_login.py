import urllib.request, json
def login(u):
    r = urllib.request.Request("http://localhost:8080/api/user/login",
        data=json.dumps({"username":u,"password":"123456"}).encode(),
        headers={"Content-Type":"application/json"})
    d = json.loads(urllib.request.urlopen(r).read())
    role = d.get('data') and d['data'].get('role') or 'N/A'
    token_ok = d.get('data') and len(d['data'].get('token','')) > 10
    print(f"{u}: code={d['code']} role={role} token={'OK' if token_ok else 'NONE'}")
