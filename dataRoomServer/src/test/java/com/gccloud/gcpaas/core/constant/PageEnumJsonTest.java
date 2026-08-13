package com.gccloud.gcpaas.core.constant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gccloud.gcpaas.dataroom.core.constant.PageStatus;
import com.gccloud.gcpaas.dataroom.core.constant.PageType;
import com.gccloud.gcpaas.dataroom.core.entity.PageEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageEnumJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializeAcceptsMcpSchemaEnumNames() throws Exception {
        String json = "{\"pageStatus\":\"DESIGN\",\"pageType\":\"VISUAL_SCREEN\"}";

        PageEntity page = objectMapper.readValue(json, PageEntity.class);

        assertEquals(PageStatus.DESIGN, page.getPageStatus());
        assertEquals(PageType.VISUAL_SCREEN, page.getPageType());
    }

    @Test
    void deserializeKeepsAcceptingBusinessValues() throws Exception {
        String json = "{\"pageStatus\":\"design\",\"pageType\":\"visualScreen\"}";

        PageEntity page = objectMapper.readValue(json, PageEntity.class);

        assertEquals(PageStatus.DESIGN, page.getPageStatus());
        assertEquals(PageType.VISUAL_SCREEN, page.getPageType());
    }
}
