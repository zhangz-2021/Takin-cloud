package io.shulie.takin.cloud.app.service;

import java.util.List;

import io.shulie.takin.cloud.app.entity.SlaEntity;
import io.shulie.takin.cloud.constant.enums.FormulaSymbol;
import io.shulie.takin.cloud.constant.enums.FormulaTarget;

/**
 * SLA服务
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
public interface SlaService {
    /**
     * 列出任务相关的所有SLA
     *
     * @param jobId 任务主键
     * @return SLA列表
     */
    List<SlaEntity> list(long jobId);

    /**
     * 创建SLA记录
     *
     * @param jobId  任务主键
     * @param ref    关键字
     * @param target 算数目标
     * @param symbol 算数符号
     * @param number 对比值
     */
    void create(long jobId, String ref, FormulaTarget target, FormulaSymbol symbol, double number);

    /**
     * 创建SLA触发记录
     *
     * @param slaId        SLA主键
     * @param jobExampleId 任务实例主键
     * @param ref          关键字
     * @param target       算数目标
     * @param symbol       算数符号
     * @param number       对比值
     */
    void event(long slaId, long jobExampleId, String ref, FormulaTarget target, FormulaSymbol symbol, double number);
}