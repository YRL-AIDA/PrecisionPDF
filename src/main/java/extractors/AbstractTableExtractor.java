package extractors;

import model.PDFRectangle;
import model.Page;
import model.TextChunk;
import model.table.Table;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class AbstractTableExtractor {
    private Page page;

    protected AbstractTableExtractor(Page page) {
        this.page = page;
    }

    protected Page getPage() {
        return page;
    }

    protected abstract List<Table> extract();

    protected final TextChunk mergeCellBlocks(List<TextChunk> blocks) {
        if (null == blocks || blocks.isEmpty())
            return null;

        if (blocks.size() == 1)
            return blocks.get(0);

        // New algorithm
        List<TextChunk> inLineBlocks = new ArrayList();
        List<TextChunk> linedBlocks = new ArrayList();

        blocks.sort(Comparator.comparing(PDFRectangle::getTop));

        TextChunk block0 = blocks.get(0);
        double lineBottom = block0.getBottom();
        inLineBlocks.add(block0);

        for (int i = 1; i < blocks.size(); i++) {
            TextChunk block = blocks.get(i);
            double tp = block.getTop();
            double bm = block.getBottom();
            if (tp <= lineBottom && lineBottom <= bm) {
                lineBottom = bm;
                inLineBlocks.add(block);
            } else {
                TextChunk linedBlock = mergeInLineBlocks(inLineBlocks);
                linedBlocks.add(linedBlock);
                inLineBlocks.clear();

                inLineBlocks.add(block);
                lineBottom = block.getBottom();
            }
        }
        TextChunk linedBlock = mergeInLineBlocks(inLineBlocks);
        linedBlocks.add(linedBlock);

        TextChunk result = mergeLinedBlocks(linedBlocks);
        return result;

    }

    private TextChunk mergeInLineBlocks(List<TextChunk> inLineBlocks) {
        if (inLineBlocks.isEmpty())
            return null;

        if (inLineBlocks.size() == 1)
            return inLineBlocks.get(0);

        inLineBlocks.sort(Comparator.comparing(PDFRectangle::getLeft));
        TextChunk result = inLineBlocks.get(0);

        double left = result.getLeft();
        double bottom = result.getBottom();
        double right = result.getRight();
        double top = result.getTop();

        String text = result.getText();
        int startOrder = result.getEndOrder();
        int endOrder = result.getEndOrder();

        String separator = " ";

        for (int i = 1; i < inLineBlocks.size(); i++) {
            TextChunk block = inLineBlocks.get(i);
            String blockText = block.getText();

            if (StringUtils.isNotBlank(blockText))
                text = text.concat(separator).concat(blockText);

            left = Math.min(left, block.getLeft());
            top = Math.min(top, block.getTop());
            right = Math.max(right, block.getRight());
            bottom = Math.max(bottom, block.getBottom());

            startOrder = Math.min(startOrder, block.getStartOrder());
            endOrder = Math.max(endOrder, block.getEndOrder());

            // Remove merged blocks except the first one from the page
            block.retract();
        }

        result.setLeft(left);
        result.setTop(top);
        result.setRight(right);
        result.setBottom(bottom);

        result.setText(text);
        result.setStartOrder(startOrder);
        result.setEndOrder(endOrder);

        result.updateTextLine();
        return result;
    }

    private TextChunk mergeLinedBlocks(List<TextChunk> linedBlocks) {
        if (linedBlocks.isEmpty())
            return null;

        if (linedBlocks.size() == 1)
            return linedBlocks.get(0);

        linedBlocks.sort(Comparator.comparing(PDFRectangle::getTop));
        TextChunk result = linedBlocks.get(0);

        double left = result.getLeft();
        double bottom = result.getBottom();
        double right = result.getRight();
        double top = result.getTop();

        String text = result.getText();
        int startOrder = result.getEndOrder();
        int endOrder = result.getEndOrder();

        String separator = System.lineSeparator();

        for (int i = 1; i < linedBlocks.size(); i++) {
            TextChunk block = linedBlocks.get(i);

            String blockText = block.getText();
            if (StringUtils.isNotBlank(blockText))
                text = text.concat(separator).concat(blockText);

            left = Math.min(left, block.getLeft());
            top = Math.min(top, block.getTop());
            right = Math.max(right, block.getRight());
            bottom = Math.max(bottom, block.getBottom());

            startOrder = Math.min(startOrder, block.getStartOrder());
            endOrder = Math.max(endOrder, block.getEndOrder());

            result.newTextLine(block);

            // Remove merged blocks except the first one from the page
            block.retract();
        }

        result.setLeft(left);
        result.setTop(top);
        result.setRight(right);
        result.setBottom(bottom);

        result.setText(text);
        result.setEndOrder(endOrder);

        return result;
    }
}