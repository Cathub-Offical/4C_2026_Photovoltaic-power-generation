@echo off
mysql -u root -proot pvadmin -e "SELECT id, device_code, level, status, error_description FROM alarm LIMIT 5;"
