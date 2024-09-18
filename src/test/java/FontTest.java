import model.PDFFont;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;


public class FontTest {

    @Test
    public void testCopyFont() throws IOException {
        PDFFont font = new PDFFont();

        font.setFontSize(10);
        font.setName("Font1");
        font.setHeight(12);
        font.setItalic(true);
        font.setBold(true);

        PDFFont copy_font = font.copyFont();

        font.setFontSize(11);
        font.setName("Font2");
        font.setHeight(13);
        font.setItalic(false);
        font.setBold(false);

        Assertions.assertNotEquals(copy_font.getFontSize(), font.getFontSize());
        Assertions.assertNotEquals(copy_font.getName(), font.getName());
        Assertions.assertNotEquals(copy_font.getHeight(), font.getHeight());
        Assertions.assertNotEquals(copy_font.isItalic(), font.isItalic());
        Assertions.assertNotEquals(copy_font.isBold(), font.isBold());

    }
}
