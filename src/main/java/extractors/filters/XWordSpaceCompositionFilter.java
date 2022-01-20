package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.TextChunk;

public class XWordSpaceCompositionFilter implements BlockCompositionFilter {

    private static final float DEFAULT_WORD_SPACING_FACTOR = 5f;
    private static final float DEFAULT_WORD_SPACING_ADDITION = 5f;

    private static final float wordSpacingFactor;
    private static final float wordSpacingAddition;

    static {
        // TODO: Read settings from properties
        wordSpacingFactor = DEFAULT_WORD_SPACING_FACTOR;
        wordSpacingAddition = DEFAULT_WORD_SPACING_ADDITION;
    }

    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        double distance = textChunk.getLeft() - block.getRight();
        if (distance < 0) return false;
        double maxAccessibleDistance = block.getSpaceWidth() * wordSpacingFactor + wordSpacingAddition;
        return distance <= maxAccessibleDistance;
    }
}
