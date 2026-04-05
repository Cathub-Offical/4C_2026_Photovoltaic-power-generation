@echo off
mysql -u root -proot --default-character-set=utf8mb4 pvadmin -e "SELECT id, device_code, error_description FROM alarm LIMIT 5;"
