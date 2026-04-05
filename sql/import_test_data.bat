@echo off
chcp 65001 > nul
mysql -u root -proot pvadmin < e:\Pv\sql\test_data.sql
echo Import finished with exit code: %errorlevel%
