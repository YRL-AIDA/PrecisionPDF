package debug;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.Color;
import java.io.Closeable;
import java.io.IOException;

class PageDrawer  implements Closeable {
    private final PDDocument document;
    private final PDPage page;

    private final float pageHeight;
    private final PDPageContentStream contentStream;

    private PageDrawer(PDDocument document, int pageIndex) throws IOException {
        this.document = document;
        page = document.getPage(pageIndex);
        contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true);
        pageHeight = page.getBBox().getHeight();
    }

    void drawRectangle(double left, double top, double right, double bottom) throws IOException {
        // TODO: Modify this code, need to set the affine transformation to the current content stream
        top = pageHeight - top;
        bottom = pageHeight - bottom;
        double width = right - left;
        double height = bottom - top;

        contentStream.addRect((float) left, (float)top, (float)width, (float)height);
        contentStream.stroke();
    }

    void fillRectangle(float left, float top, float right, float bottom) throws IOException {
        // TODO: Modify this code, need to set the affine transformation to the current content stream
        top = pageHeight - top;
        bottom = pageHeight - bottom;
        float width = right - left;
        float height = bottom - top;

        contentStream.addRect(left, top, width, height);
        contentStream.fill();
    }

    void drawLine(double x1, double y1, double x2, double y2) throws IOException {
        // TODO: Modify this code, need to set the affine transformation to the current content stream
        y1 = pageHeight - y1;
        y2 = pageHeight - y2;

        contentStream.moveTo((float) x1, (float) y1);
        contentStream.lineTo((float) x2, (float) y2);
        contentStream.stroke();
        contentStream.moveTo((float) x1,(float) y1);
    }

    void drawString(String text, double x, double y) throws IOException {
        // TODO: Modify this code, need to set the affine transformation to the current content stream
        y = pageHeight - y;
        contentStream.beginText();
        contentStream.newLineAtOffset((float) x, (float)y);
        contentStream.showText(text);
        contentStream.endText();
    }

    @Override
    public void close() throws IOException {
        contentStream.closeAndStroke();
        contentStream.close();
    }

    static class Builder {
        private PDDocument document;

        private Color strokingColor;
        private Color nonStrokingColor;
        private float lineWidth;
        private PDFont font;
        private float fontSize;

        Builder(PDDocument document, DrawStyle drawStyle) {
            this.document = document;

            strokingColor = drawStyle.getStrokingColor();
            nonStrokingColor = drawStyle.getNonStrokingColor();
            lineWidth = drawStyle.getLineWidth();
            font = drawStyle.getFont();
            fontSize = drawStyle.getFontSize();
        }

        PageDrawer createPageDrawer(int pageIndex) throws IOException {
            PageDrawer drawer = new PageDrawer(document, pageIndex);
            drawer.contentStream.setStrokingColor(strokingColor);
            drawer.contentStream.setNonStrokingColor(nonStrokingColor);
            drawer.contentStream.setLineWidth(lineWidth);
            drawer.contentStream.setFont(font, fontSize);
            return drawer;
        }
    }
}
