$root = 'C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-main'
$logOut = 'C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-main\logs\backend-startup.log'
$logErr = 'C:\Users\Administrator\Desktop\Question-12---Personal-Financial-Accounting-and-Analysis-System-main\logs\backend-error.log'
Remove-Item $logOut,$logErr -ErrorAction SilentlyContinue
Start-Process -FilePath 'cmd.exe' -ArgumentList '/c','mvn.cmd -B spring-boot:run' -WorkingDirectory "$root\system\backend" -WindowStyle Minimized -RedirectStandardOutput $logOut -RedirectStandardError $logErr
