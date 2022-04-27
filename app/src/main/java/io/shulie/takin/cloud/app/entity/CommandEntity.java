package io.shulie.takin.cloud.app.entity;

import java.util.Date;

import lombok.Data;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 数据库实体隐射 - 命令下发
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Data
@TableName("t_command")
public class CommandEntity {
    /**
     * 数据主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 命令类型
     */
    private Integer type;
    /**
     * 命令内容
     */
    private String context;
    /**
     * ACK时间
     */
    private Date ackTime;
    /**
     * ACK内容
     */
    private String ackContext;
}
