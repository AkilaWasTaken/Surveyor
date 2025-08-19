package network.akila.surveyor.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses durations like:
 * "1d", "2h", "30m", "45s", "1d2h30m", "1h 15m", case-insensitive.
 */
public final class DurationParser {
    private static final Pattern TOKEN = Pattern.compile("(\\d+)\\s*([dhms])", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    /**
     * Turns a duration string (like "1d2h30m") into a {@link Duration}.
     * Invalid or empty strings will throw an {@link IllegalArgumentException}.
     */
    public static Duration parse(String input) {
        if (input == null) throw new IllegalArgumentException("duration is null");
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) throw new IllegalArgumentException("duration is empty");

        Matcher m = TOKEN.matcher(s);
        long totalSeconds = 0L;
        int matches = 0;

        while (m.find()) {
            matches++;
            long value = Long.parseLong(m.group(1));
            char unit = m.group(2).charAt(0);
            switch (unit) {
                case 'd':
                    totalSeconds += value * 24 * 3600L;
                    break;
                case 'h':
                    totalSeconds += value * 3600L;
                    break;
                case 'm':
                    totalSeconds += value * 60L;
                    break;
                case 's':
                    totalSeconds += value;
                    break;
                default:
                    throw new IllegalArgumentException("unknown unit: " + unit);
            }
        }

        if (matches == 0) {
            throw new IllegalArgumentException("Invalid duration: '" + input + "'. Expected forms like 1d, 2h30m, 45m, 90s");
        }
        if (totalSeconds <= 0) {
            throw new IllegalArgumentException("Duration must be > 0 seconds");
        }
        return Duration.ofSeconds(totalSeconds);
    }

    /**
     * Formats a {@link Duration} into a compact string like 1d2h30m45s.
     *
     * @param d the duration to format
     * @return a human-friendly string representation of the duration
     */
    public static String format(Duration d) {
        if (d.isZero() || d.isNegative()) return "0s";
        long seconds = d.getSeconds();

        long days = seconds / (24 * 3600);
        seconds %= (24 * 3600);
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append('d');
        if (hours > 0) sb.append(hours).append('h');
        if (minutes > 0) sb.append(minutes).append('m');
        if (seconds > 0) sb.append(seconds).append('s');
        return sb.toString();
    }
}