package utils;

import java.time.Duration;
import java.time.Instant;

/**
 * @author Alberto Delgado on 3/7/22
 * @project dsd-pub-sub
 * <p>
 * Helper method to calculate if a duration has timed out
 */
public class Time {
    public static boolean hasTimedOut(Duration timeout, Instant start) {
        return (timeout.toMillis() - Duration.between(Instant.now(), start).toMillis()) < 0;
    }
}
