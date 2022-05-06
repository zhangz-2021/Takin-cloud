package io.shulie.takin.cloud.app.service.impl;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import io.shulie.takin.cloud.constant.enums.FileType;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.shulie.takin.cloud.app.entity.SlaEntity;
import io.shulie.takin.cloud.app.entity.JobEntity;
import io.shulie.takin.cloud.app.mapper.JobMapper;
import io.shulie.takin.cloud.app.service.JobService;
import io.shulie.takin.cloud.app.service.JsonService;
import io.shulie.takin.cloud.model.response.JobConfig;
import io.shulie.takin.cloud.app.entity.JobFileEntity;
import io.shulie.takin.cloud.app.entity.MetricsEntity;
import io.shulie.takin.cloud.app.entity.ResourceEntity;
import io.shulie.takin.cloud.app.service.CommandService;
import io.shulie.takin.cloud.model.request.StartRequest;
import io.shulie.takin.cloud.app.entity.JobExampleEntity;
import io.shulie.takin.cloud.app.service.ResourceService;
import io.shulie.takin.cloud.app.entity.ThreadConfigEntity;
import io.shulie.takin.cloud.constant.enums.ThreadGroupType;
import io.shulie.takin.cloud.app.entity.ResourceExampleEntity;
import io.shulie.takin.cloud.model.request.StartRequest.SlaInfo;
import io.shulie.takin.cloud.model.request.StartRequest.FileInfo;
import io.shulie.takin.cloud.app.service.mapper.SlaMapperService;
import io.shulie.takin.cloud.app.entity.ThreadConfigExampleEntity;
import io.shulie.takin.cloud.model.request.StartRequest.MetricsInfo;
import io.shulie.takin.cloud.app.service.mapper.MetricsMapperService;
import io.shulie.takin.cloud.app.service.mapper.JobFileMapperService;
import io.shulie.takin.cloud.app.service.mapper.JobExampleMapperService;
import io.shulie.takin.cloud.model.request.StartRequest.ThreadConfigInfo;
import io.shulie.takin.cloud.app.service.mapper.ThreadConfigMapperService;
import io.shulie.takin.cloud.app.service.mapper.ThreadConfigExampleMapperService;

/**
 * 任务服务 - 实例
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Slf4j
@Service
public class JobServiceImpl implements JobService {
    @javax.annotation.Resource
    JobMapper jobMapper;
    @javax.annotation.Resource
    JsonService jsonService;
    @javax.annotation.Resource
    CommandService commandService;
    @javax.annotation.Resource
    ResourceService resourceService;
    @javax.annotation.Resource
    SlaMapperService slaMapperService;
    @javax.annotation.Resource
    JobConfigServiceImpl jobConfigService;
    @javax.annotation.Resource
    JobFileMapperService jobFileMapperService;
    @javax.annotation.Resource
    MetricsMapperService metricsMapperService;
    @javax.annotation.Resource
    JobExampleMapperService jobExampleMapperService;
    @javax.annotation.Resource
    ThreadConfigMapperService threadConfigMapperService;
    @javax.annotation.Resource
    ThreadConfigExampleMapperService threadConfigExampleMapperService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String start(StartRequest jobInfo) {
        ResourceEntity resourceEntity = resourceService.entity(jobInfo.getResourceId());
        List<ResourceExampleEntity> resourceExampleEntityList = resourceService.listExample(resourceEntity.getId());
        JobEntity jobEntity = new JobEntity() {{
            setResourceId(resourceEntity.getId());
            setName(jobInfo.getName());
            // 时长取最大值
            setDuration(jobInfo.getThreadConfig().stream()
                .map(ThreadConfigInfo::getDuration)
                .max(Comparator.naturalOrder())
                .orElse(0));
            setSampling(jobInfo.getSampling());
            setType(jobInfo.getType().getCode());
            setCallbackUrl(jobInfo.getCallbackUrl());
            setResourceExampleNumber(resourceEntity.getNumber());
        }};
        jobMapper.insert(jobEntity);
        // 填充job实例
        List<JobExampleEntity> jobExampleEntityList = new ArrayList<>(resourceEntity.getNumber());
        for (int i = 0; i < resourceEntity.getNumber(); i++) {
            jobExampleEntityList.add(new JobExampleEntity()
                .setJobId(jobEntity.getId())
                .setDuration(jobEntity.getDuration())
                .setName(jobEntity.getName() + "-" + i)
                .setResourceExampleId(resourceExampleEntityList.get(i).getId())
            );
        }
        jobExampleMapperService.saveBatch(jobExampleEntityList);
        // 填充线程组配置
        List<ThreadConfigEntity> threadConfigEntityList = new ArrayList<>(0);
        for (ThreadConfigInfo threadConfigInfo : jobInfo.getThreadConfig()) {
            threadConfigEntityList.add(new ThreadConfigEntity() {{
                setJobId(jobEntity.getId());
                setMode(threadConfigInfo.getType().getCode());
                setRef(threadConfigInfo.getRef());
                HashMap<String, Object> context = threadConfigInfo(threadConfigInfo);
                if (jobInfo.getExt() != null) {context.putAll(jobInfo.getExt());}
                setContext(jsonService.writeValueAsString(context));
            }});
        }
        threadConfigMapperService.saveBatch(threadConfigEntityList);
        // 切分线程配置
        List<List<ThreadConfigInfo>> threadExampleList = splitThreadConfig(jobInfo.getThreadConfig(), resourceEntity.getNumber());
        // 填充线程配置实例
        List<ThreadConfigExampleEntity> threadConfigExampleEntityList = new ArrayList<>(resourceEntity.getNumber());
        for (int i = 0; i < threadExampleList.size(); i++) {
            JobExampleEntity jobExampleEntity = jobExampleEntityList.get(i);
            List<ThreadConfigInfo> threadConfigInfoList = threadExampleList.get(i);
            for (int j = 0; j < threadConfigInfoList.size(); j++) {
                ThreadConfigInfo t = threadConfigInfoList.get(j);
                HashMap<String, Object> context = threadConfigInfo(t);
                if (jobInfo.getExt() != null) {context.putAll(jobInfo.getExt());}
                threadConfigExampleEntityList.add(new ThreadConfigExampleEntity()
                    .setRef(t.getRef())
                    .setSerialNumber(j)
                    .setJobId(jobEntity.getId())
                    .setType(t.getType().getCode())
                    .setJobExampleId(jobExampleEntity.getId())
                    .setContext(jsonService.writeValueAsString(context))
                );
            }
        }
        threadConfigExampleMapperService.saveBatch(threadConfigExampleEntityList);
        // 填充SLA配置
        List<SlaEntity> slaEntityList = new ArrayList<>(jobInfo.getSlaConfig().size());
        for (int i = 0; i < jobInfo.getSlaConfig().size(); i++) {
            SlaInfo slaInfo = jobInfo.getSlaConfig().get(i);
            SlaEntity slaEntity = new SlaEntity() {{
                setRef(slaInfo.getRef());
                setJobId(jobEntity.getId());
                setAttach(slaInfo.getAttach());
                setFormulaNumber(slaInfo.getFormulaNumber());
                setFormulaTarget(slaInfo.getFormulaTarget().getCode());
                setFormulaSymbol(slaInfo.getFormulaSymbol().getCode());
            }};
            slaEntityList.add(slaEntity);
        }
        slaMapperService.saveBatch(slaEntityList);
        // 切分、填充任务文件
        List<JobFileEntity> jobFileEntityList = new ArrayList<>();
        for (int i = 0; i < jobExampleEntityList.size(); i++) {
            JobExampleEntity jobExampleEntity = jobExampleEntityList.get(i);
            FileInfo scriptFile = jobInfo.getScriptFile();
            {
                jobFileEntityList.add(new JobFileEntity()
                    .setJobExampleId(jobExampleEntity.getId())
                    .setType(FileType.SCRIPT.getCode())
                    .setUri(scriptFile.getUri())
                    .setStartPoint(-1L)
                    .setEndPoint(-1L)
                );
            }
            List<FileInfo> dataFile = jobInfo.getDataFile();
            final List<FileInfo> finalDataFile = dataFile == null ? new ArrayList<>() : dataFile;
            for (FileInfo info : finalDataFile) {
                jobFileEntityList.add(new JobFileEntity()
                    .setStartPoint(info.getSplitList().get(i).getStart())
                    .setEndPoint(info.getSplitList().get(i).getEnd())
                    .setJobExampleId(jobExampleEntity.getId())
                    .setType(FileType.DATA.getCode())
                    .setUri(info.getUri())
                );
            }
            List<FileInfo> dependencyFile = jobInfo.getDependencyFile();
            final List<FileInfo> finalDependencyFile = dependencyFile == null ? new ArrayList<>(0) : dependencyFile;
            for (FileInfo fileInfo : finalDependencyFile) {
                jobFileEntityList.add(new JobFileEntity()
                    .setJobExampleId(jobExampleEntity.getId())
                    .setType(FileType.ATTACHMENT.getCode())
                    .setUri(fileInfo.getUri())
                    .setStartPoint(-1L)
                    .setEndPoint(-1L)
                );
            }
        }
        jobFileMapperService.saveBatch(jobFileEntityList);
        // 指标目标
        List<MetricsEntity> metricsEntityList = new ArrayList<>();
        for (int i = 0; i < jobInfo.getMetricsConfig().size(); i++) {
            MetricsInfo metricsInfo = jobInfo.getMetricsConfig().get(i);
            String context = null;
            try {
                context = jsonService.writeValueAsString(new HashMap<String, Object>(4) {{
                    put("sa", metricsInfo.getSa());
                    put("rt", metricsInfo.getRt());
                    put("tps", metricsInfo.getTps());
                    put("successRate", metricsInfo.getSuccessRate());
                }});
            } catch (Exception e) {
                log.warn("JSON序列化失败");
            }
            String finalContext = context;
            metricsEntityList.add(new MetricsEntity() {{
                setJobId(jobEntity.getId());
                setRef(metricsInfo.getRef());
                setContext(finalContext);
            }});
        }
        metricsMapperService.saveBatch(metricsEntityList);
        // 下发启动命令
        commandService.startApplication(jobEntity.getId());
        return jobEntity.getId() + "";
    }

    /**
     * 转换线程配置信息
     *
     * @param threadConfigInfo 线程配置信息
     * @return 转换后的Map
     */
    private HashMap<String, Object> threadConfigInfo(ThreadConfigInfo threadConfigInfo) {
        return new HashMap<String, Object>(5) {{
            put("number", threadConfigInfo.getNumber());
            put("tps", threadConfigInfo.getTps());
            put("duration", threadConfigInfo.getDuration());
            put("growthTime", threadConfigInfo.getGrowthTime());
            put("step", threadConfigInfo.getGrowthStep());
        }};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(long jobId) {
        // 获取任务
        JobEntity jobEntity = jobMapper.selectById(jobId);
        if (jobEntity == null) {throw new RuntimeException("未找到任务:" + jobId);}
        // 释放资源
        commandService.releaseResource(jobEntity.getResourceId());
        // 停止任务
        commandService.stopApplication(jobEntity.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<JobConfig> getConfig(long jobId, String ref) {
        List<ThreadConfigExampleEntity> threadConfigExampleEntity = jobConfigService.threadExampleItem(jobId, ref);
        return threadConfigExampleEntity.stream().map(t -> {
            ThreadConfigInfo context = null;
            try {
                context = jsonService.readValue(t.getContext(), new TypeReference<ThreadConfigInfo>() {});
            } catch (JsonProcessingException e) {
                log.warn("线程组配置实例context解析失败");
            }
            ThreadConfigInfo finalContext = context;
            return new JobConfig() {{
                setRef(t.getRef());
                setJobId(t.getJobId());
                setContext(finalContext);
                setType(ThreadGroupType.of(t.getType()));
            }};
        }).collect(Collectors.toList());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyConfig(long jobId, JobConfig context) {
        // 1. 找到要修改的配置项
        List<ThreadConfigExampleEntity> threadConfigExampleEntity = jobConfigService.threadExampleItem(jobId, context.getRef());
        // 2. 如果没有抛出异常
        if (CollUtil.isEmpty(threadConfigExampleEntity)) {
            throw new RuntimeException("未找到可修改的配置");
        }
        // 存在即修改
        else {
            // TOOD 重新切分
            List<List<ThreadConfigInfo>> splitThreadConfig =
                splitThreadConfig(CollUtil.toList(context.getContext()), threadConfigExampleEntity.size());
            for (int i = 0; i < splitThreadConfig.get(0).size(); i++) {
                String contextString = jsonService.writeValueAsString(splitThreadConfig.get(0).get(i));
                // 2.1 更新任务配置实例项
                jobConfigService.modifThreadConfigExample(
                    threadConfigExampleEntity.get(i).getId(),
                    context.getType(),
                    contextString);
            }
        }
        // 2.2 下发命令
        commandService.updateConfig(jobId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JobEntity jobEntity(long jobId) {
        return jobMapper.selectById(jobId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JobExampleEntity jobExampleEntity(long jobExampleId) {
        return jobExampleMapperService.getById(jobExampleId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<JobExampleEntity> jobExampleEntityList(long jobId) {
        return jobExampleMapperService.lambdaQuery()
            .eq(JobExampleEntity::getJobId, jobId)
            .list();
    }

    /**
     * 切分线程组配置
     *
     * @param threadConfigInfoList 要切分的数量
     * @return 切分后的线程组配置
     */
    private List<List<ThreadConfigInfo>> splitThreadConfig(List<ThreadConfigInfo> threadConfigInfoList, int size) {
        List<List<ThreadConfigInfo>> result = new ArrayList<>(threadConfigInfoList.size());
        for (ThreadConfigInfo t : threadConfigInfoList) {
            List<ThreadConfigInfo> itemResult = new ArrayList<>(size);
            List<Integer> tpsList = splitInteger(t.getTps() == null ? 0 : t.getTps(), size);
            List<Integer> numberList = splitInteger(t.getNumber() == null ? 0 : t.getNumber(), size);
            for (int j = 0; j < size; j++) {
                itemResult.add(new ThreadConfigInfo()
                    .setRef(t.getRef())
                    .setType(t.getType())
                    .setTps(tpsList.get(j))
                    .setDuration(t.getDuration())
                    .setNumber(numberList.get(j))
                    .setGrowthStep(t.getGrowthStep())
                    .setGrowthTime(t.getGrowthTime())
                );
            }
            result.add(itemResult);
        }
        return result;
    }

    /**
     * 切分数值
     * <p>余数平分到每一项</p>
     *
     * @param value 需要分隔的值
     * @param size  分隔的份数
     * @return 结果集合
     */
    private List<Integer> splitInteger(int value, int size) {
        List<Integer> result = new ArrayList<>(size);
        int quotient = value / size, remainder = value % size;
        if (quotient == 0 && remainder != 0) {
            throw new RuntimeException(StrUtil.format("无法把{}分隔成{}份", value, size));
        }
        // 处理商
        for (int i = 0; i < size; i++) {result.add(quotient);}
        // 处理余数
        for (int i = 0; i < remainder; i++) {result.set(i, result.get(i) + 1);}
        return result;
    }
}
