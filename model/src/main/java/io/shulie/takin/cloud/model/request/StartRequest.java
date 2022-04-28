package io.shulie.takin.cloud.model.request;

import java.util.List;

import lombok.Data;

import io.swagger.v3.oas.annotations.media.Schema;

import io.shulie.takin.cloud.constant.enums.JobType;
import io.shulie.takin.cloud.constant.enums.ThreadGroupType;

/**
 * 请求体 - 启动
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Data
@Schema(description = "启动任务入参")
public class StartRequest {
    @Schema(description = "状态回调路径")
    private String callbackUrl;
    @Schema(description = "资源主键")
    private long resourceId;
    @Schema(description = "JVM选项")
    private String jvmOptions;
    @Schema(description = "采样率")
    private Integer sampling;
    @Schema(description = "任务类型")
    private JobType type;
    @Schema(description = "任务名称")
    private String name;
    @Schema(description = "脚本文件(jmx)")
    private FileInfo scriptFile;
    @Schema(description = "运行时依赖文件(插件)")
    private List<FileInfo> dependency;
    @Schema(description = "数据文件(csv)")
    private List<FileInfo> data;
    @Schema(description = "线程配置")
    private List<ThreadConfigInfo> threadConfig;

    @Data
    @Schema(description = "文件信息")
    public static class FileInfo {
        /**
         * 切片信息
         */
        @Schema(description = "切片信息")
        private List<SplitInfo> splitList;
        /**
         * 文件路径
         */
        private String uri;

        @Data
        @Schema(description = "文件切片信息")
        public static class SplitInfo {
            /**
             * 起始位置
             */
            @Schema(description = "起始位置")
            private long start;
            /**
             * 结束位置
             */
            @Schema(description = "结束位置")
            private long end;
        }
    }

    @Data
    @Schema(description = "线程组配置")
    public static class ThreadConfigInfo {
        @Schema(description = "关键词")
        private String ref;
        @Schema(description = "线程组类型")
        private ThreadGroupType type;
        // 附加参数
        @Schema(description = "持续时长")
        private Integer duration;
        @Schema(description = "线程数")
        private Integer number;
        @Schema(description = "TPS值")
        private Integer tps;
        @Schema(description = "增长时长")
        private Integer growthTime;
        @Schema(description = "增长步骤")
        private Integer growthStep;
    }
}