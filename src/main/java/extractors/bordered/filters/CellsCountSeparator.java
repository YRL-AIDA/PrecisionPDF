package extractors.bordered.filters;


import extractors.bordered.Factors;
import interfaces.IBorderedTableSeparator;
import model.table.Table;

public class CellsCountSeparator implements IBorderedTableSeparator {
    @Override
    public boolean isFullBorderedTable(Table table) {
        return table.getNumOfCells() > Factors.MIN_CELLS_COUNT_FACTOR;
    }
}
