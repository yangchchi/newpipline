DELETE FROM zh_property;

INSERT INTO zh_property (id, name, city, region, status, main_layout, avg_price, address, longitude, latitude, data_update_time, create_time, update_time)
VALUES
(1, N'中海·寰宇天下', N'A市', N'高新区', 'SALE', N'90㎡三房,120㎡四房', 85000.00, N'高新区科创大道88号', 116.48000000, 39.99000000, '2026-04-01 10:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, N'中海·云筑', N'A市', N'高新区', 'SALE', N'105㎡三房', 78000.00, N'高新区云锦路66号', 116.48100000, 39.99100000, '2026-04-02 11:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, N'中海·悦府', N'A市', N'高新区', 'SOLD_OUT', N'128㎡四房', NULL, N'高新区悦景街12号', 116.47950000, 39.98950000, '2026-03-15 09:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, N'中海·观澜', N'B市', N'滨江区', 'CONSTRUCTION', N'95㎡三房', 72000.00, N'滨江观澜路200号', 120.20000000, 30.25000000, '2026-04-10 08:00:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
