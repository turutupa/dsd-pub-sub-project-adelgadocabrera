package utils;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;

/**
 * @author Alberto Delgado on 3/24/22
 * @project dsd-pub-sub
 */
public class KaggleParserTest extends TestCase {

    @Test
    @DisplayName("should return the next match")
    public void testNext() {
        KaggleParser kp = KaggleParser.from("access.log", "images");

        Assertions.assertNotNull(kp.next());
        Assertions.assertNotNull(kp.next());
        Assertions.assertNotNull(kp.next());
    }

    @Test
    @DisplayName("should close kaggle parser")
    public void testClose() {
        KaggleParser kp = KaggleParser.from("access.log", "images");
        kp.next();

        Assertions.assertTrue(kp.close());
    }
}