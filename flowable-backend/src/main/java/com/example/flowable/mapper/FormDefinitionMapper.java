package com.example.flowable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.flowable.entity.FormDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表单定义Mapper
 */
@Mapper
public interface FormDefinitionMapper extends BaseMapper<FormDefinition> {
}