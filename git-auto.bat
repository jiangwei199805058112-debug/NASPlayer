@echo off
cd /d E:\nas\NASPlayer
git add .
git commit -m "Auto commit from bat script"
git push origin main
echo ✅ 已完成提交并推送！
pause
