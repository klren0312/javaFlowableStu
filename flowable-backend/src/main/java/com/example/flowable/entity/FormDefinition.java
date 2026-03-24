package com.example.flowable.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 表单定义实体
 */
@Data
@TableName(value = "form_definition", autoResultMap = true)
public class FormDefinition {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 表单唯一标识
     */
    private String formKey;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 表单描述
     */
    private String description;

    /**
     * 表单字段定义(JSON格式)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> fields;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 状态(0:禁用,1:启用)
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标志(0:未删除,1:已删除)
     */
    @TableLogic
    private Integer deleted;
}