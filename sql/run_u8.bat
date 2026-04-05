@echo off
mysql -u root -proot --default-character-set=utf8mb4 pvadmin < e:\Pv\sql\update_factory_u8.sql
echo Done.
