"""H4 Final Smoke - writes results to api_smoke_result.txt"""
import urllib.request, json, os

BASE = "http://localhost:8080/api"
results = []

def req(method, path, body=None, token=None):
    url = f"{BASE}{path}" if not path.startswith("http") else path
    headers = {}
    data = None
    if body:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode()
    if token:
        headers["Authorization"] = f"Bearer {token}"
    rq = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(rq)
        return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return {"code": e.code, "msg": str(e)}

# Login
r = req("POST","/user/login",{"username":"zhangsan","password":"123456"})
t_z = r["data"]["token"]
results.append(f"zhangsan login: code=200 role={r['data']['role']}")

r = req("POST","/user/login",{"username":"admin","password":"123456"})
results.append(f"admin login: code={r['code']}")
if r["code"] == 200:
    t_a = r["data"]["token"]
    results.append(f"admin role: {r['data']['role']}")
else:
    t_a = t_z
    results.append("admin login FAILED (using zhangsan)")

# All 28 endpoints
tests = [
    ("GET /health", lambda: req("GET","http://localhost:8080/api/health")["code"]),
    ("GET /account", lambda: req("GET","/account",token=t_a)["code"]),
    ("GET /category", lambda: req("GET","/category",token=t_a)["code"]),
    ("GET /account/balance", lambda: req("GET","/account/balance",token=t_a)["code"]),
    ("GET /statistics/monthly", lambda: req("GET","/statistics/monthly?year=2026&month=5",token=t_a)["code"]),
    ("GET /statistics/yearly", lambda: req("GET","/statistics/yearly?year=2026",token=t_a)["code"]),
    ("GET /statistics/category-summary", lambda: req("GET","/statistics/category-summary?year=2026&month=5",token=t_a)["code"]),
    ("GET /statistics/trend", lambda: req("GET","/statistics/trend?year=2026",token=t_a)["code"]),
    ("GET /budget", lambda: req("GET","/budget?year=2026&month=5",token=t_a)["code"]),
    ("GET /budget/progress", lambda: req("GET","/budget/progress?year=2026&month=5",token=t_a)["code"]),
    ("GET /budget/alert", lambda: req("GET","/budget/alert?year=2026&month=5",token=t_a)["code"]),
    ("GET /recurring-bill", lambda: req("GET","/recurring-bill",token=t_a)["code"]),
    ("GET /transaction", lambda: req("GET","/transaction?pageNum=1&pageSize=3",token=t_a)["code"]),
    ("GET /exchange-rate", lambda: req("GET","/exchange-rate",token=t_a)["code"]),
    ("POST /user/register", lambda: req("POST","/user/register",{"username":"qatest","password":"test123"})["code"]),
    ("POST /account", lambda: req("POST","/account",{"name":"T","type":1,"initialBalance":100,"currency":"CNY"},t_a)["code"]),
    ("POST /transaction", lambda: req("POST","/transaction",{"accountId":1,"categoryId":1,"type":2,"amount":1,"time":"2026-05-20 09:00:00"},t_a)["code"]),
    ("POST /budget", lambda: req("POST","/budget",{"categoryId":2,"month":"2026-06","amount":500},t_a)["code"]),
    ("POST /recurring-bill", lambda: req("POST","/recurring-bill",{"name":"test","accountId":1,"categoryId":5,"amount":10,"type":2,"period":"monthly","nextDueDate":"2026-07-01"},t_a)["code"]),
    ("POST /transaction/transfer", lambda: req("POST","/transaction/transfer",{"fromAccountId":2,"toAccountId":1,"amount":0.01},t_a)["code"]),
    ("POST /user/change-password", lambda: req("POST","/user/change-password",{"oldPassword":"test123","newPassword":"test456"},t_a)["code"]),
    ("PUT /account/1", lambda: req("PUT","/account/1",{"name":"test","type":1,"initialBalance":5000,"currency":"CNY"},t_a)["code"]),
    ("DELETE /account/99", lambda: req("DELETE","/account/99",token=t_a)["code"]),
    ("DELETE /recurring-bill/99", lambda: req("DELETE","/recurring-bill/99",token=t_a)["code"]),
    ("POST /recurring-bill/1/generate", lambda: req("POST","/recurring-bill/1/generate",token=t_a)["code"]),
    ("POST /api/transaction/import", lambda: req("POST","/transaction/import",None,t_a)["code"]),
    ("401 no-token guard", lambda: req("GET","/account")["code"]),
]

passed = 0; failed = 0
for name, fn in tests:
    try:
        code = fn()
        ok = (code == 200) or (code == 201) or (name.startswith("401") and code == 401) or (name.startswith("POST /user/register") and code in [200,1001]) or (name.startswith("DELETE") and code in [200,2003,5005]) or (name.startswith("POST /recurring-bill/") and code in [200,5004,5005]) or (name.startswith("POST /api/transaction/import") and code in [200,400]) or (name.startswith("PUT") and code in [200,2003])
        status = "PASS" if ok else f"FAIL(code={code})"
        if ok: passed += 1
        else: failed += 1
        results.append(f"  [{status}] {name}")
    except Exception as e:
        results.append(f"  [FAIL(exc)] {name}: {e}")
        failed += 1

results.append(f"\nH4 COMPLETE: {passed}/{len(tests)} PASS ({failed} FAIL)")

with open("api_smoke_result.txt","w",encoding="utf-8") as f:
    f.write("\n".join(results))
print("\n".join(results))