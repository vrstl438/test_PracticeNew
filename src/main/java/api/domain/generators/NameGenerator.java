package api.domain.generators;


public class NameGenerator extends BaseFaker {

    public static String validName() {
        return FAKER.name().firstName() + " " + FAKER.name().lastName();
    }

    public static String minLengthName() {
        return FAKER.regexify("[A-Za-z]{1} [A-Za-z]{1}");
    }

    public static String singleWordName() {
        return FAKER.regexify("[A-Za-z]{3,10}");
    }

    public static String threeWordsName() {
        return FAKER.regexify("[A-Za-z]{2,5} [A-Za-z]{2,5} [A-Za-z]{2,5}");
    }

    public static String doubleSpaceName() {
        return FAKER.regexify("[A-Za-z]{2,7}  [A-Za-z]{2,7}");
    }
}