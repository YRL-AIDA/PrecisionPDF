package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.PDFFont;
import model.TextChunk;

import java.awt.*;

public class XOrderCompositionFilter implements BlockCompositionFilter {

    private static final boolean DEFAULT_USE_NEXT_CHUNK;
    private static final float DEFAULT_SPACE_WIDTH_FACTOR;
    private static final float DEFAULT_TOP_MAX_DEVIATION;
    private static final float DEFAULT_BOTTOM_MAX_DEVIATION;

    static {
        DEFAULT_USE_NEXT_CHUNK = true;
        DEFAULT_SPACE_WIDTH_FACTOR = 5f;
        DEFAULT_TOP_MAX_DEVIATION = 1f;
        DEFAULT_BOTTOM_MAX_DEVIATION = 1f;
    }

    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        if (block.getEndOrder() == textChunk.getStartOrder()) return true;

        if (DEFAULT_USE_NEXT_CHUNK) {
            if (block.getEndOrder() == textChunk.getStartOrder() - 1) {
                PDFFont blockFont = block.getFont();
                if (null != blockFont && !blockFont.equals(textChunk.getFont())) return false;

                double topDeviation = Math.abs(block.getTop() - textChunk.getTop());
                if (topDeviation > DEFAULT_TOP_MAX_DEVIATION) return false;

                double bottomDeviation = Math.abs(block.getBottom() - textChunk.getBottom());
                if (bottomDeviation > DEFAULT_BOTTOM_MAX_DEVIATION) return false;

                double distance = textChunk.getLeft() - block.getRight();
                double maxAccessibleDistance = block.getSpaceWidth() * DEFAULT_SPACE_WIDTH_FACTOR;
                if (distance < maxAccessibleDistance) return true;
            }
        }
        return false;
    }

}
