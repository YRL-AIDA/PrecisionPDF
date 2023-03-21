package model;

import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.text.PDFMarkedContentExtractor;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.common.filespecification.PDFileSpecification;
import pdreaders.PDContentExtractor;
import pdreaders.VisibleRulingExtractor;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.io.FileOutputStream;

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
                document.extractImages();
                //document.extractAttachments(path);
                document.parseTags();
                document.extractLines();
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
            // Move to extractLines method
            //visibleRulingExtractor.process(page);
            return page;
        }
        return null;
    }

    public void extractLines() throws IOException {
        for (Page page: pages) {
            visibleRulingExtractor.process(page);
        }
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
                    if (line.intersects(tag.getRect()) && !tag.getName().toString().equals("LINK")) {
                        line.setMetadata(tag.getName().toString());
                    }
                    for (TextChunk.TextLine word: line.getWords()) {
                        if (word.getBbox().intersects(tag.getRect()) && tag.getName().toString().equals("LINK")) {
                            word.setMetadata(tag.getName().toString());
                            word.setUrl(tag.getUrl());
                        }
                    }
                }
            }
        }
    }

    public void extractImages() throws IOException {
        for (PDPage page: pdDocument.getPages()) {
            PDResources pdResources = page.getResources();
            int i = 1;
            for (COSName name : pdResources.getXObjectNames()) {
                PDXObject o = pdResources.getXObject(name);
                if (o instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject)o;
                    i++;
                }
            }
        }
    }

    public void extractAttachments(Path path) throws IOException {
        String filePath = path.getParent() + System.getProperty("file.separator");
        PDDocumentNameDictionary namesDictionary =
            new PDDocumentNameDictionary(pdDocument.getDocumentCatalog());
        PDEmbeddedFilesNameTreeNode efTree = namesDictionary.getEmbeddedFiles();
        if (efTree != null) {
            extractFilesFromEFTree(efTree, filePath);
        }

        for (PDPage page : pdDocument.getPages()) {
                extractFilesFromPage(page, filePath);
        }
    }

    private static void extractFilesFromPage(PDPage page, String filePath) throws IOException
    {
        for (PDAnnotation annotation : page.getAnnotations())
        {
            if (annotation instanceof PDAnnotationFileAttachment)
            {
                PDAnnotationFileAttachment annotationFileAttachment = (PDAnnotationFileAttachment) annotation;
                PDFileSpecification fileSpec = annotationFileAttachment.getFile();
                if (fileSpec instanceof PDComplexFileSpecification)
                {
                    PDComplexFileSpecification complexFileSpec = (PDComplexFileSpecification) fileSpec;
                    PDEmbeddedFile embeddedFile = getEmbeddedFile(complexFileSpec);
                    if (embeddedFile != null)
                    {
                        extractFile(filePath, complexFileSpec.getFilename(), embeddedFile);
                    }
                }
            }
        }
    }

    private static void extractFilesFromEFTree(PDNameTreeNode<PDComplexFileSpecification> efTree, String filePath) throws IOException
    {
        Map<String, PDComplexFileSpecification> names = efTree.getNames();
        if (names != null)
        {
            extractFiles(names, filePath);
        }
        else
        {
            List<PDNameTreeNode<PDComplexFileSpecification>> kids = efTree.getKids();
            if (kids == null)
            {
                return;
            }
            for (PDNameTreeNode<PDComplexFileSpecification> node : kids)
            {
                extractFilesFromEFTree(node, filePath);
            }
        }
    }

    private static void extractFiles(Map<String, PDComplexFileSpecification> names, String filePath)
            throws IOException
    {
        for (Map.Entry<String, PDComplexFileSpecification> entry : names.entrySet())
        {
            PDComplexFileSpecification fileSpec = entry.getValue();
            PDEmbeddedFile embeddedFile = getEmbeddedFile(fileSpec);
            if (embeddedFile != null)
            {
                extractFile(filePath, fileSpec.getFilename(), embeddedFile);
            }
        }
    }

    private static void extractFile(String filePath, String filename, PDEmbeddedFile embeddedFile)
            throws IOException
    {
        String embeddedFilename = filePath + filename;
        File file = new File(embeddedFilename);
        File parentDir = file.getParentFile();
        if (!parentDir.exists())
        {
            System.out.println("Creating " + parentDir);
            parentDir.mkdirs();
        }
        System.out.println("Writing " + embeddedFilename);
        try (FileOutputStream fos = new FileOutputStream(file))
        {
            fos.write(embeddedFile.toByteArray());
        }
    }

    private static PDEmbeddedFile getEmbeddedFile(PDComplexFileSpecification fileSpec )
    {
        // search for the first available alternative of the embedded file
        PDEmbeddedFile embeddedFile = null;
        if (fileSpec != null)
        {
            embeddedFile = fileSpec.getEmbeddedFileUnicode();
            if (embeddedFile == null)
            {
                embeddedFile = fileSpec.getEmbeddedFileDos();
            }
            if (embeddedFile == null)
            {
                embeddedFile = fileSpec.getEmbeddedFileMac();
            }
            if (embeddedFile == null)
            {
                embeddedFile = fileSpec.getEmbeddedFileUnix();
            }
            if (embeddedFile == null)
            {
                embeddedFile = fileSpec.getEmbeddedFile();
            }
        }
        return embeddedFile;
    }
    public void parseTags() throws IOException {

        Map<PDPage, Map<Integer, PDMarkedContent>> markedContents = new HashMap<>();
        Map<PDPage, Rectangle2D> boxes;
        boxes = new HashMap<PDPage, Rectangle2D>();

        int pageNum = 0;

        for (PDPage page : pdDocument.getPages()) {
            pageNum++;
            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            List<PDAnnotation> annotations = page.getAnnotations();
            for( int j=0; j<annotations.size(); j++ ) {
                PDAnnotation annot = annotations.get(j);

                if (getActionURI(annot) != null) {
                    PDRectangle rect = annot.getRectangle();
                    float x = rect.getLowerLeftX();
                    float y = rect.getUpperRightY();
                    float width = rect.getWidth();
                    float height = rect.getHeight();
                    int rotation = page.getRotation();
                    if( rotation == 0 )
                    {
                        PDRectangle pageSize = page.getMediaBox();
                        y = pageSize.getHeight() - y;
                    } else {

                    }

                    Rectangle2D.Float awtRect = new Rectangle2D.Float(x,y,width,height);
                    stripper.addRegion( "" + j, awtRect );
                }
            }

            stripper.extractRegions( page );

            for( int j=0; j<annotations.size(); j++ ) {
                PDAnnotation annot = annotations.get(j);
                PDRectangle rect = annot.getRectangle();
                float x = rect.getLowerLeftX();
                float y = rect.getUpperRightY();
                float width = rect.getWidth();
                float height = rect.getHeight();
                PDRectangle pageSize = page.getMediaBox();
                y = pageSize.getHeight() - y;
                Page p = taggedPages.get(page);
                Rectangle2D.Float awtRect = new Rectangle2D.Float(x,y,width,height);

                PDActionURI uri = getActionURI(annot);
                if (uri != null) {
                    String urlText = stripper.getTextForRegion("" + j);
                    p.addTag(new Tag(TagsName.LINK, awtRect, uri.getURI()));
                }
            }

            PDFMarkedContentExtractor extractor = new PDFMarkedContentExtractor();
            extractor.processPage(page);

            Map<Integer, PDMarkedContent> theseMarkedContents = new HashMap<>();
            markedContents.put(page, theseMarkedContents);
            for (PDMarkedContent markedContent : extractor.getMarkedContents()) {
                addToMap(theseMarkedContents, markedContent);
            }
        }

        PDStructureNode root = pdDocument.getDocumentCatalog().getStructureTreeRoot();

        if (root == null)
            return;

        boxes = showStructure(this.pdDocument, root, markedContents);

        for (int i = 0; i < pdDocument.getNumberOfPages(); i ++) {
            PDPage currPage = pdDocument.getPage(i);
            Page page = taggedPages.get(currPage);
            if (page == null) continue;
        }

    }
    private static PDActionURI getActionURI(PDAnnotation annot) {
        try {
            Method actionMethod = annot.getClass().getDeclaredMethod("getAction");
            if (actionMethod.getReturnType().equals(PDAction.class)) {
                PDAction action = (PDAction) actionMethod.invoke(annot);
                if (action instanceof PDActionURI) {
                    return (PDActionURI) action;
                }
            }
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        }
        return null;
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
        for (Object object: node.getKids()) {
            if (object instanceof COSArray) {
                for (COSBase base : (COSArray) object) {
                    if (base instanceof COSDictionary) {
                        boxes = union(boxes, showStructure(document, PDStructureNode.create((COSDictionary) base), markedContents));
                    } else if (base instanceof COSNumber) {
                        boxes = union(boxes, page, this.showContent(((COSNumber)base).intValue(), theseMarkedContents));
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
            }
        }


        if (boxes != null && structType != null) {
            Color color = new Color((int)(Math.random() * 256), (int)(Math.random() * 256), (int)(Math.random() * 256));

            for (Map.Entry<PDPage, Rectangle2D> entry : boxes.entrySet()) {
                page = entry.getKey();
                Rectangle2D box = entry.getValue();

                if (box == null) {
                    continue;
                }

                result.put(page, box);

                Page p = taggedPages.get(page);

                if (p == null) {
                    continue;
                }

                if (structType.equals("Footnote")) {
                    Rectangle2D rec = new Rectangle2D.Float((float)box.getMinX(),
                            (float)page.getBBox().getHeight() - (float)box.getMaxY(),
                            (float)box.getWidth(), (float)box.getHeight());
                    p.addTag(new Tag(TagsName.FOOTNOTE, rec, ""));
                } else if (structType.equals("RunningTitle")) {
                    Rectangle2D rec = new Rectangle2D.Float((float)box.getMinX(),
                            (float)page.getBBox().getHeight() - (float)box.getMaxY(),
                            (float)box.getWidth(), (float)box.getHeight());
                    p.addTag(new Tag(TagsName.PAGE_ID, rec, ""));
                } /*else if (structType.equals("Link")) {
                    Rectangle2D rec = new Rectangle2D.Float((float) box.getMinX(),
                            (float) page.getBBox().getHeight() - (float) box.getMaxY(),
                            (float) box.getWidth(), (float) box.getHeight());
                    p.addTag(new Tag(TagsName.LINK, rec));
                }*/
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
                } else {
                    box = union(box, calculateGlyphBounds(textPosition.getTextMatrix(), textPosition.getFont(), codes[0]).getBounds2D());
                }
            } else if (object instanceof PDMarkedContent) {
                PDMarkedContent thisMarkedContent = (PDMarkedContent) object;
                box = union(box, showContent(thisMarkedContent.getMCID(), theseMarkedContents));
            } else {
            }
        }
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
                p.addTag(new Tag(TagsName.FIGURE, image.getData().getBounds(), ""));
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