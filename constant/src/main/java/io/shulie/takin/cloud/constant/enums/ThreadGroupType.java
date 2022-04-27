package io.shulie.takin.cloud.constant.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 线程组类型
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@AllArgsConstructor
public enum ThreadGroupType {
    /**
     * 固定线程数
     */
    CONSTANT(100, "固定", 0, 1),
    /**
     * 线性增长
     */
    LINEAR_GROWTH(101, "线性递增", 0, 2),
    /**
     * 阶段增长
     */
    STAGE_GROWTH(102, "阶段递增", 0, 3),
    /**
     * TPS
     */
    TPS(200, "TPS模式", 1, 0),
    /**
     * TPS
     */
    DIY(300, "TPS模式", 2, 0),
    // 格式化用
    ;
    @Getter
    @JsonValue
    private final Integer code;
    @Getter
    private final String name;
    @Getter
    private final Integer type;
    @Getter
    private final Integer model;

    @Override
    public String toString() {return code + ":" + name + "(" + type + "," + model + ")";}
}