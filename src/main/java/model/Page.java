package model;

import jdk.nashorn.internal.ir.Block;
import model.table.Cell;
import model.table.Table;
import org.apache.pdfbox.pdmodel.PDPage;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Page extends PDFRectangle {

    public static final float MIN_MARGIN = 5f;

    private final Document document;       // Page owner
    private final int index;               // Page 0-based index
    private final Orientation orientation; // Paper orientation

    // Text content
    private final java.util.List<TextChunk> chunks; // Original text chunks extracted from a PDF document
    private final java.util.List<TextChunk> chars;  // Characters extracted from original PDF chunks
    private final java.util.List<TextChunk> words;  // Words composed from characters
    private final java.util.List<TextChunk> lines;  // Text lines composed from characters
    private final java.util.List<TextChunk> blocks; // Text blocks composed from words

    // Ruling lines
    private final java.util.List<Ruling> rulings;           // Original rulings
    private final java.util.List<Ruling> normalizedRulings; // Normalized (merged) rulings
    private final java.util.List<Ruling> verticalRulings;   // Vertical normalized rulings
    private final java.util.List<Ruling> horizontalRulings; // Horizontal normalized rulings
    private final java.util.List<Ruling> visibleRulings;
    private java.util.List<Ruling> joinedRulings;
    //private java.util.List<Ruling> borderedTableExtractionRulings;

    //private java.util.List<TableArea> tableAreas;
    private java.util.List<PDFRectangle> possibleTables;
    private java.util.List<Table> tables;
    //private java.util.List<Table> nakedTables;
    private List<PDFRectangle> cells;

    // Initialization
    {
        chunks = new ArrayList<>();
        chars  = new ArrayList<>();
        words  = new ArrayList<>();
        lines  = new ArrayList<>();
        blocks = new ArrayList<>();

        rulings           = new ArrayList<>();
        normalizedRulings = new ArrayList<>();
        verticalRulings   = new ArrayList<>();
        horizontalRulings = new ArrayList<>();
        visibleRulings    = new ArrayList<>();
        tables            = new ArrayList<>();
        cells             = new ArrayList<>();
        possibleTables    = new ArrayList<>();
        tables            = new ArrayList<>();
        //borderedTableExtractionRulings = new ArrayList<>();

    }

    public Page(Document document, int index, float left, float top, float right, float bottom) {
        super(left, top, right, bottom);

        if (null == document) {
            throw new IllegalArgumentException("Document cannot be null");
        } else {
            this.document = document;
        }

        if (index < 0) {
            throw new IllegalArgumentException("Page index cannot be less 0");
        } else {
            this.index = index;
        }

        double width = getWidth();
        double height = getHeight();

        if (width > height)
            orientation = Orientation.LANDSCAPE;
        else if (width < height)
            orientation = Orientation.PORTRAIT;
        else
            orientation = Orientation.NEITHER;

    }

    public int getIndex() {
        return this.index;
    }

    public void removeBlock(TextChunk block) {
        blocks.remove(block);
    }

    public void addBlocks(Collection<TextChunk> blocks) {
        for (TextChunk block: blocks)
            addBlock(block);
    }

    private void addBlock(TextChunk block) {
        if (blocks.add(block))
            block.setId(blocks.size());
    }

    public boolean addChunks(List<TextChunk> chunks) {
        return this.chunks.addAll(chunks);
    }

    public boolean addChars(List<TextChunk> chars) {
        return this.chars.addAll(chars);
    }

    public boolean addWords(List<TextChunk> words) {
        return this.words.addAll(words);
    }

    public boolean addRulings(List<Ruling> rulings) {
        return this.rulings.addAll(rulings);
    }

    public PDPage getPDPage() {
        return document.getPDPage(index);
    }

/*    public boolean addBorderedTableRuling(List<Ruling> visibleRulings){
        this.borderedTableExtractionRulings.clear();
        boolean result = visibleRulings == null ? false : this.visibleRulings.addAll(rulings);
        return result;
    }*/

    public boolean addVisibleRulings(List<Ruling> visibleRulings) {
        this.visibleRulings.clear();
        boolean result = visibleRulings == null ? false : this.visibleRulings.addAll(rulings);
        //categorizeRulingLines();
        //joinRulingLines();
        return result;
    }

    public boolean canPrint(Point2D.Float point) {
        double lt = getLeft()   + MIN_MARGIN;
        double tp = getTop()    + MIN_MARGIN;
        double rt = getRight()  - MIN_MARGIN;
        double bm = getBottom() - MIN_MARGIN;
        return lt < point.x && point.x < rt && tp < point.y && point.y < bm;
    }

    public void addLines(List<TextChunk> lines) {
        lines.addAll(lines);
    }

    public Iterator<TextChunk> getChunks() {
        return chunks.iterator();
    }

    public Iterator<TextChunk> getChars() {
        return chars.iterator();
    }

    public Iterator<TextChunk> getWords() {
        return words.iterator();
    }

    public Iterator<TextChunk> getBlocks() {
        return blocks.iterator();
    }

    public List<TextChunk> getAllBlocks() {
        return blocks;
    }

    public Iterator<Ruling> getVisibleRulings() {
        return visibleRulings.iterator();
    }

    public Iterator<Ruling> getRulings() {
        return rulings.iterator();
    }


    public List<Table> getTables(){
        return tables;
    }

    public void addJoinedRulings(ArrayList<Ruling> joinedHorizontalRulings) {
    }

    public void addCell(PDFRectangle cell) {
/*        boolean intersected = false;
        for (PDFRectangle c: cells) {
            if (c.intersects(cell)){
                c.add(cell);
                intersected = true;
            }
        }*/
        //if (!intersected)
            cells.add(cell);
    }

    private void mergeCells(Cell c1, Cell c2){

    }

    public Iterator<PDFRectangle> getCells(){
        return cells.iterator();
    }

    public void addPossibleTableArea (PDFRectangle tableArea) {
        possibleTables.add(tableArea);
    }

    public Iterator<PDFRectangle> getPossibleTableArea () {
        return possibleTables.iterator() ;
    }

    public void addTable(Table table) {
        if (tables.contains(table)) {
            tables.remove(table);
        }
        tables.add(table);
    }

    public Iterator<Ruling> getBorderedTableRulings() {
        return visibleRulings.iterator();
    }

    public List<TextChunk> getOutsideBlocks(){
        List<TextChunk> result = new ArrayList<>();
        if (tables.isEmpty()) return blocks;
        for (TextChunk block: blocks) {
            for (Table table: tables) {
                if (!block.intersects(table) && !result.contains(block)) {
                    result.add(block);
                }
            }
        }
        return result;
    }

    public enum Orientation {
        PORTRAIT, LANDSCAPE, NEITHER
    }

}
