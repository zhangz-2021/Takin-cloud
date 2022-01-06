package io.shulie.takin.cloud.sdk.model.response.report;

import java.io.Serializable;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author moriarty
 */
@Data
public class ScriptNodeTreeResp implements Serializable {

    @ApiModelProperty(value = "业务活动ID")
    private Long businessActivityId;

    @ApiModelProperty(value = "节点类型名称")
    private String name;

    @ApiModelProperty(value = "节点名称")
    private String testName;

    @ApiModelProperty(value = "节点MD5值")
    private String md5;

    @ApiModelProperty(value = "节点绝对路径")
    private String xpath;

    @ApiModelProperty(value = "节点路径MD5值")
    private String xpathMd5;

    @ApiModelProperty(value = "节点定义")
    private String identification;

    @ApiModelProperty(value = "子节点")
    private List<ScriptNodeTreeResp> children;
    /**
     * 施压类型
     * 参考:io.shulie.takin.cloud.data.result.report.ReportResult#pressureType
     */
    @ApiModelProperty(value = "施压类型")
    private Integer pressureType;

}