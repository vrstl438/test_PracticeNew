package common.extensions;

import com.codeborne.selenide.Selenide;
import common.annotations.OpenAsUser;
import common.context.ScenarioContext;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static com.codeborne.selenide.Selenide.executeJavaScript;
import static common.datakeys.Keys.USER_TOKEN;
import static ui.actions.Pages.*;

public class OpenAsUserExtensions implements BeforeEachCallback {
    ScenarioContext context = new ScenarioContext();


    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        OpenAsUser annotation = extensionContext.getRequiredTestMethod().getAnnotation(OpenAsUser.class);

        if (annotation != null){
            Selenide.open("/");
            executeJavaScript("localStorage.setItem('authToken', arguments[0]);", context.getData(USER_TOKEN, String.class));

            switch (annotation.page()){
                case DASHBOARD -> Selenide.open("/dashboard");
                case DEPOSIT -> Selenide.open("/deposit");
                case TRANSFER -> Selenide.open("/transfer");
                case EDIT_PROFILE -> Selenide.open("/edit-profile");
            }
        }
    }
}
