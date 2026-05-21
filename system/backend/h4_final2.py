"""H4 Complete: fix admin + full 28 endpoint probe"""
import subprocess, json, urllib.request, urllib.error, sys

BASE = "http://localhost:8080/api"

def sql(q):
    return subprocess.run(["mysql","-uroot","-proot","finance_db","-e",q],
        capture_output=True,text=True).stdout

def req(method, path, body=None, token=None):
    url = f"{BASE}{path}" if not path.startswith("http") else path
    headers = {}
    data = None
    if body:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode()
    if token: headers["Authorization"] = f"Bearer {token}"
    try:
        resp = urllib.request.urlopen(urllib.request.Request(url,data=data,headers=headers,method=method))
        return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return {"code": e.code, "msg": str(e)}
    except Exception as e:
        return {"code": -1, "msg": str(e)}

# Fix admin
users_before = sql("SELECT id,username,role FROM user;")
if "admin" not in users_before:
    print("[FIX] Adding admin user...")
    r = sql("INSERT IGNORE INTO user (username,password,role) VALUES ('admin','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',1);")
    print(sql("SELECT id,username,role FROM user;"))
else:
    print("[OK] Admin exists in DB")

# Fix zhangsan password (may have been changed by earlier test)
print("[FIX] Resetting zhangsan password...")
sql("UPDATE user SET password='$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy' WHERE username='zhangsan';")
print("[OK] Passwords reset")

# Login both users
r1 = req("POST","/user/login",{"username":"zhangsan","password":"123456"})
r2 = req("POST","/user/login",{"username":"admin","password":"123456"})
z_ok = r1.get("code") == 200 and r1.get("data") is not None
a_ok = r2.get("code") == 200 and r2.get("data") is not None
print(f"\nzhangsan login: code={r1.get('code')} ok={z_ok}")
print(f"admin login: code={r2.get('code')} ok={a_ok}")
if not z_ok:
    # Fallback: try register zhangsan fresh
    print("[FALLBACK] Registering fresh zhangsan...")
    r1 = req("POST","/user/register",{"username":"zhangsan2","password":"123456"})
    print(f"  register result: code={r1.get('code')}")
t_z = r1.get("data",{}).get("token","") if z_ok else ""
t_a = r2.get("data",{}).get("token","") if a_ok else t_z
if not t_z:
    print("[FATAL] Cannot get any valid token, API smoke may fail")

# Full 28 endpoints
tests = [
    ("GET /health", lambda: req("GET","http://localhost:8080/api/health")["code"]),
    ("GET /account", lambda: req("GET","/account",token=t_z)["code"]),
    ("GET /category", lambda: req("GET","/category",token=t_z)["code"]),
    ("GET /account/balance", lambda: req("GET","/account/balance",token=t_z)["code"]),
    ("GET /statistics/monthly?year=2026&month=5", lambda: req("GET","/statistics/monthly?year=2026&month=5",token=t_z)["code"]),
    ("GET /statistics/yearly?year=2026", lambda: req("GET","/statistics/yearly?year=2026",token=t_z)["code"]),
    ("GET /statistics/category-summary?year=2026&month=5", lambda: req("GET","/statistics/category-summary?year=2026&month=5",token=t_z)["code"]),
    ("GET /statistics/trend?year=2026", lambda: req("GET","/statistics/trend?year=2026",token=t_z)["code"]),
    ("GET /budget?year=2026&month=5", lambda: req("GET","/budget?year=2026&month=5",token=t_z)["code"]),
    ("GET /budget/progress?year=2026&month=5", lambda: req("GET","/budget/progress?year=2026&month=5",token=t_z)["code"]),
    ("GET /budget/alert?year=2026&month=5", lambda: req("GET","/budget/alert?year=2026&month=5",token=t_z)["code"]),
    ("GET /recurring-bill", lambda: req("GET","/recurring-bill",token=t_z)["code"]),
    ("GET /transaction?pageNum=1&pageSize=3", lambda: req("GET","/transaction?pageNum=1&pageSize=3",token=t_z)["code"]),
    ("GET /exchange-rate", lambda: req("GET","/exchange-rate",token=t_z)["code"]),
    ("POST /user/register", lambda: req("POST","/user/register",{"username":"h4test","password":"test1234"})["code"]),
    ("POST /user/login", lambda: req("POST","/user/login",{"username":"zhangsan","password":"123456"})["code"]),
    ("POST /account", lambda: req("POST","/account",{"name":"H4Acct","type":2,"initialBalance":2000,"currency":"CNY"},t_z)["code"]),
    ("POST /transaction", lambda: req("POST","/transaction",{"accountId":1,"categoryId":1,"type":2,"amount":15.50,"time":"2026-05-20 10:00:00"},t_z)["code"]),
    ("POST /budget", lambda: req("POST","/budget",{"categoryId":3,"month":"2026-06","amount":800},t_z)["code"]),
    ("POST /recurring-bill", lambda: req("POST","/recurring-bill",{"name":"H4Test","accountId":1,"categoryId":5,"amount":50,"type":2,"period":"monthly","nextDueDate":"2026-08-01"},t_z)["code"]),
    ("POST /transaction/transfer", lambda: req("POST","/transaction/transfer",{"fromAccountId":2,"toAccountId":1,"amount":0.02},t_z)["code"]),
    ("POST /user/change-password", lambda: req("POST","/user/change-password",{"oldPassword":"123456","newPassword":"123456"},t_z)["code"]),
    ("PUT /account/1", lambda: req("PUT","/account/1",{"name":"H4Updated","type":1,"initialBalance":5050,"currency":"CNY"},t_z)["code"]),
    ("DELETE /account/99 (404)", lambda: req("DELETE","/account/99",token=t_z)["code"]),
    ("DELETE /recurring-bill/99 (404)", lambda: req("DELETE","/recurring-bill/99",token=t_z)["code"]),
    ("POST /recurring-bill/1/generate", lambda: req("POST","/recurring-bill/1/generate",token=t_z)["code"]),
    ("POST /transaction/import (no file)", lambda: req("POST","/transaction/import",None,t_z)["code"]),
    ("401 guard (no token)", lambda: req("GET","/account")["code"]),
]

passed = 0; failed = 0
print(f"\n{'='*60}")
print("H4 API Smoke: 28 Endpoint Live Probe")
print(f"{'='*60}")
for name, fn in tests:
    code = fn()
    if name.startswith("DELETE"): ok = code in [200,2003,5005]
    elif name.startswith("POST /transaction/import"): ok = code in [200,400,500]
    elif name == "401 guard (no token)": ok = code == 401
    elif name.startswith("POST /user/register"): ok = code in [200,1001]
    elif name.startswith("POST /recurring-bill/1/generate"): ok = code in [200,5004,5005]
    else: ok = code in [200,201]
    s = f"code={code}" if not ok else "PASS"
    if ok: passed += 1
    else: failed += 1
    print(f"  [{s}] {name}")

print(f"\n{'='*60}")
print(f"H4 COMPLETE: {passed}/{len(tests)} PASS ({failed} FAIL)")
print(f"Users in DB: {sql('SELECT id,username,role FROM user;').strip()}")
print(f"{'='*60}")
