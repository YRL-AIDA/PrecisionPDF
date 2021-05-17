package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.Page;
import model.Ruling;
import model.TextChunk;

import java.util.Iterator;

public class YRulingCompositionFilter implements BlockCompositionFilter {
    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        double minX = Math.min(block.getLeft(), textChunk.getLeft());
        double maxX = Math.max(block.getRight(), textChunk.getRight());
        double minY = block.getBottom();
        double maxY = textChunk.getTop();

        Page page = block.getPage();
        Iterator<Ruling> rulings = page.getRulings();
        while(rulings.hasNext()) {
            Ruling ruling = rulings.next();
            if (!ruling.isHorizontal()) continue;
            float x1 = (float) ruling.getStartPoint().getX();
            float x2 = (float) ruling.getEndPoint().getX();
            float y = (float) ruling.getStartPoint().getY();

            if (!(minY < y && y < maxY)) continue;
            if ((x1 <= minX && minX < x2) || (x1 < maxX && maxX <= x2)) return false;
        }
        return true;
    }

}
