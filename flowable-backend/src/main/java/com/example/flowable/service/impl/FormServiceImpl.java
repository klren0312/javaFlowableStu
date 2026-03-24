package com.example.flowable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.flowable.entity.FormDefinition;
import com.example.flowable.entity.FormInstance;
import com.example.flowable.entity.ProcessFormRelation;
import com.example.flowable.mapper.FormDefinitionMapper;
import com.example.flowable.mapper.FormInstanceMapper;
import com.example.flowable.mapper.ProcessFormRelationMapper;
import com.example.flowable.service.FormService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 表单服务实现
 */
@Service
@RequiredArgsConstructor
public class FormServiceImpl implements FormService {

    private final FormDefinitionMapper formDefinitionMapper;
    private final ProcessFormRelationMapper processFormRelationMapper;
    private final FormInstanceMapper formInstanceMapper;

    // ==================== 表单定义相关 ====================

    @Override
    @Transactional
    public FormDefinition createForm(FormDefinition formDefinition) {
        // 检查formKey是否已存在
        FormDefinition existing = getFormByKey(formDefinition.getFormKey());
        if (existing != null) {
            throw new RuntimeException("表单标识已存在: " + formDefinition.getFormKey());
        }
        formDefinition.setVersion(1);
        formDefinition.setStatus(1);
        formDefinitionMapper.insert(formDefinition);
        return formDefinition;
    }

    @Override
    @Transactional
    public FormDefinition updateForm(FormDefinition formDefinition) {
        FormDefinition existing = formDefinitionMapper.selectById(formDefinition.getId());
        if (existing == null) {
            throw new RuntimeException("表单不存在");
        }
        // 如果formKey变更，检查是否与其他表单冲突
        if (!existing.getFormKey().equals(formDefinition.getFormKey())) {
            FormDefinition byKey = getFormByKey(formDefinition.getFormKey());
            if (byKey != null && !byKey.getId().equals(formDefinition.getId())) {
                throw new RuntimeException("表单标识已存在: " + formDefinition.getFormKey());
            }
        }
        // 版本号+1
        formDefinition.setVersion(existing.getVersion() + 1);
        formDefinitionMapper.updateById(formDefinition);
        return formDefinition;
    }

    @Override
    @Transactional
    public void deleteForm(Long id) {
        formDefinitionMapper.deleteById(id);
    }

    @Override
    public FormDefinition getFormById(Long id) {
        return formDefinitionMapper.selectById(id);
    }

    @Override
    public FormDefinition getFormByKey(String formKey) {
        return formDefinitionMapper.selectOne(
                new LambdaQueryWrapper<FormDefinition>()
                        .eq(FormDefinition::getFormKey, formKey)
        );
    }

    @Override
    public List<FormDefinition> getFormList() {
        return formDefinitionMapper.selectList(
                new LambdaQueryWrapper<FormDefinition>()
                        .orderByDesc(FormDefinition::getCreateTime)
        );
    }

    // ==================== 流程表单关联相关 ====================

    @Override
    @Transactional
    public void bindFormToProcess(ProcessFormRelation relation) {
        // 检查表单是否存在
        FormDefinition form = getFormByKey(relation.getFormKey());
        if (form == null) {
            throw new RuntimeException("表单不存在: " + relation.getFormKey());
        }
        
        // 检查是否已存在绑定关系
        ProcessFormRelation existing = processFormRelationMapper.selectOne(
                new LambdaQueryWrapper<ProcessFormRelation>()
                        .eq(ProcessFormRelation::getProcessDefinitionKey, relation.getProcessDefinitionKey())
                        .eq(ProcessFormRelation::getNodeId, relation.getNodeId())
        );
        
        if (existing != null) {
            // 更新绑定关系
            relation.setId(existing.getId());
            processFormRelationMapper.updateById(relation);
        } else {
            // 新建绑定关系
            processFormRelationMapper.insert(relation);
        }
    }

    @Override
    @Transactional
    public void unbindForm(Long id) {
        processFormRelationMapper.deleteById(id);
    }

    @Override
    public FormDefinition getStartForm(String processDefinitionKey) {
        ProcessFormRelation relation = processFormRelationMapper.selectOne(
                new LambdaQueryWrapper<ProcessFormRelation>()
                        .eq(ProcessFormRelation::getProcessDefinitionKey, processDefinitionKey)
                        .eq(ProcessFormRelation::getFormType, "start")
        );
        
        if (relation == null) {
            // 尝试获取全局表单（nodeId为空）
            relation = processFormRelationMapper.selectOne(
                    new LambdaQueryWrapper<ProcessFormRelation>()
                            .eq(ProcessFormRelation::getProcessDefinitionKey, processDefinitionKey)
                            .isNull(ProcessFormRelation::getNodeId)
                            .or()
                            .eq(ProcessFormRelation::getNodeId, "")
            );
        }
        
        if (relation != null) {
            return getFormByKey(relation.getFormKey());
        }
        return null;
    }

    @Override
    public FormDefinition getTaskForm(String processDefinitionKey, String nodeId) {
        ProcessFormRelation relation = processFormRelationMapper.selectOne(
                new LambdaQueryWrapper<ProcessFormRelation>()
                        .eq(ProcessFormRelation::getProcessDefinitionKey, processDefinitionKey)
                        .eq(ProcessFormRelation::getNodeId, nodeId)
        );
        
        if (relation != null) {
            return getFormByKey(relation.getFormKey());
        }
        return null;
    }

    @Override
    public List<ProcessFormRelation> getProcessFormRelations(String processDefinitionKey) {
        return processFormRelationMapper.selectList(
                new LambdaQueryWrapper<ProcessFormRelation>()
                        .eq(ProcessFormRelation::getProcessDefinitionKey, processDefinitionKey)
        );
    }

    // ==================== 表单实例相关 ====================

    @Override
    @Transactional
    public void saveFormInstance(FormInstance formInstance) {
        formInstanceMapper.insert(formInstance);
    }

    @Override
    public FormInstance getFormInstanceByProcessInstanceId(String processInstanceId) {
        return formInstanceMapper.selectOne(
                new LambdaQueryWrapper<FormInstance>()
                        .eq(FormInstance::getProcessInstanceId, processInstanceId)
                        .isNull(FormInstance::getTaskId)
                        .orderByDesc(FormInstance::getSubmitTime)
                        .last("LIMIT 1")
        );
    }

    @Override
    public FormInstance getFormInstanceByTaskId(String taskId) {
        return formInstanceMapper.selectOne(
                new LambdaQueryWrapper<FormInstance>()
                        .eq(FormInstance::getTaskId, taskId)
        );
    }
}