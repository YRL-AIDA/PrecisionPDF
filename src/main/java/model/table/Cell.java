package model.table;

import model.PDFRectangle;
import model.TextChunk;

import java.util.Iterator;
import java.util.List;

public class Cell extends PDFRectangle {

    public static final float MIN_CELL_WIDTH = 10;
    public static final float MIN_CELL_HEIGHT = 5;

    // Row-column 0-based coordinates. Rows increase downwards. Columns increase to right.
    private int cl; // Left column index
    private int rt; // Top row index
    private int cr; // Right column index
    private int rb; // Bottom row index
    private int order;

    private final List<TextChunk> contentBlocks;

    public Cell(PDFRectangle bbox, List<TextChunk> contentBlocks, int cl, int rt, int cr, int rb) {
        super(bbox.getLeft(), bbox.getTop(), bbox.getRight(), bbox.getBottom());
        this.contentBlocks = contentBlocks;
        this.order = Integer.MIN_VALUE;
        if (contentBlocks != null) {
            for (TextChunk chunk : contentBlocks) {
                this.order = Math.max(this.order, chunk.getEndOrder());
            }
        }
        assert (cl >= 0);
        this.cl = cl;
        assert (rt >= 0);
        this.rt = rt;
        assert (cr >= cl);
        this.cr = cr;
        assert (rb >= rt);
        this.rb = rb;
    }

    public int getOrder() {
        return this.order;
    }

    private PDFRectangle getBBox() {
        return new PDFRectangle(getLeft(), getTop(), getRight(), getBottom());
    }

    public Cell(Cell cell) {
        this(cell.getBBox(), cell.contentBlocks, cell.cl, cell.rt, cell.cr, cell.rb);
    }

    public String getText() {
        if (null == contentBlocks || contentBlocks.isEmpty())
            return "";

        return contentBlocks
                .stream()
                .map(TextChunk::getText)
                .reduce(String::concat)
                .orElse("").trim();
    }

    public int getChunksCount() {
        return contentBlocks.size();
    }

    public Iterator<TextChunk> getBlocks() {
        return null == contentBlocks ? null : contentBlocks.iterator();
    }

    public List<TextChunk> getTextBlocks() {
        return contentBlocks;
    }

    @Override
    public boolean isEmpty() {
        if (contentBlocks == null)
            return true;
        if (contentBlocks.size() == 0)
            return true;
        return false;
    }

    public int getCl() {
        return cl;
    }

    public int getRt() {
        return rt;
    }

    public int getCr() {
        return cr;
    }

    public int getRb() {
        return rb;
    }

    public void setCl(int cl){
        this.cl =cl;
    }

    public void setRt(int rt) {
        this.rt = rt;
    }

    public void setCr(int cr) {
        this.cr = cr;
    }

    public void setRb(int rb) {
        this.rb = rb;
    }


}