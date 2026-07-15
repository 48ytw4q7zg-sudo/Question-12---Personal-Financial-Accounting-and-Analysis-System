$root = 'C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-main'
$logOut = 'C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-main\logs\frontend-startup.log'
$logErr = 'C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-main\logs\frontend-error.log'
Remove-Item $logOut,$logErr -ErrorAction SilentlyContinue
Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','pnpm.CMD dev' -WorkingDirectory "$root\system\frontend" -WindowStyle Minimized -RedirectStandardOutput $logOut -RedirectStandardError $logErr
