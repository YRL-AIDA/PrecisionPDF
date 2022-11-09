package model;

import org.apache.commons.io.FilenameUtils;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFMarkedContentExtractor;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.pdfbox.util.Matrix;
import org.apache.tools.ant.taskdefs.Manifest;
import org.w3c.dom.Element;
import pdreaders.PDContentExtractor;
import pdreaders.VisibleRulingExtractor;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class Document implements Closeable {
    private final File sourceFile;
    private final PDDocument pdDocument;
    private final List<Page> pages = new ArrayList<>();
    private final PDContentExtractor contentExtractor;
    private final VisibleRulingExtractor visibleRulingExtractor;
    private int index = 0;
    private int pageCnt = 0;
    Map<PDPage, Page> taggedPages = new HashMap<>();

    @Override
    public void close() throws IOException {
        pdDocument.close();
    }

    public static Document load(Path path, int startPage, int endPage) throws IllegalArgumentException, IOException {

        if (path == null) {
            throw new IllegalArgumentException("The path to a PDF document cannot be null");
        }

        else {

            File file = path.toFile();

            if (file == null) {
                return null;
            }

            if (file.exists() && file.canRead()) {
                PDDocument pdDocument = Loader.loadPDF(path.toFile());
                Document document = new Document(file, pdDocument, startPage, endPage, pdDocument.getNumberOfPages());
                document.parseTags();
                document.annotateLines();
                pdDocument.close();
                return document;
            } else
                return null;
        }
    }

    private Document(File file, PDDocument pdDocument, int startPage, int endPage, int pageCnt) throws IllegalArgumentException, IOException {
        if (null == pdDocument) {
            throw new IllegalArgumentException("PDDocument cannot be null");
        }
        else {
            this.pageCnt = pageCnt;
            this.sourceFile = file;
            this.pdDocument = pdDocument;
            contentExtractor = new PDContentExtractor(this.pdDocument);
            visibleRulingExtractor = new VisibleRulingExtractor(this.pdDocument);
        }
        createPages(startPage, endPage);
    }

    public int getPageCnt(){
        return this.pageCnt;
    }
    private void createPages(int startPage, int endPage) throws IOException {
        int size = pdDocument.getNumberOfPages();
        for (int i = startPage; i <= endPage; i++) {
            Page page = createPage(i);
            pages.add(page);
            taggedPages.put(pdDocument.getPage(i), page);
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

    public Iterator<Page> getPagesItrerator() {
        return pages.iterator();
    }
    public List<Page> getPages() {
        return pages;
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
    public void annotateLines(){
        for (Page page: pages) {
            for (Tag tag: page.getTags()) {
                for (TextChunk line : page.getTextLines()) {
                    if (line.intersects(tag.getRect())) {
                        line.setMetadata(tag.getName().toString());
                    }
                }
            }
        }

    }
    public void parseTags() throws IOException {

        Map<PDPage, Map<Integer, PDMarkedContent>> markedContents = new HashMap<>();
        Map<PDPage, Rectangle2D> boxes;
        boxes = new HashMap<PDPage, Rectangle2D>();

        for (PDPage page : pdDocument.getPages()) {
            PDFMarkedContentExtractor extractor = new PDFMarkedContentExtractor();
            extractor.processPage(page);

            Map<Integer, PDMarkedContent> theseMarkedContents = new HashMap<>();
            markedContents.put(page, theseMarkedContents);
            for (PDMarkedContent markedContent : extractor.getMarkedContents()) {
                addToMap(theseMarkedContents, markedContent);
            }
        }

        //processImagesFromPDF(document);

        PDStructureNode root = pdDocument.getDocumentCatalog().getStructureTreeRoot();

        if (root == null)
            return;

        //Map<PDPage, PDPageContentStream> visualizations = new HashMap<>();
        boxes = showStructure(this.pdDocument, root, markedContents);

        for (int i = 0; i < pdDocument.getNumberOfPages(); i ++) {
            PDPage currPage = pdDocument.getPage(i);
            Page page = taggedPages.get(currPage);
            if (page == null) continue;
/*            for (Tag tag : page.getTags()) {
                if (tag.getName().equals(TagsName.FOOTNOTE))
                    System.out.println(tag.getName());
            }*/
        }
        //pdDocument.close();
    }

    void addToMap(Map<Integer, PDMarkedContent> theseMarkedContents, PDMarkedContent markedContent) {
        theseMarkedContents.put(markedContent.getMCID(), markedContent);
        for (Object object : markedContent.getContents()) {
            if (object instanceof PDMarkedContent) {
                addToMap(theseMarkedContents, (PDMarkedContent)object);
            }
        }
    }

    Map<PDPage, Rectangle2D> showStructure(PDDocument document,
                                           PDStructureNode node, Map<PDPage, Map<Integer,
            PDMarkedContent>> markedContents) throws IOException {
        Map<PDPage, Rectangle2D> boxes = null;
        Map<PDPage, Rectangle2D> result = new HashMap<>();

        String structType = null;
        PDPage page = null;
        if (node instanceof PDStructureElement) {
            PDStructureElement element = (PDStructureElement) node;
            structType = element.getStructureType();
            page = element.getPage();
        }
        Map<Integer, PDMarkedContent> theseMarkedContents = markedContents.get(page);
        int indexHere = index++;
        //System.out.printf("<%s index=%s>\n", structType, indexHere);
        for (Object object: node.getKids()) {
            if (object instanceof COSArray) {
                for (COSBase base : (COSArray) object) {
                    if (base instanceof COSDictionary) {
                        boxes = union(boxes, showStructure(document, PDStructureNode.create((COSDictionary) base), markedContents));
                    } else if (base instanceof COSNumber) {
                        boxes = union(boxes, page, this.showContent(((COSNumber)base).intValue(), theseMarkedContents));
                    } else {
                        //System.out.printf("?%s\n", base);
                    }
                }
            } else if (object instanceof PDStructureNode) {
                boxes = union(boxes, showStructure(document, (PDStructureNode) object, markedContents));
            } else if (object instanceof Integer) {
                boxes = union(boxes, page, this.showContent((Integer)object, theseMarkedContents));
            } else if (object instanceof PDMarkedContentReference) {
                page = ((PDMarkedContentReference) object).getPage();
                theseMarkedContents = markedContents.get(page);
                boxes = union(boxes, page, showContent(((PDMarkedContentReference) object).getMCID(), theseMarkedContents));
            } else {
                //System.out.printf("?%s\n", object);
            }
        }

        //System.out.printf("</%s>\n", structType);

        if (boxes != null && structType != null) {
            Color color = new Color((int)(Math.random() * 256), (int)(Math.random() * 256), (int)(Math.random() * 256));

            for (Map.Entry<PDPage, Rectangle2D> entry : boxes.entrySet()) {
                page = entry.getKey();
                Rectangle2D box = entry.getValue();

                if (box == null) {
                    continue;
                }

                result.put(page, box);

/*
                if (structType.equals("LBody") || structType.equals("TR")
                        || structType.equals("Div")  || structType.equals("Document")
                        || structType.equals("Form") || structType.equals("Span")
                        || structType.equals("Normal (Web)") || structType.equals("Footnote")
                        || structType.equals("Link") || structType.equals("Footnote")
                ) {
                    continue;
                }
*/

                Page p = taggedPages.get(page);

                if (p == null) {
                    continue;
                }

                //if (structType.equals("Table")) {
                //    p.addTag(new Tag(TagsName.TABLE, box));
                //} else if (structType.equals("Figure")) {
                //    p.addTag(new Tag(TagsName.FIGURE, box));
                //} else

                if (structType.equals("Footnote")) {
                    Rectangle2D rec = new Rectangle2D.Float((float)box.getMinX(), (float)page.getBBox().getHeight() - (float)box.getMaxY(), (float)box.getWidth(), (float)box.getHeight());
                    p.addTag(new Tag(TagsName.FOOTNOTE, rec));
                } else if (structType.equals("RunningTitle")) {
                    Rectangle2D rec = new Rectangle2D.Float((float)box.getMinX(), (float)page.getBBox().getHeight() - (float)box.getMaxY(), (float)box.getWidth(), (float)box.getHeight());
                    p.addTag(new Tag(TagsName.PAGE_ID, rec));
                }
                //else {
                  //  p.addTag(new Tag(TagsName.UNKNOWN, box));
                //}
            }
        }
        return result;
    }

    private Rectangle2D showContent(int mcid, Map<Integer, PDMarkedContent> theseMarkedContents) throws IOException {
        Rectangle2D box = null;
        PDMarkedContent markedContent = theseMarkedContents != null ? theseMarkedContents.get(mcid) : null;
        List<Object> contents = markedContent != null ? markedContent.getContents() : Collections.emptyList();
        StringBuilder textContent =  new StringBuilder();
        for (Object object : contents) {
            if (object instanceof TextPosition) {
                TextPosition textPosition = (TextPosition)object;
                textContent.append(textPosition.getUnicode());

                int[] codes = textPosition.getCharacterCodes();
                if (codes.length != 1) {
                    //System.out.printf("<!-- text position with unexpected number of codes: %d -->", codes.length);
                } else {
                    box = union(box, calculateGlyphBounds(textPosition.getTextMatrix(), textPosition.getFont(), codes[0]).getBounds2D());
                }
            } else if (object instanceof PDMarkedContent) {
                PDMarkedContent thisMarkedContent = (PDMarkedContent) object;
                box = union(box, showContent(thisMarkedContent.getMCID(), theseMarkedContents));
            } else {
                textContent.append("?" + object);
            }
        }
        //System.out.printf("%s\n", textContent);
        return box;
    }

    @SafeVarargs
    final Map<PDPage, Rectangle2D> union(Map<PDPage, Rectangle2D>... maps) {
        Map<PDPage, Rectangle2D> result = null;
        for (Map<PDPage, Rectangle2D> map : maps) {
            if (map != null) {
                if (result != null) {
                    for (Map.Entry<PDPage, Rectangle2D> entry : map.entrySet()) {
                        PDPage page = entry.getKey();
                        Rectangle2D rectangle = union(result.get(page), entry.getValue());
                        if (rectangle != null)
                            result.put(page, rectangle);
                    }
                } else {
                    result = map;
                }
            }
        }
        return result;
    }

    Map<PDPage, Rectangle2D> union(Map<PDPage, Rectangle2D> map, PDPage page, Rectangle2D rectangle) {
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(page, union(map.get(page), rectangle));
        return map;
    }

    Rectangle2D union(Rectangle2D... rectangles) {
        Rectangle2D box = null;
        for (Rectangle2D rectangle : rectangles) {
            if (rectangle != null) {
                if (box != null)
                    box.add(rectangle);
                else
                    box = rectangle;
            }
        }
        return box;
    }

    private Shape calculateGlyphBounds(Matrix textRenderingMatrix, PDFont font, int code) throws IOException {
        GeneralPath path = null;
        AffineTransform at = textRenderingMatrix.createAffineTransform();
        at.concatenate(font.getFontMatrix().createAffineTransform());
        if (font instanceof PDType3Font) {

            PDType3Font t3Font = (PDType3Font) font;
            PDType3CharProc charProc = t3Font.getCharProc(code);
            if (charProc != null) {
                BoundingBox fontBBox = t3Font.getBoundingBox();
                PDRectangle glyphBBox = charProc.getGlyphBBox();
                if (glyphBBox != null) {
                    glyphBBox.setLowerLeftX(Math.max(fontBBox.getLowerLeftX(), glyphBBox.getLowerLeftX()));
                    glyphBBox.setLowerLeftY(Math.max(fontBBox.getLowerLeftY(), glyphBBox.getLowerLeftY()));
                    glyphBBox.setUpperRightX(Math.min(fontBBox.getUpperRightX(), glyphBBox.getUpperRightX()));
                    glyphBBox.setUpperRightY(Math.min(fontBBox.getUpperRightY(), glyphBBox.getUpperRightY()));
                    path = glyphBBox.toGeneralPath();
                }
            }
        }
        else if (font instanceof PDVectorFont) {
            PDVectorFont vectorFont = (PDVectorFont) font;
            path = vectorFont.getPath(code);

            if (font instanceof PDTrueTypeFont) {
                PDTrueTypeFont ttFont = (PDTrueTypeFont) font;
                int unitsPerEm = ttFont.getTrueTypeFont().getHeader().getUnitsPerEm();
                at.scale(1000d / unitsPerEm, 1000d / unitsPerEm);
            }
            if (font instanceof PDType0Font) {
                PDType0Font t0font = (PDType0Font) font;
                if (t0font.getDescendantFont() instanceof PDCIDFontType2) {
                    int unitsPerEm = ((PDCIDFontType2) t0font.getDescendantFont()).getTrueTypeFont().getHeader().getUnitsPerEm();
                    at.scale(1000d / unitsPerEm, 1000d / unitsPerEm);
                }
            }
        }
        else if (font instanceof PDSimpleFont) {
            PDSimpleFont simpleFont = (PDSimpleFont) font;
            String name = simpleFont.getEncoding().getName(code);
            path = simpleFont.getPath(name);
        } else {
            //System.out.println("Unknown font class: " + font.getClass());
        }
        if (path == null) {
            return null;
        }

        return at.createTransformedShape(path.getBounds2D());
    }

    public void processImagesFromPDF(PDDocument document) throws IOException {
        List<RenderedImage> images = new ArrayList<>();
        for (PDPage page : document.getPages()) {
            images = getImagesFromResources(page.getResources());
            Page p = taggedPages.get(page);
            for (RenderedImage image: images) {
                p.addTag(new Tag(TagsName.FIGURE, image.getData().getBounds()));
            }
        }
    }

    private List<RenderedImage> getImagesFromResources(PDResources resources) throws IOException {
        List<RenderedImage> images = new ArrayList<>();

        for (COSName xObjectName : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(xObjectName);

            if (xObject instanceof PDFormXObject) {
                images.addAll(getImagesFromResources(((PDFormXObject) xObject).getResources()));
            } else if (xObject instanceof PDImageXObject) {
                images.add(((PDImageXObject) xObject).getImage());
            }
        }

        return images;
    }


}