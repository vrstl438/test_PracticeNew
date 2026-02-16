package ui.alert;

import lombok.Getter;

public enum EditProfileAlerts {
    NAME_UPDATED_SUCCESSFULLY("Name updated successfully!"),
    NAME_MUST_CONTAIN_TWO_WORDS("Name must contain two words with letters only");

    @Getter
    private final String message;

    EditProfileAlerts(String message){
        this.message = message;
    }
}
