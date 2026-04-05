@echo off
mysql -u root -proot pvadmin -e "SHOW CREATE TABLE device\G" 2>&1
