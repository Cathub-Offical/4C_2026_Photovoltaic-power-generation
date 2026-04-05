INSERT IGNORE INTO power_station (id, parent_id, code, name, subsidized_prices, installed_capacity, grid_voltage, lon, lat, owning_user_id, remark, create_time, create_by, update_time, update_by, user_id, dept_id)
VALUES ('1000000000000000001', NULL, 'DZ1', '电站1', 0.42, 5.2000, 250.00, 116.4074, 39.9042, NULL, '北京示范电站', '2023-08-01 09:00:00', 'admin', '2025-02-17 14:00:00', NULL, NULL, 200);

INSERT IGNORE INTO device (id, power_station_id, code, name, device_type_id, capacity, factory, rated_ac_power, grid_type, module_peak_power, ammeter, remark, create_time, create_by)
VALUES
('dev1s1', '1000000000000000001', 'DZ1-INV-001', '电站1逆变器1', '1698627923435794433', 50.00, '启明星辰', 50.00, '三相', 400.00, 0, NULL, '2024-01-01 08:00:00', 'admin'),
('dev1s2', '1000000000000000001', 'DZ1-INV-002', '电站1逆变器2', '1698627923435794433', 50.00, '启明星辰', 50.00, '三相', 400.00, 0, NULL, '2024-01-01 08:00:00', 'admin'),
('dev1s3', '1000000000000000001', 'DZ1-MTR-001', '电站1电表1', '1698627923435794434', 0.00, '海科', 0.00, '三相', NULL, 1, NULL, '2024-01-01 08:00:00', 'admin');