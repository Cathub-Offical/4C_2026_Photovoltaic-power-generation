@echo off
mysql -u root -proot --default-character-set=utf8mb4 pvadmin -e "SELECT id, name FROM power_station LIMIT 20;"
