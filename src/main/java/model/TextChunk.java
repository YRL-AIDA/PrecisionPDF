package model;

import org.apache.pdfbox.text.TextPosition;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TextChunk extends PDFRectangle {
    private int id;
    private Page page;
    private String text;
    private PDFFont PDFFont;
    private Color color;
    private float spaceWidth;
    private int startOrder; // The index of an original chunk in its PDF document
    private int endOrder;
    private int coherence;
    private boolean modified = false;
    private List<TextPosition> textPositions = new ArrayList<>();

    public TextChunk(double left, double top, double right, double bottom, String text, Page page) {
        super(left, top, right, bottom);
        setText(text);
        setPage(page);
        initTextLine();
    }

    public TextChunk(PDFRectangle boundingBox, String text, Page page) {
        this(boundingBox.getLeft(), boundingBox.getTop(), boundingBox.getRight(), boundingBox.getBottom(), text, page);
    }

    public TextChunk(Point2D.Float leftTopPoint, Point2D.Float rightBottomPoint, String text, Page page) {
        this(leftTopPoint.x, leftTopPoint.y, rightBottomPoint.x, rightBottomPoint.y, text, page);
    }

    public void retract() {
        assert (null != page);
        page.removeBlock(this);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Page getPage() {
        return page;
    }

    private void setPage(Page page) {
        if (null == page) throw new IllegalArgumentException("The page cannot be null");
        this.page = page;
    }

    public PDFFont getFont() {
        return PDFFont;
    }

    public void setFont(PDFFont PDFFont) {
        assert (null != PDFFont);
        this.PDFFont = PDFFont;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public float getSpaceWidth() {
        return spaceWidth;
    }

    public void setSpaceWidth(float spaceWidth) {
        this.spaceWidth = spaceWidth;
    }

    public int getStartOrder() {
        return startOrder;
    }

    public void setStartOrder(int startOrder) {
        this.startOrder = startOrder;
    }

    public int getEndOrder() {
        return endOrder;
    }

    public void setEndOrder(int endOrder) {
        this.endOrder = endOrder;
    }

    public int getCoherence() {
        return coherence;
    }

    public void setCoherence(int coherence) {
        this.coherence = coherence;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        int pageIndex = getPage().getIndex();
        return String.format("P%dB%d", pageIndex, id);
    }

    private List<TextLine> textLines = new ArrayList<>();

    public void updateTextLine() {
        TextLine textLine = textLines.get(0);
        textLine.setText(text);
        PDFRectangle bbox = new PDFRectangle(getLeft(), getTop(), getRight(), getBottom());
        textLine.setBbox(bbox);
    }

    private void initTextLine() {
        String text = getText();
        PDFRectangle bbox = new PDFRectangle(getLeft(), getTop(), getRight(), getBottom());
        TextLine textLine = new TextLine(text, bbox);
        textLines.add(textLine);
    }

    public void newTextLine(TextChunk block) {
        textLines.addAll(block.textLines);
    }

    public Iterator<TextLine> getTextLines() {
        return textLines.iterator();
    }

    public void addAllTextPositions(List<TextPosition> tp) {
        textPositions.addAll(tp);
    }

    public Iterator<TextPosition> getTextPositions() {
        return textPositions.iterator();
    }

    public class TextLine {
        private String text;
        private PDFRectangle bbox;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public PDFRectangle getBbox() {
            return this.bbox;
        }

        public void setBbox(PDFRectangle bbox) {
            this.bbox = bbox;
        }

        private TextLine(String text, PDFRectangle bbox) {
            setText(text);
            setBbox(bbox);
        }
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
