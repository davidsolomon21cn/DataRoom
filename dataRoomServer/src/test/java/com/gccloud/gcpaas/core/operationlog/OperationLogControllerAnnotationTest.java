package com.gccloud.gcpaas.core.operationlog;

import com.gccloud.gcpaas.dataroom.core.user.CaptchaController;
import com.gccloud.gcpaas.dataroom.core.dataset.DatasetController;
import com.gccloud.gcpaas.dataroom.core.datasource.DataSourceController;
import com.gccloud.gcpaas.dataroom.core.datasource.ExcelDataSourceController;
import com.gccloud.gcpaas.dataroom.core.map.MapController;
import com.gccloud.gcpaas.dataroom.core.page.PageController;
import com.gccloud.gcpaas.dataroom.core.resources.ResourceController;
import com.gccloud.gcpaas.dataroom.core.user.UserController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class OperationLogControllerAnnotationTest {

    @Test
    void keyControllersExposeBusinessModuleViaTag() {
        List<Class<?>> controllers = List.of(
                PageController.class,
                DatasetController.class,
                DataSourceController.class,
                ExcelDataSourceController.class,
                ResourceController.class,
                MapController.class,
                UserController.class,
                CaptchaController.class
        );
        for (Class<?> controller : controllers) {
            assertNotNull(controller.getAnnotation(Tag.class),
                    controller.getSimpleName() + " 应标注 @Tag 作为业务模块来源");
        }
    }

    @Test
    void selectedMethodsAreLoggedViaOperationAnnotation() {
        assertNotNull(findMethod(PageController.class, "publish").getAnnotation(Operation.class));
        assertNotNull(findMethod(PageController.class, "offline").getAnnotation(Operation.class));
        assertNotNull(findMethod(PageController.class, "updatePageConfig").getAnnotation(Operation.class));
        assertNotNull(findMethod(PageController.class, "updatePageConfig4Preview").getAnnotation(Operation.class));
        assertNotNull(findMethod(PageController.class, "getPageConfig").getAnnotation(Operation.class));
        assertNotNull(findMethod(PageController.class, "stageClear").getAnnotation(Operation.class));
        assertNotNull(findMethod(PageController.class, "stageRollback").getAnnotation(Operation.class));
        assertNotNull(findMethod(DatasetController.class, "run").getAnnotation(Operation.class));
        assertNotNull(findMethod(DatasetController.class, "runTest").getAnnotation(Operation.class));
        assertNotNull(findMethod(ExcelDataSourceController.class, "upload").getAnnotation(Operation.class));
        assertNotNull(findMethod(ExcelDataSourceController.class, "createAndImport").getAnnotation(Operation.class));
        assertNotNull(findMethod(ExcelDataSourceController.class, "reimport").getAnnotation(Operation.class));
        assertNotNull(findMethod(ExcelDataSourceController.class, "viewData").getAnnotation(Operation.class));
        assertNotNull(findMethod(ResourceController.class, "upload").getAnnotation(Operation.class));
        assertNotNull(findMethod(ResourceController.class, "updateModelConfig").getAnnotation(Operation.class));
        assertNotNull(findMethod(ResourceController.class, "uploadModelCover").getAnnotation(Operation.class));
        assertNotNull(findMethod(UserController.class, "login").getAnnotation(Operation.class));
        assertNotNull(findMethod(UserController.class, "updateProfile").getAnnotation(Operation.class));
        assertNotNull(findMethod(CaptchaController.class, "generate").getAnnotation(Operation.class));
    }

    private Method findMethod(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
