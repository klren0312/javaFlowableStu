package com.example.flowable.config;

import org.flowable.app.spring.SpringAppEngineConfiguration;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable配置类
 * 用于配置流程引擎的相关属性
 */
@Configuration
public class FlowableConfig {

    /**
     * 配置流程引擎
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> processEngineConfigurer() {
        return engineConfiguration -> {
            // 不自动更新数据库Schema，避免重复创建表
            engineConfiguration.setDatabaseSchemaUpdate(SpringProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
            // 设置历史记录级别为完整
            engineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            // 禁用异步执行器，确保任务完成时同步写入历史记录
            engineConfiguration.setAsyncExecutorActivate(false);
        };
    }

    /**
     * 配置App引擎
     */
    @Bean
    public EngineConfigurationConfigurer<SpringAppEngineConfiguration> appEngineConfigurer() {
        return appEngineConfiguration -> {
            // 不自动更新数据库Schema，避免重复创建表
            appEngineConfiguration.setDatabaseSchemaUpdate(SpringAppEngineConfiguration.DB_SCHEMA_UPDATE_FALSE);
        };
    }
}