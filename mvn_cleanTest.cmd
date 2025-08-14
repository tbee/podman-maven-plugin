if exist javaHome.cmd (
    call javaHome.cmd
)
call mvnw.cmd clean test 
pause
