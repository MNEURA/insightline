package pro.mneura.status;

import java.util.Locale;

public enum TranscriptCategory {
    TECHNICAL,
    HELP,
    ACCOUNT_BILLING,
    ORDER_SERVICE,
    FEEDBACK_REQUEST,
    UNKNOWN;

    public static TranscriptCategory from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public static boolean isKnown(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            valueOf(value.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
