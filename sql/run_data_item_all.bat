@echo off
mysql -u root -proot --default-character-set=utf8mb4 pvadmin < e:\Pv\sql\data_item_all.sql
echo Done.
