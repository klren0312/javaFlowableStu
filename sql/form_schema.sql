-- =============================================
-- 表单定义相关表
-- =============================================

-- 表单定义表
DROP TABLE IF EXISTS form_definition;
CREATE TABLE form_definition (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '表单ID',
    form_key VARCHAR(100) NOT NULL COMMENT '表单唯一标识',
    form_name VARCHAR(100) NOT NULL COMMENT '表单名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '表单描述',
    fields JSON NOT NULL COMMENT '表单字段定义(JSON格式)',
    version INT DEFAULT 1 COMMENT '版本号',
    status TINYINT DEFAULT 1 COMMENT '状态(0:禁用,1:启用)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志(0:未删除,1:已删除)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_form_key (form_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单定义表';

-- 流程表单关联表
DROP TABLE IF EXISTS process_form_relation;
CREATE TABLE process_form_relation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    process_definition_id VARCHAR(64) DEFAULT NULL COMMENT '流程定义ID',
    process_definition_key VARCHAR(100) NOT NULL COMMENT '流程定义Key',
    node_id VARCHAR(100) DEFAULT '' COMMENT '流程节点ID(为空表示全局表单)',
    form_key VARCHAR(100) NOT NULL COMMENT '表单Key',
    form_type VARCHAR(50) DEFAULT 'start' COMMENT '表单类型(start:启动表单, task:任务表单)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_process_node (process_definition_key, node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程表单关联表';

-- 表单实例数据表（存储流程实例的表单数据）
DROP TABLE IF EXISTS form_instance;
CREATE TABLE form_instance (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    process_instance_id VARCHAR(64) NOT NULL COMMENT '流程实例ID',
    task_id VARCHAR(64) DEFAULT NULL COMMENT '任务ID(启动表单为空)',
    form_key VARCHAR(100) NOT NULL COMMENT '表单Key',
    form_data JSON NOT NULL COMMENT '表单数据(JSON格式)',
    submitter VARCHAR(50) DEFAULT NULL COMMENT '提交人',
    submit_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (id),
    KEY idx_process_instance (process_instance_id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单实例数据表';

-- 初始化示例表单
INSERT INTO form_definition (form_key, form_name, description, fields, status) VALUES
('leave_form', '请假申请表', '员工请假申请表单', 
 '[{"id":"field_1","type":"user","name":"applicant","label":"申请人","required":true,"readonly":true},{"id":"field_2","type":"department","name":"department","label":"所属部门","required":true},{"id":"field_3","type":"select","name":"leaveType","label":"请假类型","required":true,"options":[{"label":"事假","value":"1"},{"label":"病假","value":"2"},{"label":"年假","value":"3"},{"label":"调休","value":"4"}]},{"id":"field_4","type":"datetime","name":"startTime","label":"开始时间","required":true},{"id":"field_5","type":"datetime","name":"endTime","label":"结束时间","required":true},{"id":"field_6","type":"number","name":"days","label":"请假天数","required":true,"min":0.5,"max":30},{"id":"field_7","type":"textarea","name":"reason","label":"请假原因","required":true,"maxLength":500}]', 
 1);
