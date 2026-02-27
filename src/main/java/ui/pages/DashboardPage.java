package ui.pages;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;

@Getter
public class DashboardPage extends BasePage<DashboardPage>{
    private SelenideElement depositButton = $(byText("\uD83D\uDCB0 Deposit Money"));
    private SelenideElement transferButton = $(byText("\uD83D\uDD04 Make a Transfer"));

    @Override
    public String url(){
        return "/dashboard";
    }

//    public DashboardPage dashboard(){
//    }
}
