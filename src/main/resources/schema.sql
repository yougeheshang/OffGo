-- 检查并添加 video_id 列
SET @dbname = 'OffGo_database';
SET @tablename = 'diary';
SET @columnname = 'video_id';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE 
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " BIGINT")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 检查并添加 area_type 列
SET @columnname = 'area_type';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE 
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(10) CHECK (area_type IN ('校区', '景区'))")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 检查并添加外键约束
SET @constraintName = 'fk_diary_video';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE 
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (constraint_name = @constraintName)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD CONSTRAINT ", @constraintName, " FOREIGN KEY (", @columnname, ") REFERENCES video(id)")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 创建 content 表（如果不存在）
CREATE TABLE IF NOT EXISTS content (
    id INT AUTO_INCREMENT PRIMARY KEY,
    content TEXT NOT NULL
);

-- 创建 diary 表（如果不存在）
CREATE TABLE IF NOT EXISTS diary (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    content INT,
    image VARCHAR(255),
    hot INT DEFAULT 0,
    destination VARCHAR(255),
    rating DOUBLE DEFAULT 0,
    rate_sum INT DEFAULT 0,
    rate_counts INT DEFAULT 0,
    userid INT,
    video_id BIGINT,
    area_type VARCHAR(10) CHECK (area_type IN ('校区', '景区')),
    FOREIGN KEY (content) REFERENCES content(id),
    FOREIGN KEY (video_id) REFERENCES video(id)
);

-- 创建 video 表（如果不存在）
CREATE TABLE IF NOT EXISTS video (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_path VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100),
    original_name VARCHAR(255)
); 

CREATE TABLE IF NOT EXISTS map_location (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    type VARCHAR(100),
    crowd_level INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS map_road (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255),
    road_type VARCHAR(100),
    start_point_id BIGINT,
    end_point_id BIGINT,
    path_points TEXT,
    FOREIGN KEY (start_point_id) REFERENCES map_location(id),
    FOREIGN KEY (end_point_id) REFERENCES map_location(id)
); 