package api.utils;

public class TestUtils {

    private static final int TRANSFER_MIN = 1;
    private static final int TRANSFER_MAX = 10000;

    public static String generateTransferAmount() {
        return String.valueOf(TRANSFER_MIN + (int) (Math.random() * (TRANSFER_MAX - TRANSFER_MIN + 1)));
    }

    public static void repeat(int count, Runnable action) {
        for (int i = 0; i < count; i++) {
            action.run();
        }
    }
}