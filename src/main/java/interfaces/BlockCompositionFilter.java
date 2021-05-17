package interfaces;

import model.TextChunk;

public interface BlockCompositionFilter {
    boolean canMerge(TextChunk block, TextChunk textChunk);
}