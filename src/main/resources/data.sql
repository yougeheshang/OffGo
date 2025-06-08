-- 初始化管理员账户
INSERT INTO users (id, name, password, role) 
VALUES (1, 'admin123', 'admin123', 'ADMIN')
ON DUPLICATE KEY UPDATE 
    name = 'admin123',
    password = 'admin123',
    role = 'ADMIN'; 
    rate_id = {};
    rate_value = {};

-- 更新现有景点的区域类型
UPDATE diary 
SET area_type = CASE 
    WHEN RAND() < 0.5 THEN '校区'
    ELSE '景区'
END
WHERE area_type IS NULL;

-- 检查区域类型分布
SELECT area_type, COUNT(*) as count 
FROM diary 
GROUP BY area_type;

-- 确保所有景点都有区域类型
UPDATE diary 
SET area_type = '校区' 
WHERE area_type IS NULL;