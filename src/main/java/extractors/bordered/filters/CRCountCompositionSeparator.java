package extractors.bordered.filters;

import extractors.bordered.Factors;
import interfaces.IBorderedTableSeparator;
import model.table.Table;

public class CRCountCompositionSeparator implements IBorderedTableSeparator {
    @Override
    public boolean isFullBorderedTable(Table table) {
        return table.getNumOfColumn() > Factors.MIN_ROW_COUNT_FACTOR
                & table.getNumOfRows() > Factors.MIN_COLUMN_COUNT_FACTOR;
    }
}