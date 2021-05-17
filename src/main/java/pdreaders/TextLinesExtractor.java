package pdreaders;

import model.Page;
import model.TextChunk;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TextLinesExtractor extends PDFTextStripper {

    private PDDocument document;          // A PDF document to process
    private final List<TextChunk> lines;  // Text lines composed from characters
    private final StringBuilder lineText;
    private final Point2D.Float lineStartPoint;
    private final Point2D.Float lineEndPoint;
    private boolean newLineStarted;

    private Page currentPage;

    public TextLinesExtractor(PDDocument document) throws IOException {
        lines = new ArrayList<>(500);
        lineText = new StringBuilder();
        lineStartPoint = new Point2D.Float(0,0);
        lineEndPoint = new Point2D.Float(0,0);
        this.document = document;
        newLineStarted = false;
    }

    public void process(Page page) {
        if (null == page) {
            throw new IllegalArgumentException("Page cannot be null");
        } else {
            currentPage = page;
            final int pageIndex = page.getIndex();
            try {
                stripPage(pageIndex);
                page.addLines(lines);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                release();
            }
        }
    }

    private void release() {
        lines.clear();
    }

    private void stripPage(int pageIndex) throws IOException {
        pageIndex += 1; // PDFBox page numbers are 1-based
        setStartPage(pageIndex);
        setEndPage(pageIndex);
        Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
        super.writeText(document, dummy);
    }

    @Override
    protected void startPage(PDPage page) throws IOException {
        newLineStarted = false;
        addLine();
        lineText.setLength(0);
        super.startPage(page);
    }

    @Override
    protected void writeLineSeparator() throws IOException  {
        newLineStarted = false;
        addLine();
        lineText.setLength(0);
        super.writeLineSeparator();
    }

    private void addLine() {
        if (StringUtils.isNotBlank(lineText)) {
            if (currentPage.canPrint(lineStartPoint) && currentPage.canPrint(lineEndPoint)) {
                TextChunk line = new TextChunk(lineStartPoint, lineEndPoint, lineText.toString(), currentPage);
                this.lines.add(line);
            }
        }
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {

        float minTop = Float.MAX_VALUE;
        float maxBottom = Float.MIN_VALUE;

        TextPosition lastTextPos = textPositions.get(textPositions.size() - 1);

        for (TextPosition tp : textPositions) {
            if (tp.getDir() != 0) {
                continue;
            }

            final float left   = tp.getXDirAdj();
            final float top    = tp.getYDirAdj() - tp.getHeightDir();
            final float right  = tp.getXDirAdj() + tp.getWidthDirAdj();
            final float bottom = tp.getYDirAdj();

            // Line coordinates
            if (maxBottom < bottom) {
                maxBottom = bottom;
            }
            if (minTop > top) {
                minTop = top;
            }

        }

        if (!newLineStarted) {
            TextPosition tp = textPositions.get(0);
            final float left = tp.getXDirAdj();
            final float top = tp.getYDirAdj() - tp.getHeightDir();
            lineStartPoint.setLocation(left, top);
            newLineStarted = true;
        }
        lineText.append(text);
        super.writeString(text, textPositions);

        final float right = lastTextPos.getXDirAdj() + lastTextPos.getWidthDirAdj();

        // Line processing
        lineEndPoint.setLocation(right, maxBottom);
    }
}
