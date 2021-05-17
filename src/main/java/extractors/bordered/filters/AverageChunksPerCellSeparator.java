package extractors.bordered.filters;

import extractors.bordered.Factors;
import interfaces.IBorderedTableSeparator;
import model.table.Cell;
import model.table.Table;
import org.apache.commons.collections4.IteratorUtils;

import java.util.Iterator;
import java.util.List;

public class AverageChunksPerCellSeparator implements IBorderedTableSeparator {
    @Override
    public boolean isFullBorderedTable(Table table) {
        Iterator<Cell> cellsIterator = table.getCells();
        List<Cell> cells = IteratorUtils.toList(cellsIterator);
        double averageBlocksPerCell = cells.stream()
                .mapToInt(s->s.getChunksCount())
                .average()
                .getAsDouble();
        return averageBlocksPerCell < Factors.MAX_BLOCKS_PER_CELL_FACTOR;
    }
}