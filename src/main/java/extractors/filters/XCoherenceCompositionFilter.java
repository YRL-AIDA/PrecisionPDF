package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.TextChunk;

public class XCoherenceCompositionFilter implements BlockCompositionFilter {
    private static final float DEFAULT_MAX_SPACE_DISTANCE_FACTOR;
    private static final int DEFAULT_MAX_LEFT_COHERENCE;
    private static float maxSpaceDistanceFactor;
    private static final int maxLeftCoherence;

    static {
        DEFAULT_MAX_SPACE_DISTANCE_FACTOR = 1.2f;
        DEFAULT_MAX_LEFT_COHERENCE = 2;
        // TODO: Read settings from properties
        maxSpaceDistanceFactor = DEFAULT_MAX_SPACE_DISTANCE_FACTOR;
        maxLeftCoherence = DEFAULT_MAX_LEFT_COHERENCE;
    }

    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        double distance = textChunk.getLeft() - block.getRight();
        double maxSpaceDistance = block.getSpaceWidth() * maxSpaceDistanceFactor;
        if (distance < maxSpaceDistance) return true;
        return textChunk.getCoherence() < maxLeftCoherence;
    }
}