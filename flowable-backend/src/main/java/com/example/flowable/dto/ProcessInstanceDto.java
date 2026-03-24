package com.example.flowable.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程实例DTO
 */
@Data
public class ProcessInstanceDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程实例ID
     */
    private String id;

    /**
     * 流程定义ID
     */
    private String processDefinitionId;

    /**
     * 流程定义KEY
     */
    private String processDefinitionKey;

    /**
     * 流程定义名称
     */
    private String processDefinitionName;

    /**
     * 流程定义版本
     */
    private Integer processDefinitionVersion;

    /**
     * 发起人
     */
    private String startUserId;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 业务KEY
     */
    private String businessKey;

    /**
     * 是否结束
     */
    private Boolean ended;

    /**
     * 是否挂起
     */
    private Boolean suspended;
}