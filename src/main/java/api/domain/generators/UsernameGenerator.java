package api.domain.generators;

public class UsernameGenerator extends BaseFaker{

    public static String validUsername(){
        return FAKER.regexify("[A-Za-z]{4,15}");
    }

    public static String shortUsername(){
        return FAKER.regexify("[A-Za-z]{1,2}");
    }

    public static String twoWordsInUsername(){
        return FAKER.regexify("[A-Za-z]{2,9}" + " " + "[A-Za-z]{2,9}");
    }

    public static String specialSymbolInUsername(){
        return FAKER.name().firstName() + FAKER.regexify("[!@#$%^&*()]{1,4}");
    }
}
