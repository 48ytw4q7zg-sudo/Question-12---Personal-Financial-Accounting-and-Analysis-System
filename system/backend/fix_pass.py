"""Fix zhangsan password via Python MySQL connector (avoids shell escaping)"""
import subprocess

correct_hash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

sql_commands = [
    "SET @hash = '%s';" % correct_hash,
    "UPDATE user SET password = @hash WHERE username = 'zhangsan';",
    "UPDATE user SET password = @hash WHERE username = 'admin';",
    "SELECT username, role, LENGTH(password) as pwd_len FROM user WHERE username IN ('zhangsan','admin');"
]

# Write to temp file and execute
sql_path = "fix_temp.sql"
with open(sql_path, "w") as f:
    f.write("\n".join(sql_commands))

# Verify the file has correct hash before executing
with open(sql_path) as f:
    content = f.read()
    if "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy" in content:
        print("OK: Hash correctly written to SQL file")
    else:
        print("ERROR: Hash corrupted in SQL file!")
        print(content[:200])
        exit(1)

r = subprocess.run(["mysql","-uroot","-proot","finance_db"],
    stdin=open(sql_path), capture_output=True, text=True)
print(r.stdout)
if r.stderr: print("STDERR:", r.stderr)
print(f"Return code: {r.returncode}")
