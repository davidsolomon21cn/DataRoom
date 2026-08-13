package com.gccloud.gcpaas.core.config;

import com.gccloud.gcpaas.dataroom.core.config.converter.PageTypeConverter;
import com.gccloud.gcpaas.dataroom.core.constant.PageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageTypeConverterTest {

    private final PageTypeConverter converter = new PageTypeConverter();

    @Test
    void convertSupportsBusinessValueAndEnumName() {
        assertEquals(PageType.VISUAL_SCREEN, converter.convert("visualScreen"));
        assertEquals(PageType.VISUAL_SCREEN, converter.convert(" VISUAL_SCREEN "));
    }

    @Test
    void convertReturnsNullForBlankInput() {
        assertNull(converter.convert(null));
        assertNull(converter.convert(" "));
    }

    @Test
    void convertRejectsUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("unknown"));
    }

    @Test
    void enumFactorySupportsBusinessValueAndEnumName() {
        assertEquals(PageType.VISUAL_SCREEN, PageType.fromValue("visualScreen"));
        assertEquals(PageType.VISUAL_SCREEN, PageType.fromValue(" VISUAL_SCREEN "));
    }
}
