package com.example.flowable.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 表单实例数据实体
 */
@Data
@TableName(value = "form_instance", autoResultMap = true)
public class FormInstance {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 任务ID(启动表单为空)
     */
    private String taskId;

    /**
     * 表单Key
     */
    private String formKey;

    /**
     * 表单数据(JSON格式)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> formData;

    /**
     * 提交人
     */
    private String submitter;

    /**
     * 提交时间
     */
    private LocalDateTime submitTime;
}