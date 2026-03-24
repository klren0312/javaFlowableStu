-- ============================================
-- Flowable 工作流系统初始数据
-- ============================================

USE flowable_test;

-- ============================================
-- 初始化部门数据
-- ============================================
INSERT INTO sys_department (id, name, parent_id, sort, leader, phone, email, status) VALUES
(1, '总公司', 0, 1, '管理员', '010-12345678', 'admin@company.com', 1),
(2, '技术部', 1, 1, '张三', '010-12345679', 'tech@company.com', 1),
(3, '产品部', 1, 2, '李四', '010-12345680', 'product@company.com', 1),
(4, '市场部', 1, 3, '王五', '010-12345681', 'market@company.com', 1),
(5, '财务部', 1, 4, '赵六', '010-12345682', 'finance@company.com', 1),
(6, '人事部', 1, 5, '钱七', '010-12345683', 'hr@company.com', 1),
(7, '研发一组', 2, 1, '孙八', '010-12345684', 'dev1@company.com', 1),
(8, '研发二组', 2, 2, '周九', '010-12345685', 'dev2@company.com', 1);

-- ============================================
-- 初始化角色数据
-- ============================================
INSERT INTO sys_role (id, name, code, description, status) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '系统超级管理员，拥有所有权限', 1),
(2, '管理员', 'ADMIN', '系统管理员', 1),
(3, '部门经理', 'DEPT_MANAGER', '部门经理，可以审批本部门流程', 1),
(4, '普通员工', 'EMPLOYEE', '普通员工，可以发起和处理任务', 1),
(5, '财务审批', 'FINANCE_APPROVER', '财务审批人员', 1),
(6, '人事审批', 'HR_APPROVER', '人事审批人员', 1);

-- ============================================
-- 初始化用户数据
-- 密码均为: 123456 (使用BCrypt加密)
-- ============================================
INSERT INTO sys_user (id, username, password, real_name, email, phone, department_id, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '超级管理员', 'admin@company.com', '13800000001', 1, 1),
(2, 'zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张三', 'zhangsan@company.com', '13800000002', 2, 1),
(3, 'lisi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李四', 'lisi@company.com', '13800000003', 3, 1),
(4, 'wangwu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王五', 'wangwu@company.com', '13800000004', 4, 1),
(5, 'zhaoliu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵六', 'zhaoliu@company.com', '13800000005', 5, 1),
(6, 'qianqi', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '钱七', 'qianqi@company.com', '13800000006', 6, 1),
(7, 'sunba', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '孙八', 'sunba@company.com', '13800000007', 7, 1),
(8, 'zhoujiu', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '周九', 'zhoujiu@company.com', '13800000008', 8, 1);

-- ============================================
-- 初始化用户角色关联数据
-- ============================================
INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),  -- admin -> 超级管理员
(1, 2),  -- admin -> 管理员
(2, 2),  -- zhangsan -> 管理员
(2, 3),  -- zhangsan -> 部门经理
(3, 3),  -- lisi -> 部门经理
(4, 3),  -- wangwu -> 部门经理
(5, 5),  -- zhaoliu -> 财务审批
(6, 6),  -- qianqi -> 人事审批
(7, 4),  -- sunba -> 普通员工
(8, 4);  -- zhoujiu -> 普通员工