package ui.actions;

import com.codeborne.selenide.Selenide;
import common.context.ScenarioContext;

import static com.codeborne.selenide.Selenide.executeJavaScript;
import static common.datakeys.Keys.USER_TOKEN;

public class UserActions {
    private static ScenarioContext context = new ScenarioContext();

    public static void openPageAsCreatedUser(Pages page, String token){
        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", token);

        switch (page){
            case DASHBOARD -> Selenide.open("/dashboard");
            case DEPOSIT -> Selenide.open("/deposit");
            case TRANSFER -> Selenide.open("/transfer");
            case EDIT_PROFILE -> Selenide.open("/edit-profile");
        }
    }

    public static void openPageAsCreatedUser(Pages page){
        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", context.getData(USER_TOKEN, String.class));

        switch (page){
            case DASHBOARD -> Selenide.open("/dashboard");
            case DEPOSIT -> Selenide.open("/deposit");
            case TRANSFER -> Selenide.open("/transfer");
            case EDIT_PROFILE -> Selenide.open("/edit-profile");
        }
    }
}
