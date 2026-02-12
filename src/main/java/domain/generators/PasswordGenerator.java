package domain.generators;

public class PasswordGenerator extends BaseFaker {

    public static String validPassword() {
        return FAKER.regexify("[A-Z]{1}[a-z]{1}[0-9]{1}[!@#$%^&]{1}[A-Za-z0-9!@#$%^&]{4,12}");
    }

    public static String withoutUppercasePassword() {
        return FAKER.regexify("[a-z]{2}[0-9]{1}[!@#$%^&]{1}[a-z0-9!@#$%^&]{4,12}");
    }

    public static String withoutDigitPassword() {
        return FAKER.regexify("[A-Z]{1}[a-z]{2}[!@#$%^&]{1}[A-Za-z!@#$%^&]{4,12}");
    }

    public static String shortPassword() {
        return FAKER.regexify("[A-Z]{1}[a-z]{1}[0-9]{1}[!@#$%^&]{1}[A-Za-z0-9!@#$%^&]{1,3}");
    }

    public static String withSpacesPassword() {
        return FAKER.regexify("[A-Z]{1}[a-z]{1} [0-9]{1}[!@#$%^&]{1}[A-Za-z0-9!@#$%^&]{4,12}");
    }
}
