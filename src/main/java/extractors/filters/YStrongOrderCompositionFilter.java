package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.Page;
import model.TextChunk;

import java.util.Iterator;

public class YStrongOrderCompositionFilter implements BlockCompositionFilter {
    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        if (block.getEndOrder() == textChunk.getStartOrder() - 1) {
            Page page = block.getPage();
            Iterator<TextChunk> blocks = page.getBlocks();

            int order = textChunk.getStartOrder();

            int counter = 0;
            while (blocks.hasNext()) {
                TextChunk b = blocks.next();
                if (order == b.getStartOrder()) counter ++;
                if (counter > 1) return false;
            }
            return true;
        }
        return false;
    }
}