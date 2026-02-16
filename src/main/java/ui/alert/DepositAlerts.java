package ui.alert;

import lombok.Getter;

public enum DepositAlerts {
    SUCCESSFULLY_DEPOSITED("Successfully deposited"),
    PLEASE_ENTER_VALID_AMOUNT("Please enter a valid amount."),
    PLEASE_DEPOSIT_LESS_OR_EQUAL_5000("Please deposit less or equal to 5000$."),
    PLEASE_SELECT_ACCOUNT("Please select an account.");

    @Getter
    private final String message;

    DepositAlerts(String message){
        this.message = message;
    }
}
