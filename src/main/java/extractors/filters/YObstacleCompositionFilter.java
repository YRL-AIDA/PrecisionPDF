package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.Page;
import model.TextChunk;

import java.util.Iterator;

public class YObstacleCompositionFilter implements BlockCompositionFilter {
    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        final double minX = Math.min(block.getLeft(), textChunk.getLeft());
        final double minY = block.getTop();
        final double maxX = Math.max(block.getRight(), textChunk.getRight());
        final double maxY = textChunk.getBottom();

        Page page = block.getPage();
        Iterator<TextChunk> obstacles = page.getBlocks();

        while (obstacles.hasNext()) {
            TextChunk o = obstacles.next();

            final double lt = o.getLeft();
            final double tp = o.getTop();
            final double rt = o.getRight();
            final double bm = o.getBottom();

            if (minX < rt && maxX > lt && minY < bm && maxY > tp) {
                if (block.getLeft() == lt && block.getTop() == tp && block.getRight() == rt && block.getBottom() == bm) continue;
                if (textChunk.getLeft() == lt && textChunk.getTop() == tp && textChunk.getRight() == rt && textChunk.getBottom() == bm) continue;
                return false;
            }
        }
        return true;
    }
}
