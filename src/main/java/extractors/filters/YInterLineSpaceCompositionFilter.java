package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.TextChunk;

public class YInterLineSpaceCompositionFilter implements BlockCompositionFilter {

    private static final float DEFAULT_INTER_LINE_SPACING_FACTOR = 2.2f;
    private static final float DEFAULT_INTER_LINE_SPACING_ADDITION = 0f;

    private static final float interLineSpacingFactor;
    private static final float interLineSpacingAddition;

    static {
        // TODO: Read settings from properties
        interLineSpacingFactor = DEFAULT_INTER_LINE_SPACING_FACTOR;
        interLineSpacingAddition = DEFAULT_INTER_LINE_SPACING_ADDITION;
    }

    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        // TODO: Modify this code. The inter line spacing should be set from a font height
        double interLineSpacing = textChunk.getBottom() - textChunk.getTop();
        double distance = textChunk.getTop() - block.getBottom();
        if (distance < 0) return false;

        double maxAccessibleDistance = interLineSpacing * interLineSpacingFactor + interLineSpacingAddition;
        return distance <= maxAccessibleDistance;
    }
}