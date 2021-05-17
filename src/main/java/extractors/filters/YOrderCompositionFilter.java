package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.TextChunk;

public class YOrderCompositionFilter implements BlockCompositionFilter {

    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        //return block.getEndOrder() == textChunk.getStartOrder() - 1 || block.getEndOrder() == textChunk.getStartOrder();
        return true;
                //block.getEndOrder() == textChunk.getStartOrder() - 1;
    }
}