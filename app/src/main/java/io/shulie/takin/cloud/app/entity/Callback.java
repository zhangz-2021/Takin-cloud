package io.shulie.takin.cloud.app.entity;

import java.util.Date;

import lombok.Data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 数据库实体隐射 - 回调
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Data
@TableName("t_callback")
public class Callback {
    /**
     * 数据主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 回调路径
     */
    private String url;
    /**
     * 回调内容
     */
    private byte[] context;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 是否完成
     */
    private Boolean complete;
}