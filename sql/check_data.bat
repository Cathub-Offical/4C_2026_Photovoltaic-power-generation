@echo off
mysql -u root -proot pvadmin -e "SELECT 'device' as tbl, COUNT(*) as cnt FROM device UNION ALL SELECT 'alarm', COUNT(*) FROM alarm UNION ALL SELECT 'data_item', COUNT(*) FROM data_item UNION ALL SELECT 'electricity_data_item', COUNT(*) FROM electricity_data_item;"
