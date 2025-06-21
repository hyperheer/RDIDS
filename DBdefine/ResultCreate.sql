CREATE TABLE IF NOT EXISTS detection_result (
    result_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '上传结果的用户ID',
    area_id VARCHAR(50) COMMENT '关联的区域编号',
    image_path VARCHAR(200) NOT NULL COMMENT '检测图片存储路径',
    gps_location VARCHAR(100) NOT NULL COMMENT 'GPS位置（经纬度坐标）',
    detection_time DATETIME NOT NULL COMMENT '检测时间',
    defect_type VARCHAR(50) NOT NULL COMMENT '缺陷类型（如裂缝、坑洼）',
    accuracy DECIMAL(5,2) NOT NULL COMMENT '检测精度',
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (area_id) REFERENCES area(area_id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_detection_time (detection_time),
    INDEX idx_area_id (area_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检测结果表';