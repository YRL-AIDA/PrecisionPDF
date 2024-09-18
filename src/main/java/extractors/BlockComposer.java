package extractors;

import extractors.filters.*;
import interfaces.BlockCompositionFilter;
import model.Document;
import model.PDFFont;
import model.Page;
import model.TextChunk;
import org.apache.commons.collections4.IteratorUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockComposer {

    private static final BlockCompositionFilter[] xFilters;
    private static final XOverlappingCharacterFilter wordMergingFilter;
    private static final BlockCompositionFilter[] yFilters;
    private static final BlockCompositionFilter[] yLinesFilters;

    static {
        xFilters = new BlockCompositionFilter[] {
                new XOrderCompositionFilter(),
                new XCoherenceCompositionFilter(),
                new XWordSpaceCompositionFilter(),
                new XFontCompositionFilter(),
                new XColorCompositionFilter(),
                new XRulingCompositionFilter()
        };


        wordMergingFilter = new XOverlappingCharacterFilter();

        yFilters = new BlockCompositionFilter[] {
                new YOrderCompositionFilter(),
                new YInterLineSpaceCompositionFilter(),
                new YAlignmentCompositionFilter(),
                new YFontCompositionFilter(),
                new YRulingCompositionFilter(),
                new YObstacleCompositionFilter(),
                new YStrongOrderCompositionFilter()
        };

        yLinesFilters = new BlockCompositionFilter[] {
                new YSameLineFilter(),
                new YStrongOrderCompositionFilter(),
                new YSameLineSpaceFilter(),
                new XRulingCompositionFilter(),
                new YRulingCompositionFilter()
        };

    }

    public void compose(Document document) {
        for(Iterator<Page> pages = document.getPagesItrerator(); pages.hasNext();) {
            Page page = pages.next();
            compose(page);
        }
    }

    public void compose(Document document, int startPage, int endPage) {
        for (int i = startPage; i <= endPage; i++) {
            Page page = document.getPage(i);
            compose(page);
        }
    }

    private void compose(Page page) {
        List<TextChunk> words = IteratorUtils.toList(page.getWords());
        determineWordCoherence(words); // Calculate coherence
        composeWords(page);
        composeBlocks(page);
        composeLine(page);
    }

    private void determineWordCoherence(List<TextChunk> words) {
        final float maxLeftDeviation = 1f;

        for (int i = 0; i < words.size(); i++) {
            TextChunk currentWord = words.get(i);
            int coherence = 0;
            double minLeft = currentWord.getLeft() - maxLeftDeviation;
            double maxLeft = currentWord.getLeft() + maxLeftDeviation;

            for (int j = 0; j < words.size(); j++) {
                if (j == i) continue;

                TextChunk coherentWord = words.get(j);
                double deviation = Math.abs(currentWord.getLeft() - coherentWord.getLeft());

                if (deviation < maxLeftDeviation) {
                    double tp  = 0f, bm  = 0f;

                    if (currentWord.getBottom() < coherentWord.getTop()) {
                        tp = currentWord.getBottom();
                        bm = coherentWord.getTop();
                    } else if (currentWord.getTop() > coherentWord.getBottom()) {
                        tp = coherentWord.getBottom();
                        bm = currentWord.getTop();
                    }

                    boolean noObstacleWords = true;
                    for (int k = 0; k < words.size(); k++) {
                        if (k == i || k == j) continue;
                        TextChunk obstacleWord = words.get(k);

                        if (tp < obstacleWord.getTop() && obstacleWord.getBottom() < bm) {
                            if (obstacleWord.getLeft() < minLeft && maxLeft < obstacleWord.getRight()) {
                                noObstacleWords = false;
                                break;
                            }
                        }
                    }

                    if (noObstacleWords) {
                        coherence ++;
                    }
                }
                currentWord.setCoherence(coherence);
            }
        }
    }

    private void composeWords(Page page) {
        List<TextChunk> words = IteratorUtils.toList(page.getWords());
        List<TextChunk> blocks = new ArrayList<>();
        List<TextChunk> blockedWords = new ArrayList<>();

        while (! words.isEmpty()) {
            TextChunk word = words.get(0);
            blockedWords.add(word);

            TextChunk block = copyTextChunk(word);
            blocks.add(block);

            for (int i = 1; i < words.size(); i ++) {
                word = words.get(i);

                // Check the case when the end character of the block overlaps the start character of the word
                if (wordMergingFilter.canMerge(block, word)) {
                    composeBlock(block, word, "");
                    block.updateTextLine();
                    blockedWords.add(word);
                }
                else {
                    boolean canMerge = true;
                    for (BlockCompositionFilter filter : xFilters) {
                        if (!filter.canMerge(block, word)) {
                            canMerge = false;
                            break;
                        }
                    }
                    if (canMerge) {
                        composeBlock(block, word, " ");
                        block.updateTextLine();
                        blockedWords.add(word);
                    }
                }
            }
            words.removeAll(blockedWords);
            blockedWords.clear();
        }
        page.addBlocks(blocks);
    }

    private void composeLine(Page page) {
        List<TextChunk> blocks = IteratorUtils.toList(page.getBlocks());
        List<TextChunk> blockedWords = new ArrayList<>();

        while (! blocks.isEmpty()) {
            TextChunk oldBlock = blocks.get(0);
            blockedWords.add(oldBlock);

            TextChunk newBlock = oldBlock;
            blocks.add(newBlock);

            for (int i = 1; i < blocks.size(); i ++) {
                oldBlock = blocks.get(i);

                boolean canMerge = true;

                for (BlockCompositionFilter filter: yLinesFilters) {
                    if (!filter.canMerge(newBlock, oldBlock)) {
                        canMerge = false;
                        break;
                    }
                }
                if (canMerge) {
                    composeLine(newBlock, oldBlock, " ");
                    newBlock.newTextLine(oldBlock);
                    blockedWords.add(oldBlock);
                    page.removeBlock(oldBlock);
                }
            }
            blocks.removeAll(blockedWords);
            blockedWords.clear();
        }

        blocks = IteratorUtils.toList(page.getBlocks());

        for (int i = 0; i < blocks.size(); i ++) {
            String s = blocks.get(i).getText().concat(System.lineSeparator());
            blocks.get(i).setText(s);
        }

    }

    private void composeBlocks(Page page) {
        List<TextChunk> blocks = IteratorUtils.toList(page.getBlocks());
        List<TextChunk> blockedWords = new ArrayList<>();

        while (! blocks.isEmpty()) {
            TextChunk oldBlock = blocks.get(0);
            blockedWords.add(oldBlock);

            TextChunk newBlock = oldBlock;
            blocks.add(newBlock);

            for (int i = 1; i < blocks.size(); i ++) {
                oldBlock = blocks.get(i);

                boolean canMerge = true;
                for (BlockCompositionFilter filter: yFilters) {
                    if (!filter.canMerge(newBlock, oldBlock)) {
                        canMerge = false;
                        break;
                    }
                }
                if (canMerge) {
                    composeBlock(newBlock, oldBlock, System.lineSeparator());
                    newBlock.newTextLine(oldBlock);
                    blockedWords.add(oldBlock);
                    page.removeBlock(oldBlock);
                }
            }
            blocks.removeAll(blockedWords);
            blockedWords.clear();
        }
    }

    private TextChunk copyTextChunk(TextChunk from) {
        double left   = from.getLeft();
        double bottom = from.getBottom();
        double right  = from.getRight();
        double top    = from.getTop();

        String text = from.getText();
        Page page = from.getPage();

        PDFFont font = from.getFont().copyFont();
        Color color = from.getColor();
        float spaceWidth = from.getSpaceWidth();
        int startOrder = from.getStartOrder();
        int endOrder = from.getEndOrder();

        TextChunk copy = new TextChunk(left, top, right, bottom, text, page);
        copy.setFont(font);
        copy.setColor(color);
        copy.setSpaceWidth(spaceWidth);
        copy.setStartOrder(startOrder);
        copy.setEndOrder(endOrder);
        copy.updateTextLine();

        return copy;
    }

    public void composeBlock(TextChunk block, TextChunk textChunk, String separator) {
        if (block.equals(textChunk)) return;

        double left   = Math.min(block.getLeft(), textChunk.getLeft());
        double bottom = Math.max(block.getBottom(), textChunk.getBottom());
        double right  = Math.max(block.getRight(), textChunk.getRight());
        double top    = Math.min(block.getTop(), textChunk.getTop());

        block.setLeft(left);
        block.setBottom(bottom);
        block.setRight(right);
        block.setTop(top);

        //String s = block.getText().concat(" ").concat(textChunk.getText());
        String s = block.getText().concat(separator).concat(textChunk.getText());

        block.setText(s);

        PDFFont blockFont = block.getFont();
        if (null != blockFont) {
            PDFFont chunkFont = textChunk.getFont();
            if (null != chunkFont) {
                blockFont.setName(chunkFont.getName());
                blockFont.setItalic(chunkFont.isItalic());
                blockFont.setBold(chunkFont.isBold());
                blockFont.setHeight(chunkFont.getHeight());
            }
        }

        Color chunkColor = block.getColor();
        if (null != chunkColor) {
            block.setColor(chunkColor);
        }

        block.setEndOrder(textChunk.getEndOrder());
    }

    public void composeLine(TextChunk block, TextChunk textChunk, String separator) {
        if (block.equals(textChunk)) return;

        double left   = Math.min(block.getLeft(), textChunk.getLeft());
        double bottom = Math.max(block.getBottom(), textChunk.getBottom());
        double right  = Math.max(block.getRight(), textChunk.getRight());
        double top    = Math.min(block.getTop(), textChunk.getTop());

        block.setLeft(left);
        block.setBottom(bottom);
        block.setRight(right);
        block.setTop(top);

        //String s = block.getText().concat(" ").concat(textChunk.getText());
        String s = block.getText().concat(separator).concat(textChunk.getText());

        block.setText(s);

        PDFFont blockFont = block.getFont();
        if (null != blockFont) {
            PDFFont chunkFont = textChunk.getFont();
            if (null != chunkFont) {
                blockFont.setName(chunkFont.getName());
                blockFont.setItalic(chunkFont.isItalic());
                blockFont.setBold(chunkFont.isBold());
                blockFont.setHeight(chunkFont.getHeight());
            }
        }

        Color chunkColor = block.getColor();
        if (null != chunkColor) {
            block.setColor(chunkColor);
        }

        block.setEndOrder(textChunk.getEndOrder());
    }


}
