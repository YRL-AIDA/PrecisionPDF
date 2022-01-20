package model.table;

import extractors.bordered.Range;
import model.PDFRectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Table extends PDFRectangle {
    private String code;
    private final List<Cell> cells;
    private final List<Row> rows;
    private int pageIndex;
    private int numOfPages;
    private boolean combined;  // true, if the table was created by combining several other tables
    //private boolean bordered;  // true, if the table is extracted by bordered table extractor
    private boolean continued; // true, if the table was used to create a combined table
    private TableType type;

    public ArrayList<Range> getHorizontal() {
        return horizontal;
    }

    public void setHorizontal(ArrayList<Range> horizontal) {
        this.horizontal = horizontal;
    }

    private ArrayList<Range> horizontal = new ArrayList<>();

    public ArrayList<Range> getVertical() {
        return vertical;
    }

    public void setVertical(ArrayList<Range> vertical) {
        this.vertical = vertical;
    }

    private ArrayList<Range> vertical = new ArrayList<>();

    {
        rows = new ArrayList();
        cells = new ArrayList();
    }

    public Table(double left, double top, double right, double bottom, TableType type) {
        super(left, top, right, bottom);
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Iterator<Cell> getCells() {
        return cells.iterator();
    }

    public void addCell(Cell cell, int rowId) {
        if (rows.size() < rowId + 1) {
            for (int i = rows.size(); i < rowId + 1; i++) {
                rows.add(new Row(rowId));
            }
        }
        rows.get(rowId).addCell(cell);

        cells.add(cell); // I added this code to read cells in the draw debugging (A. Shigarov)
    }

    public int getNumOfRows() {
        return rows.size();
    }

    public int getNumOfCells() {
        return cells.size();
    }

    public int getNumOfColumn() {
        int result = 0;
        for (Cell cell: cells) {
            int columnCount = cell.getCr() + 1;
            if (result < columnCount)
                result = columnCount;
        }
        return result;
    }

    public List<Row> getRows() {
        return rows;
    }

    public Row getRow(int rowNumber) {
        return rows.get(rowNumber);
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int index) {
        pageIndex = index;
    }

    public int getNumOfPages() {
        return numOfPages;
    }

    private void setNumOfPages(int n) {
        assert (n > 0);
        numOfPages = n;
    }

    public boolean isBordered() {
        return type == TableType.FULL_BORDERED || type == TableType.PARTIAL_BORDERED;
    }

    public boolean isCombined() {
        return combined;
    }

    public boolean isContinued() {
        return continued;
    }

    public TableType getType() {
        return type;
    }

    public void setType(TableType type) {
        this.type = type;
    }

    public void removeEmptyRows(){
        Iterator<Row> rowIterator = rows.iterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            int rowWithContentCnt = 0;
            for (Cell c: row.getCells()) {
                if (!c.isEmpty()){
                    rowWithContentCnt++;
                }
            }
            if (rowWithContentCnt == 0) {
                rowIterator.remove();
            }
        }
    }

    public void completeRows(){
        int colCnt = getNumOfColumn();
        for (Row r: rows) {
            List<Cell> cells = r.getCells();
            if (cells.size() != colCnt) {
                for (int i = 0; i < colCnt; i++ ) {
                    if (!r.existCell(i,i)) {
                        PDFRectangle rec = new PDFRectangle(0, 0, 0, 0);
                        r.addCell(new Cell(rec, null, i, r.getId(), i, r.getId()));
                    }
                }
            }
        }
    }

    public void printTable(){
        for (Row r: rows){
            List<Cell> cellList = r.getCells();
            int cSize = cellList.size();
            for (int i = 0; i < cSize - 1; i++){
                Cell c = cellList.get(i);
                System.out.printf("%s;", c.getText());
            }
            System.out.printf("%s", cellList.get(cSize - 1).getText());
        }
    }

    public void splitCells(){
        Iterator<Row> rowIterator = rows.iterator();
        while (rowIterator.hasNext()){
            Row row = rowIterator.next();
            ArrayList<Cell> splitedCells = new ArrayList<>();
            Iterator<Cell> cellsIterator = row.getCells().iterator();
            while (cellsIterator.hasNext()) {
                Cell cell = cellsIterator.next();
                int cl = cell.getCl();
                int cr = cell.getCr();
                int rt = cell.getRt();
                int rb = cell.getRb();
                int l = cr - cl;
                int w = rb - rt;
                if (l > 0) {
                    cell.setCl(cl);
                    cell.setCr(cl);
                    for (int i = cl + 1; i <= cr; i++){
                        PDFRectangle rec = new PDFRectangle(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());
                        splitedCells.add(new Cell(rec, cell.getTextBlocks(), i, rt, i, rt));
                    }
                }
                if (w > 0) {
                    cell.setRt(rt);
                    cell.setRb(rt);
                    for (int i = rt + 1; i < rb; i++){
                        Row r = rows.get(i);
                        r.addCell(new Cell(cell, cell.getTextBlocks(), cl, i, cr, i));
                    }
                }
            }
            for (Cell c: splitedCells){
                row.addCell(c);
            }
        }
    }
}
