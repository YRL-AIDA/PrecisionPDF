package debug;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.awt.Color;

public class DrawStyle {
    private Color strokingColor;
    private Color nonStrokingColor;
    private float lineWidth;
    private PDFont font;
    private float fontSize;

    private DrawStyle() {}

    public Color getStrokingColor() {
        return strokingColor;
    }

    public Color getNonStrokingColor() {
        return nonStrokingColor;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public PDFont getFont() {
        return font;
    }

    public float getFontSize() {
        return fontSize;
    }

    public static class Builder {
        private Color strokingColor;
        private Color nonStrokingColor;
        private float lineWidth;
        private PDFont font;
        private float fontSize;

        // Default settings
        {
            setStrokingColor(Color.BLACK);
            setNonStrokingColor(Color.BLACK);
            setLineWidth(0.5f);
            setFont(PDType1Font.HELVETICA);
            setFontSize(6f);
        }

        public Builder setStrokingColor(Color strokingColor) {
            this.strokingColor = strokingColor;
            return this;
        }

        public Builder setNonStrokingColor(Color nonStrokingColor) {
            this.nonStrokingColor = nonStrokingColor;
            return this;
        }

        public Builder setLineWidth(float lineWidth) {
            this.lineWidth = lineWidth;
            return this;
        }

        public Builder setFont(PDFont font) {
            this.font = font;
            return this;
        }

        public Builder setFontSize(float fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public DrawStyle createDrawStyle() {
            DrawStyle drawStyle = new DrawStyle();
            drawStyle.strokingColor = this.strokingColor;
            drawStyle.nonStrokingColor = this.nonStrokingColor;
            drawStyle.lineWidth = this.lineWidth;
            drawStyle.font = this.font;
            drawStyle.fontSize = this.fontSize;

            return drawStyle;
        }
    }
}
