package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.TextChunk;

public class YAlignmentCompositionFilter implements BlockCompositionFilter {

    private static final float DEFAULT_MAX_LEFT_DEVIATION;
    private static final float DEFAULT_MAX_CENTER_DEVIATION;
    private static final float DEFAULT_MAX_RIGHT_DEVIATION;

    private static final float maxLeftDeviation;
    private static final float maxCenterDeviation;
    private static final float maxRightDeviation;

    static {
        DEFAULT_MAX_LEFT_DEVIATION = 0.5f;
        DEFAULT_MAX_CENTER_DEVIATION = 1.5f;
        DEFAULT_MAX_RIGHT_DEVIATION = 0.5f;
        // TODO: Read settings from properties
        maxLeftDeviation = DEFAULT_MAX_LEFT_DEVIATION;
        maxCenterDeviation = DEFAULT_MAX_CENTER_DEVIATION;
        maxRightDeviation = DEFAULT_MAX_RIGHT_DEVIATION;
    }

    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        if (block.getLeft() == textChunk.getLeft()) return true;
        double leftDeviation = Math.abs(block.getLeft() - textChunk.getLeft());
        if (leftDeviation < maxLeftDeviation) return true;

        double blockCenter = block.getLeft() + (block.getRight() - block.getLeft()) / 2;
        double chunkCenter = textChunk.getLeft() + (textChunk.getRight() - textChunk.getLeft()) / 2;
        if (Math.abs(blockCenter - chunkCenter) < maxCenterDeviation) return true;

        if (block.getRight() == textChunk.getRight()) return true;
        double rightDeviation = Math.abs(block.getRight() - textChunk.getRight());
        if (rightDeviation < maxRightDeviation) return true;

        return false;
    }
}
