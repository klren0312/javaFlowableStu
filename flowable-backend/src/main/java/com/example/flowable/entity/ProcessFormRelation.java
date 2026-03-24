package com.example.flowable.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程表单关联实体
 */
@Data
@TableName("process_form_relation")
public class ProcessFormRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 流程定义Key
     */
    private String processDefinitionKey;

    /**
     * 流程节点ID(为空表示全局表单/启动表单)
     */
    private String nodeId;

    /**
     * 表单Key
     */
    private String formKey;

    /**
     * 表单类型(start:启动表单, task:任务表单)
     */
    private String formType;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}