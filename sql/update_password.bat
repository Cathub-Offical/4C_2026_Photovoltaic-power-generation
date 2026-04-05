@echo off
mysql -u root -proot pvadmin -e "UPDATE sys_user SET password='admin123' WHERE user_name='admin'; UPDATE sys_user SET password='guest123' WHERE user_name='guestUser'; SELECT user_name, password FROM sys_user WHERE user_name IN ('admin','guestUser');"
