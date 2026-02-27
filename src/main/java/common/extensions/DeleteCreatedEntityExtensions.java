package common.extensions;

import api.domain.model.response.UserResponse;
import api.skelethon.requesters.CrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import common.annotations.DeleteCreatedEntity;
import common.context.ScenarioContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import static api.skelethon.Endpoint.DELETE_USER;
import static common.datakeys.ResponseKey.CREATED_USER;

public class DeleteCreatedEntityExtensions implements AfterEachCallback {
    ScenarioContext context = new ScenarioContext();

    @Override
    public void afterEach(ExtensionContext extensionContext) {

        if (AnnotationSupport.findAnnotation(extensionContext.getElement(), DeleteCreatedEntity.class).isEmpty()
                && AnnotationSupport.findAnnotation(extensionContext.getTestClass(), DeleteCreatedEntity.class).isEmpty()) {
            return;
        }

        new CrudRequester(
                RequestSpecs.adminAuthSpec(),
                DELETE_USER,
                ResponseSpecs.okText()
        ).delete(context.getData(CREATED_USER, UserResponse.class).getId());
    }
}
