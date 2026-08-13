package com.gccloud.gcpaas.dataroom.core.constant;

import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.StringUtils;

/**
 * 页面类型
 */
public enum PageType implements IEnum<String> {
    DIRECTORY("directory", "目录或文件夹"),
    VISUAL_SCREEN("visualScreen", "虚拟大屏"),
    PAGE("page", "页面");

    public static final String DIRECTORY_TYPE = "directory";
    public static final String VISUAL_SCREEN_TYPE = "visualScreen";
    public static final String PAGE_TYPE = "page";

    private String type;
    private String desc;

    PageType(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    @JsonCreator
    public static PageType fromValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String normalized = value.trim();
        for (PageType pageType : values()) {
            if (pageType.type.equalsIgnoreCase(normalized) || pageType.name().equalsIgnoreCase(normalized)) {
                return pageType;
            }
        }
        throw new IllegalArgumentException("不支持的页面类型: " + value);
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
