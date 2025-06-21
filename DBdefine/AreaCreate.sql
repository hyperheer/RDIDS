CREATE TABLE IF NOT EXISTS area (
    area_id VARCHAR(50) PRIMARY KEY COMMENT '区域编号',
    team_id CHAR(36) NOT NULL COMMENT '所属小组ID',
    user_id INT COMMENT '负责该区域的用户ID（非组长）',
    gps_data GEOMETRY SRID 4326 NOT NULL COMMENT 'GPS区域数据（多边形坐标）',
    status ENUM('未分配', '已分配') NOT NULL DEFAULT '未分配' COMMENT '区域状态',
    FOREIGN KEY (team_id) REFERENCES team(team_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE SET NULL,
    SPATIAL INDEX idx_gps_data (gps_data),
    INDEX idx_team_id (team_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务区域表';
