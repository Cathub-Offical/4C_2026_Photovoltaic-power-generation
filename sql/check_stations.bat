@echo off
mysql -u root -proot --default-character-set=utf8mb4 pvadmin -e "SELECT id, name, address FROM power_station ORDER BY create_time LIMIT 20;"
