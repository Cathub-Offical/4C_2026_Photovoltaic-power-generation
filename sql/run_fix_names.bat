@echo off
mysql -u root -proot --default-character-set=utf8mb4 pvadmin < e:\Pv\sql\fix_names.sql
echo Done.
