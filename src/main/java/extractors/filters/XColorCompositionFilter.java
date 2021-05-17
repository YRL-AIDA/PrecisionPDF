package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.TextChunk;

import java.awt.*;

public class XColorCompositionFilter  implements BlockCompositionFilter {
    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        Color color = block.getColor();
        if (null == color) return true;
        return color.equals(textChunk.getColor());
    }
}

