package extractors.filters;

import interfaces.BlockCompositionFilter;
import model.PDFFont;
import model.TextChunk;

public class XFontCompositionFilter implements BlockCompositionFilter {
    @Override
    public boolean canMerge(TextChunk block, TextChunk textChunk) {
        PDFFont blockFont = block.getFont();
        if (null != blockFont && blockFont.isBold()) {
            PDFFont chunkFont = textChunk.getFont();
            return blockFont.equals(chunkFont);
        }
        return true;
    }
}
