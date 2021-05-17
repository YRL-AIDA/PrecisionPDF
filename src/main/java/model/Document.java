package model;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.tools.ant.taskdefs.Manifest;
import pdreaders.PDContentExtractor;
import pdreaders.RulingExtractor;
import pdreaders.TextLinesExtractor;
import pdreaders.VisibleRulingExtractor;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Document implements Closeable {
    private final File sourceFile;
    private final PDDocument pdDocument;
    private final List<Page> pages = new ArrayList<>();
    private final PDContentExtractor contentExtractor;
    private final TextLinesExtractor textLineExtractor;
    private final VisibleRulingExtractor visibleRulingExtractor;
    //private final RulingExtractor realRulingExtractor;
    private final List<Manifest.Section> sections = new ArrayList<>();

    @Override
    public void close() throws IOException {
        pdDocument.close();
    }

    public static Document load(Path path) throws IllegalArgumentException, IOException {
        if (null == path) {
            throw new IllegalArgumentException("The path to a PDF document cannot be null");
        }
        else {
            File file = path.toFile();
            if (null == file)
                return null;

            if (file.exists() && file.canRead()) {
                PDDocument pdDocument = PDDocument.load(file);
                Document document = new Document(file, pdDocument);

                pdDocument.close();
                return document;
            } else
                return null;
        }
    }

    private Document(File file, PDDocument pdDocument) throws IllegalArgumentException, IOException {
        if (null == pdDocument) {
            throw new IllegalArgumentException("PDDocument cannot be null");
        }
        else {
            this.sourceFile = file;
            this.pdDocument = pdDocument;
            contentExtractor = new PDContentExtractor(this.pdDocument);
            textLineExtractor = new TextLinesExtractor(this.pdDocument);
            visibleRulingExtractor = new VisibleRulingExtractor(this.pdDocument);
            //realRulingExtractor = new RulingExtractor(this.pdDocument);
            textLineExtractor.setSortByPosition(true);
        }
        createPages();
    }

    private void createPages() throws IOException {
        int size = pdDocument.getNumberOfPages();
        for (int i = 0; i < size; i++) {
            Page page = createPage(i);
            pages.add(page);
        }
    }

    private Page createPage(int pageIndex) throws IOException {
        PDDocument pdDocument = this.getPdDocument();
        PDPage pdPage = pdDocument.getPage(pageIndex);

        if (null != pdPage) {
            PDRectangle rect = pdPage.getBBox();

            float left   = rect.getLowerLeftX();
            float top    = rect.getUpperRightY();
            float right  = rect.getUpperRightX();
            float bottom = rect.getLowerLeftY();

            Page page = new Page(this, pageIndex, left, bottom, right, top);

            // Only portrait pages are processed
            /*
            if (page.getOrientation() == Page.Orientation.PORTRAIT) {
                contentExtractor.process(page);
                textLineExtractor.process(page);
            } else {
                System.err.println("WARNING: The page is not processed because its paper orientation is not portrait");
            }
            */
            contentExtractor.process(page);
            textLineExtractor.process(page);
            visibleRulingExtractor.process(page);

            return page;
        }
        return null;
    }

    public PDDocument getPdDocument() {
        return pdDocument;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public Iterator<Page> getPages() {
        return pages.iterator();
    }

    public PDPage getPDPage(int index){
        return pdDocument.getPage(index) != null ? pdDocument.getPage(index) : null;
    }

    public Page getPage(int index) {
        for(Page page: pages) {
            if(page.getIndex() == index) return page;
        }
        return null;
    }
}