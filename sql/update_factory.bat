@echo off
chcp 65001 > nul
mysql -u root -proot pvadmin --default-character-set=utf8 -e "UPDATE device SET factory='启明星辰' WHERE ammeter=0; SELECT id, name, factory FROM device WHERE ammeter=0 LIMIT 5;"
