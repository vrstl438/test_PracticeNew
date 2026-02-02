package utils;

public class TestUtils {

    public static void repeat(int count, Runnable action) {
        for (int i = 0; i < count; i++) {
            action.run();
        }
    }
}