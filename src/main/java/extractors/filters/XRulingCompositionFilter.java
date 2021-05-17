package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.Page;
import model.Ruling;
import model.TextChunk;

import java.util.Iterator;

public class XRulingCompositionFilter implements BlockCompositionFilter {

    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        double minX = block.getRight();
        double maxX = textChunk.getLeft();
        double minY = Math.min(block.getTop(), textChunk.getTop());
        double maxY = Math.max(block.getBottom(), textChunk.getBottom());

        Page page = block.getPage();
        Iterator<Ruling> rulings = page.getRulings();
        while(rulings.hasNext()) {
            Ruling ruling = rulings.next();
            if (!ruling.isVertical()) continue;
            float x = (float) ruling.getStartPoint().getX();
            float y1 = (float) ruling.getStartPoint().getY();
            float y2 = (float) ruling.getEndPoint().getY();

            if (!(minX < x && x < maxX)) continue;
            if ((y1 < minY && minY < y2) || (y1 < maxY && maxY < y2)) return false;
        }
        return true;
    }
}
