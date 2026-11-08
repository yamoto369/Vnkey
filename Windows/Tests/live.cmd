@echo off
rem Dev loop in the VM (start from Win+R: Z:\Vikey\Windows\Tests\live.cmd): restart the freshly built Vikey, run the live test.
taskkill /IM Vikey.exe /F >nul 2>&1
start "" C:\vk\build\App\Vikey.exe
timeout /t 3 /nobreak >nul
call Z:\Vikey\Windows\Tests\run_livetest.cmd
