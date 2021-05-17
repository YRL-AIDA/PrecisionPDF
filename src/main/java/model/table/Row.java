package model.table;

import java.util.ArrayList;
import java.util.List;

public class Row {
    private final int id;
    private final List<Cell> cells = new ArrayList<>();

    public Row(int id) {
        this.id = id;
    }

    public List<Cell> getCells() {
        return cells;
    }

    public int getId() {
        return id;
    }

    public boolean existCell(int cl, int cr) {
        for (Cell cell: cells){
            if (cell.getCl() == cl && cell.getCr() == cr)
                return true;
        }
        return false;
    }

    public void addCell(Cell cell) {
        cells.add(cell);
        cells.sort((c1, c2) -> Integer.compare(c1.getCl(), c2.getCl()));
    }

}
