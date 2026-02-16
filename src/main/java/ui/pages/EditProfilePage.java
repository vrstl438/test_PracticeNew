package ui.pages;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

@Getter
public class EditProfilePage extends BasePage<EditProfilePage> {
    private SelenideElement nameInput = $("input.form-control.mt-3");
    private SelenideElement saveChangesButton = $(byText("\uD83D\uDCBE Save Changes"));
    private SelenideElement homeButton = $(byText("\uD83C\uDFE0 Home"));

    @Override
    public String url() {
        return "/edit-profile";
    }

    public EditProfilePage editName(String name) {
        nameInput.shouldBe(visible);
        Selenide.sleep(500);
        nameInput.setValue(name);
        saveChangesButton.click();
        return this;
    }

    public EditProfilePage goHome() {
        homeButton.click();
        return this;
    }
}
