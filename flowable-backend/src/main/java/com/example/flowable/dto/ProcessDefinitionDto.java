package com.example.flowable.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程定义DTO
 */
@Data
public class ProcessDefinitionDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义ID
     */
    private String id;

    /**
     * 流程定义KEY
     */
    private String key;

    /**
     * 流程定义名称
     */
    private String name;

    /**
     * 流程定义版本
     */
    private Integer version;

    /**
     * 部署ID
     */
    private String deploymentId;

    /**
     * 流程定义描述
     */
    private String description;

    /**
     * 部署时间
     */
    private Date deploymentTime;

    /**
     * 是否挂起
     */
    private Boolean suspended;
}