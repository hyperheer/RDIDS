CREATE TABLE IF NOT EXISTS user (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    user_type TINYINT NOT NULL COMMENT '用户类型：0=作业人员，1=管理员',
    name VARCHAR(20) COMMENT '姓名',
    gender ENUM('男', '女') COMMENT '性别',
    phone VARCHAR(11) UNIQUE COMMENT '手机号',
    INDEX idx_user_type (user_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';