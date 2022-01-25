package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.TextChunk;

public class YSameLineFilter implements BlockCompositionFilter {

    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        return (Math.abs(block.getBottom() - textChunk.getBottom()) < 3);
    }
}
