import network.akila.surveyor.util.DurationParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DurationParserTest {

    @Test
    @DisplayName("Parses single time units correctly")
    void parsesSingleUnits() {
        assertEquals(Duration.ofDays(1), DurationParser.parse("1d"));
        assertEquals(Duration.ofHours(2), DurationParser.parse("2h"));
        assertEquals(Duration.ofMinutes(30), DurationParser.parse("30m"));
        assertEquals(Duration.ofSeconds(45), DurationParser.parse("45s"));
    }

    @Test
    @DisplayName("Parses combined time units correctly")
    void parsesCombinedUnits() {
        assertEquals(Duration.ofHours(26), DurationParser.parse("1d2h"));
        assertEquals(Duration.ofMinutes(90), DurationParser.parse("1h30m"));
    }

    @Test
    @DisplayName("Rejects invalid inputs")
    void rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("abc"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse(""));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parse("0h"));
    }

    @Test
    @DisplayName("Formats Duration into readable string")
    void formatsDuration() {
        assertEquals("1d2h30m", DurationParser.format(Duration.ofHours(26).plusMinutes(30)));
        assertEquals("45s", DurationParser.format(Duration.ofSeconds(45)));
    }
}