@echo off
mysql -u root -proot --default-character-set=utf8mb4 pvadmin -e "SELECT id, name, factory FROM device LIMIT 8;"
