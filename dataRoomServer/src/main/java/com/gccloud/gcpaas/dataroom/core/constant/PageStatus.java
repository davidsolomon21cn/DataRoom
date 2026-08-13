package com.gccloud.gcpaas.dataroom.core.constant;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

/**
 * 页面类型
 */
public enum PageStatus implements IEnum<String> {
    DESIGN("design", "设计态"),
    PUBLISHED("published", "已发布"),
    HISTORY("history", "历史记录"),
    PREVIEW("preview", "预览"),
    SNAPSHOT("snapshot", "快照");

    private String type;
    private String desc;

    PageStatus(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    @JsonCreator
    public static PageStatus fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        for (PageStatus pageStatus : values()) {
            if (pageStatus.type.equalsIgnoreCase(normalized) || pageStatus.name().equalsIgnoreCase(normalized)) {
                return pageStatus;
            }
        }
        throw new IllegalArgumentException("不支持的页面状态: " + value);
    }

    @JsonValue
    public String getType() {
        return type;
    }

    @Override
    public String getValue() {
        return type;
    }
}
