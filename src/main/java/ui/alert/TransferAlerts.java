package ui.alert;

import lombok.Getter;

public enum TransferAlerts {
    PLEASE_FILL_ALL_FIELDS_AND_CONFIRM("Please fill all fields and confirm"),
    SUCCESSFULLY_TRANSFER("Successfully transferred"),
    TRANSFER_MIN_AMOUNT("Transfer amount must be at least 0.01"),
    TRANSFER_MAX_AMOUNT("Transfer amount cannot exceed 10000");

    @Getter
    private final String message;

    TransferAlerts(String message){
        this.message = message;
    }
}
