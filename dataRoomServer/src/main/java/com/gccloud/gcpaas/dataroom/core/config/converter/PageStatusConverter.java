package com.gccloud.gcpaas.dataroom.core.config.converter;

import com.gccloud.gcpaas.dataroom.core.constant.PageStatus;
import org.springframework.core.convert.converter.Converter;

public class PageStatusConverter implements Converter<String, PageStatus> {

    @Override
    public PageStatus convert(String source) {
        return PageStatus.fromValue(source);
    }
}
