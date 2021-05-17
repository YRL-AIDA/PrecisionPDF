package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.TextChunk;

public class XOverlappingCharacterFilter implements BlockCompositionFilter {

    private static final float DEFAULT_EPSILON = 0.01f;
    private static final float epsilon;

    static {
        // TODO: Read settings from properties
        epsilon = DEFAULT_EPSILON;
    }

    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        if (Math.abs(block.getTop() - textChunk.getTop()) > epsilon)
            return false;

        if (Math.abs(block.getBottom() - textChunk.getBottom()) > epsilon)
            return false;

        double distance = textChunk.getLeft() - block.getRight();
        // The distance must be negative but more than the space width
        return distance < 0f && distance > -block.getSpaceWidth();
    }
}
