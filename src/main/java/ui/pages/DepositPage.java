package ui.pages;

import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Condition.*;

@Getter
public class DepositPage extends BasePage<DepositPage>{
    private SelenideElement depositPageLabel = $(byText("\uD83D\uDCB0 Deposit Money"));
    private SelenideElement chooseAsAccountDropdown = $(".account-selector");
    private SelenideElement amountInput =  $(byAttribute("placeholder", "Enter amount"));
    private SelenideElement depositButton = $(byText("\uD83D\uDCB5 Deposit"));

    @Override
    public String url (){
        return "/deposit";
    }

    public DepositPage createDeposit(String accountNumber, String amount){
        chooseAsAccountDropdown.selectOptionContainingText(accountNumber);
        amountInput.setValue(amount);
        depositButton.click();
        return this;
    }

    public DepositPage chekCreatedDepositInDropdown(String accountNumber, String amount) {
        chooseAsAccountDropdown
                .$$("option")
                .findBy(text(accountNumber))
                .shouldHave(text(amount));
        return this;
    }
}
