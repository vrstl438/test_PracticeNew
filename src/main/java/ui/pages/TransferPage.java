package ui.pages;

import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

@Getter
public class TransferPage extends BasePage<TransferPage> {
    private SelenideElement accountSelector = $(".account-selector");
    private SelenideElement recipientInput = $(byAttribute("placeholder", "Enter recipient account number"));
    private SelenideElement amountInput = $(byAttribute("placeholder", "Enter amount"));
    private SelenideElement confirmCheck = $("#confirmCheck");
    private SelenideElement sendTransferButton = $(byText("\uD83D\uDE80 Send Transfer"));

    @Override
    public String url() {
        return "/transfer";
    }

    public TransferPage createTransfer(String senderAccountNumber, String recipientAccountNumber, String amount) {
        accountSelector.selectOptionContainingText(senderAccountNumber);
        recipientInput.setValue(recipientAccountNumber);
        amountInput.setValue(amount);
        confirmCheck.click();
        sendTransferButton.click();
        return this;
    }

    public TransferPage createTransferWithoutConfirmation(String senderAccountNumber, String recipientAccountNumber, String amount) {
        accountSelector.selectOptionContainingText(senderAccountNumber);
        recipientInput.setValue(recipientAccountNumber);
        amountInput.setValue(amount);
        sendTransferButton.click();
        return this;
    }

    public TransferPage submitOnlyWithConfirmation() {
        confirmCheck.click();
        sendTransferButton.click();
        return this;
    }
}
