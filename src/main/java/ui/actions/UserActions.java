package ui.actions;

import com.codeborne.selenide.Selenide;

import static com.codeborne.selenide.Selenide.executeJavaScript;

public class UserActions {

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
}
