CREATE TABLE IF NOT EXISTS team (
    team_id CHAR(36) PRIMARY KEY COMMENT '小组唯一标识',
    team_name VARCHAR(50) NOT NULL UNIQUE COMMENT '小组名称',
    leader_id INT NOT NULL COMMENT '组长user_id（管理人员）',
    task_area GEOMETRY COMMENT '小组总任务区域（GPS多边形）',
    FOREIGN KEY (leader_id) REFERENCES user(user_id) ON DELETE CASCADE,
    INDEX idx_team_name (team_name),
    INDEX idx_leader_id (leader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小组信息表';