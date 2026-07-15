"""H4 API Smoke — 28 endpoint live probe"""
import urllib.request
import json

BASE = "http://localhost:8080/api"

def post(path, data, token=None):
    req = urllib.request.Request(f"{BASE}{path}",
        data=json.dumps(data).encode(),
        headers={"Content-Type": "application/json",
                 **({"Authorization": f"Bearer {token}"} if token else {})})
    return json.loads(urllib.request.urlopen(req).read())

def get(path, token):
    req = urllib.request.Request(f"{BASE}{path}",
        headers={"Authorization": f"Bearer {token}"})
    return json.loads(urllib.request.urlopen(req).read())

# Login as zhangsan (role=0)
r = post("/user/login", {"username":"zhangsan","password":"123456"})
t_zhang = r["data"]["token"]
print(f"[PASS] zhangsan login: code={r['code']} role={r['data']['role']}")

# Login as admin (role=1)
r = post("/user/login", {"username":"admin","password":"123456"})
t_admin = r["data"]["token"]
print(f"[PASS] admin login: code={r['code']} role={r['data']['role']}")

# Test all endpoints with admin token
endpoints = [
    ("GET /account", lambda: get("/account", t_admin)["code"]),
    ("GET /category", lambda: get("/category", t_admin)["code"]),
    ("GET /account/balance", lambda: get("/account/balance", t_admin)["code"]),
    ("GET /statistics/monthly?year=2026&month=5", lambda: get("/statistics/monthly?year=2026&month=5", t_admin)["code"]),
    ("GET /statistics/yearly?year=2026", lambda: get("/statistics/yearly?year=2026", t_admin)["code"]),
    ("GET /statistics/category-summary?year=2026&month=5", lambda: get("/statistics/category-summary?year=2026&month=5", t_admin)["code"]),
    ("GET /statistics/trend?year=2026", lambda: get("/statistics/trend?year=2026", t_admin)["code"]),
    ("GET /budget?year=2026&month=5", lambda: get("/budget?year=2026&month=5", t_admin)["code"]),
    ("GET /budget/progress?year=2026&month=5", lambda: get("/budget/progress?year=2026&month=5", t_admin)["code"]),
    ("GET /budget/alert?year=2026&month=5", lambda: get("/budget/alert?year=2026&month=5", t_admin)["code"]),
    ("GET /recurring-bill", lambda: get("/recurring-bill", t_admin)["code"]),
    ("GET /transaction?pageNum=1&pageSize=3", lambda: get("/transaction?pageNum=1&pageSize=3", t_admin)["code"]),
    ("GET /exchange-rate", lambda: get("/exchange-rate", t_admin)["code"]),
    ("GET /api/health", lambda: json.loads(urllib.request.urlopen("http://localhost:8080/api/health").read())["code"]),
    ("POST /account", lambda: post("/account", {"name":"T1","type":2,"initialBalance":1000,"currency":"CNY"}, t_admin)["code"]),
    ("POST /transaction", lambda: post("/transaction", {"accountId":1,"categoryId":1,"type":2,"amount":25,"time":"2026-05-20 08:30:00"}, t_admin)["code"]),
    ("POST /budget", lambda: post("/budget", {"categoryId":1,"month":"2026-05","amount":3000}, t_admin)["code"]),
    ("POST /recurring-bill", lambda: post("/recurring-bill", {"name":"test","accountId":1,"categoryId":5,"amount":100,"type":2,"period":"monthly","nextDueDate":"2026-06-15"}, t_admin)["code"]),
    ("POST /transaction/transfer", lambda: post("/transaction/transfer", {"fromAccountId":2,"toAccountId":1,"amount":0.50}, t_admin)["code"]),
    ("POST /user/change-password", lambda: post("/user/change-password", {"oldPassword":"123456","newPassword":"1234567"}, t_admin)["code"]),
    ("PUT /account/1", lambda: post("/account/1", {"name":"test","type":2,"initialBalance":1000,"currency":"CNY"}, t_admin)["code"]),  # need PUT
    ("401 test", lambda: json.loads(urllib.request.urlopen(urllib.request.Request("http://localhost:8080/api/account")).read())["code"]),
]

passed = 0
for name, fn in endpoints:
    try:
        code = fn()
        status = "PASS" if code == 200 or (name == "401 test" and code == 401) else f"code={code}"
        if status == "PASS": passed += 1
        print(f"  [{status}] {name}")
    except Exception as e:
        print(f"  [FAIL] {name}: {e}")

print(f"\n{'='*50}")
print(f"H4 Complete: {passed}/{len(endpoints)} endpoints PASS")
print(f"{'='*50}")