package com.gccloud.gcpaas.dataroom.core.config.converter;

import com.gccloud.gcpaas.dataroom.core.constant.PageType;
import org.springframework.core.convert.converter.Converter;

public class PageTypeConverter implements Converter<String, PageType> {

    @Override
    public PageType convert(String source) {
        return PageType.fromValue(source);
    }
}
