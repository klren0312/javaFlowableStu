package com.example.flowable.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务DTO
 */
@Data
public class TaskDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 任务定义KEY
     */
    private String taskDefinitionKey;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 流程定义名称
     */
    private String processDefinitionName;

    /**
     * 任务受理人
     */
    private String assignee;

    /**
     * 任务候选人
     */
    private String candidateUsers;

    /**
     * 任务候选组
     */
    private String candidateGroups;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 任务优先级
     */
    private Integer priority;

    /**
     * 是否挂起
     */
    private Boolean suspended;
}