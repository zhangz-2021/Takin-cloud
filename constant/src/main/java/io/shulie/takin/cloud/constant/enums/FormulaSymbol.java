package io.shulie.takin.cloud.constant.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 算数符号
 * <p>(>=、>、=、<=、<)</p>
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Getter
@AllArgsConstructor
public enum FormulaSymbol {
    /**
     * 消除警告
     */
    GREATER_THAN_OR_EQUAL_TO(10, ">=", "大于等于"),
    GREATER_THAN(10, ">", "大于"),
    EQUAL(10, "=", "等于"),
    LESS_THAN_OR_EQUAL_TO(10, "<=", "小于等于"),
    LESS_THAN(10, ">", "大于"),
    // 格式化用
    ;
    @Getter
    private final Integer code;
    private final String symbol;
    private final String description;
}
