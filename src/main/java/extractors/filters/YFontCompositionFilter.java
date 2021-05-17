package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.PDFFont;
import model.TextChunk;

public class YFontCompositionFilter implements BlockCompositionFilter {

    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        PDFFont blockFont = block.getFont();
        PDFFont chunkFont = textChunk.getFont();
        if (null == blockFont && null == chunkFont)
            return true;
        return null != blockFont && blockFont.equals(chunkFont);
    }
}
