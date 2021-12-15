package io.shulie.takin.cloud.ext.content.enginecall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.shulie.takin.ext.content.enginecall.BusinessActivityExt;
import io.shulie.takin.ext.content.enginecall.ThreadGroupConfigExt;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 莫问
 * @date 2020-05-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleStartRequestExt extends ScheduleEventRequestExt {
    /**
     * 脚本引擎
     */
    private String engineType;

    //    /**
    //     * 施压模式
    //     */
    //    private String pressureMode;

    /**
     * 脚本文件路径
     */
    private String scriptPath;

    /**
     * IP数量
     */
    private Integer totalIp;

    /**
     * 从上次位点继续读取文件,默认false
     */
    private Boolean fileContinueRead = false;

    /**
     * 数据文件
     */
    private List<DataFile> dataFile;

    /**
     * 压测时长
     */
    private Long continuedTime;

    //    /**
    //     * 施压类型,0:并发,1:tps,2:自定义;不填默认为0
    //     */
    //    private Integer pressureType;
    /**
     * 施压场景：常规（压测场景页面），试跑，巡检
     */
    private Integer pressureScene;

    /**
     * 最大并发
     */
    private Integer expectThroughput;

    //    /**
    //     * 递增时长
    //     */
    //    private Long rampUp;
    //
    //    /**
    //     * 阶梯层数
    //     */
    //    private Integer steps;

    /**
     * cloud数据上报接口
     */
    private String console;
    /**
     * cloud数据回调接口
     */
    private String callbackUrl;

    /**
     * 总的目标tps
     */
    private Integer totalTps;
    /**
     * 单引擎的目标tps
     */
    private Double tps;

    /**
     * 业务指标，目标rt
     */
    private Map<String, BusinessActivityExt> businessData;
    /**
     * 是否通过xpath的md5进行关联，新老板区分
     */
    private Boolean bindByXpathMd5;

    //    /**
    //     * 业务指标，目标tps
    //     */
    //    private Map<String, Integer> businessTpsData;
    //    /**
    //     * 业务目标tps占总的tps百分比
    //     */
    //    private List<Map<String, String>> businessActivities;
    //    /**
    //     * 业务活动名称和testName绑定关系
    //     */
    //    private Map<String, String> businessNameData;

    /**
     * 压测引擎插件文件位置  一个压测场景可能有多个插件 一个插件也有可能有多个文件
     */
    private List<String> enginePluginsFilePath;

    /**
     * 循环次数
     */
    private Integer loopsNum;

    /**
     * 固定定时器配置的周期
     */
    private Long fixedTimer;

    /**
     * 是否为巡检任务
     */
    private boolean isInspect;

    /**
     * 是否为流量试跑
     */
    private boolean isTryRun;

    /**
     * 施压配置
     */
    private Map<String, ThreadGroupConfigExt> threadGroupConfigMap;

    /**
     * 添加引擎插件路径
     *
     * @param enginePluginsFilePath 引擎插件路径
     * @return -
     * @author lipeng
     */
    public List<String> addEnginePluginsFilePath(String enginePluginsFilePath) {
        if (null == this.enginePluginsFilePath) {
            this.enginePluginsFilePath = new ArrayList<>();
        }
        this.enginePluginsFilePath.add(enginePluginsFilePath);
        return this.enginePluginsFilePath;
    }

    @Data
    public static class DataFile {

        /**
         * 文件名称
         */
        private String name;

        /**
         * 文件路径
         */
        private String path;

        /**
         * 文件类型
         */
        private Integer fileType;

        /**
         * 是否分割文件
         */
        private boolean split;

        /**
         * 是否有序
         */
        private boolean ordered;

        /**
         * refId
         */
        private Long refId;

        /**
         * 是否大文件
         */
        private boolean isBigFile;

        /**
         * MD5值
         */
        private String fileMd5;

        /**
         * 文件分片信息,key-排序，引擎会用到；value-需要读取的分区数据
         */
        Map<Integer, List<StartEndPosition>> startEndPositions;

    }

    @Data
    public static class StartEndPosition {

        /**
         * 分区
         */
        private String partition;

        /**
         * pod读取文件开始位置
         */
        private String start = "-1";

        /**
         * pod读取文件结束位置
         */
        private String end = "-1";
    }
}