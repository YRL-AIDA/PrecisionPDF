package extractors.bordered.filters;

import interfaces.IBorderedTableSeparator;
import model.table.Table;

public class BorderedTableSeparator {

    private final IBorderedTableSeparator[] borderedTableSeparators;

    public BorderedTableSeparator() {
        borderedTableSeparators = new IBorderedTableSeparator[] {
                new CellsCountSeparator(),
                new CRCountCompositionSeparator(),
                new AverageChunksPerCellSeparator(),
                new CellsCountSeparator()
        };
    }

    public boolean isFullBorderedTable(Table table) {
        for (IBorderedTableSeparator bts : borderedTableSeparators) {
            if (!bts.isFullBorderedTable(table)) {
                return false;
            }
        }
        return true;
    }

}