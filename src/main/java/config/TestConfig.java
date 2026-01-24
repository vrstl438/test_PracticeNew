package config;

public class TestConfig {
    public static String baseUrl(){
        return System.getProperty(
                "base.url",
                "http://localhost:4111"
        );
    }
}