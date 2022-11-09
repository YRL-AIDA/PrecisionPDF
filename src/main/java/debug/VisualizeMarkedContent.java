package debug;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkedContentReference;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDMarkedContent;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDType3CharProc;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.font.PDVectorFont;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFMarkedContentExtractor;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.pdfbox.util.Matrix;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class VisualizeMarkedContent {

    private PDDocument document;
    private Path debugDirectoryPath;
    private static final String SUFFIX_START_CHAR = "_";
    PDFont font;

    public VisualizeMarkedContent(PDDocument document, Path debugDirectoryPath) throws IOException {
        this.document = document;
        this.debugDirectoryPath = debugDirectoryPath;
    }

    public void visualize(String fileName) throws IOException, ParserConfigurationException, TransformerException {

        PDFRenderer pdfRenderer = new PDFRenderer(document);

        String outputDirectoryPath = debugDirectoryPath.toFile().getCanonicalPath();
        String outDirectoryPath = outputDirectoryPath.concat("/images");
        String outAtationsPath = outputDirectoryPath.concat("/annotations");
        String outXmlsPath = outputDirectoryPath.concat("/xmls");
        File outXmls = new File(outXmlsPath);
        File outAnnotations = new File(outAtationsPath);
        File outDirectory = new File(outDirectoryPath);

        outXmls.mkdirs();
        outAnnotations.mkdirs();
        outDirectory.mkdirs();
        
        Map<PDPage, Map<Integer, PDMarkedContent>> markedContents = new HashMap<>();
        Map<PDPage, Rectangle2D> boxes;
        boxes = new HashMap<PDPage, Rectangle2D>();

        for (PDPage page : document.getPages()) {
            PDFMarkedContentExtractor extractor = new PDFMarkedContentExtractor();
            extractor.processPage(page);

            Map<Integer, PDMarkedContent> theseMarkedContents = new HashMap<>();
            markedContents.put(page, theseMarkedContents);
            for (PDMarkedContent markedContent : extractor.getMarkedContents()) {
                addToMap(theseMarkedContents, markedContent);
            }
        }

        PDStructureNode root = document.getDocumentCatalog().getStructureTreeRoot();
        if (root == null)
            return;

        Map<PDPage, PDPageContentStream> visualizations = new HashMap<>();
        boxes = showStructure(document, root, markedContents, visualizations);

        for (PDPageContentStream canvas : visualizations.values()) {
            canvas.close();
        }

        String pdfFileBaseName = FilenameUtils.getBaseName(fileName);
        String outFilePath = getOutputFilePath("taggedpdf", pdfFileBaseName, "pdf");
        document.save(outFilePath);

        for (int i = 0; i < document.getNumberOfPages(); i ++) {
            BufferedImage image = pdfRenderer.renderImageWithDPI(i, 150, ImageType.RGB);
            String imageFileName = String.format("%s_%03d.%s", pdfFileBaseName, i, "jpeg");
            Path outputImagePath = Paths.get(outDirectory.toString(),imageFileName);
            ImageIOUtil.writeImage(image, outputImagePath.toString(), 150);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            String annotationFileName = String.format("%s_%03d.%s", pdfFileBaseName, i, "xml");
            Path outputAnnotationsPath = Paths.get(outAnnotations.toString(), annotationFileName);

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("annotation");
            doc.appendChild(rootElement);
            Element filename = doc.createElement("filename");
            filename.setTextContent(imageFileName);
            rootElement.appendChild(filename);
            Element size = doc.createElement("size");
            Element width = doc.createElement("width");
            width.setTextContent(Integer.toString(image.getWidth()));
            size.appendChild(width);
            Element height = doc.createElement("height");
            height.setTextContent(Integer.toString(image.getHeight()));
            size.appendChild(height);
            Element depth = doc.createElement("depth");
            depth.setTextContent("3");
            size.appendChild(depth);
            rootElement.appendChild(size);
            Element segmented = doc.createElement("segmented");
            segmented.setTextContent("0");
            rootElement.appendChild(segmented);

            PDPage currPage = document.getPage(i);
            for (Map.Entry<PDPage, Rectangle2D> entry : boxes.entrySet()) {
                PDPage page = entry.getKey();
                if (page.equals(currPage)) {
                    Element object = doc.createElement("object");
                    Rectangle2D box = entry.getValue();
                    Element name = doc.createElement("name");
                    name.setTextContent("tblock");
                    object.appendChild(name);
                    Element bndbox = doc.createElement("bndbox");
                    Element xmin = doc.createElement("xmin");
                    xmin.setTextContent(Integer.toString((int) box.getMinX()));
                    bndbox.appendChild(xmin);
                    Element уmin = doc.createElement("уmin");
                    уmin.setTextContent(Integer.toString((int) box.getMinY()));
                    bndbox.appendChild(уmin);
                    Element xmax = doc.createElement("xmax");
                    xmax.setTextContent(Integer.toString((int) box.getMaxX()));
                    bndbox.appendChild(xmax);
                    Element уmax = doc.createElement("уmax");
                    уmax.setTextContent(Integer.toString((int) box.getMaxY()));
                    bndbox.appendChild(уmax);
                    object.appendChild(bndbox);
                    rootElement.appendChild(object);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(outputAnnotationsPath.toFile());
            transformer.transform(source, result);
        }

        document.close();

    }

    void addToMap(Map<Integer, PDMarkedContent> theseMarkedContents, PDMarkedContent markedContent) {
        theseMarkedContents.put(markedContent.getMCID(), markedContent);
        for (Object object : markedContent.getContents()) {
            if (object instanceof PDMarkedContent) {
                addToMap(theseMarkedContents, (PDMarkedContent)object);
            }
        }
    }

    int index = 0;

    Map<PDPage, Rectangle2D> showStructure(PDDocument document,
                                           PDStructureNode node, Map<PDPage, Map<Integer,
            PDMarkedContent>> markedContents, Map<PDPage, PDPageContentStream> visualizations) throws IOException {
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
        System.out.printf("<%s index=%s>\n", structType, indexHere);
        for (Object object: node.getKids()) {
            if (object instanceof COSArray) {
                for (COSBase base : (COSArray) object) {
                    if (base instanceof COSDictionary) {
                        boxes = union(boxes, showStructure(document, PDStructureNode.create((COSDictionary) base), markedContents, visualizations));
                    } else if (base instanceof COSNumber) {
                        boxes = union(boxes, page, showContent(((COSNumber)base).intValue(), theseMarkedContents));
                    } else {
                        System.out.printf("?%s\n", base);
                    }
                }
            } else if (object instanceof PDStructureNode) {
                boxes = union(boxes, showStructure(document, (PDStructureNode) object, markedContents, visualizations));
            } else if (object instanceof Integer) {
                boxes = union(boxes, page, showContent((Integer)object, theseMarkedContents));
            } else if (object instanceof PDMarkedContentReference) {
                page = ((PDMarkedContentReference) object).getPage();
                theseMarkedContents = markedContents.get(page);
                boxes = union(boxes, page, showContent(((PDMarkedContentReference) object).getMCID(), theseMarkedContents));
            } else {
                System.out.printf("?%s\n", object);
            }

        }
        System.out.printf("</%s>\n", structType);
        if (boxes != null && structType != null) {
            Color color = new Color((int)(Math.random() * 256), (int)(Math.random() * 256), (int)(Math.random() * 256));

            for (Map.Entry<PDPage, Rectangle2D> entry : boxes.entrySet()) {
                page = entry.getKey();
                Rectangle2D box = entry.getValue();
                if (box == null)
                    continue;

                PDPageContentStream canvas = visualizations.get(page);
                if (canvas == null) {
                    canvas = new PDPageContentStream(document, page, AppendMode.APPEND, false, true);
                    visualizations.put(page, canvas);
                    canvas.setFont(PDType1Font.TIMES_ROMAN, 12);
                }
                //canvas.setFont(font, 12);
                canvas.saveGraphicsState();
                canvas.setStrokingColor(color);
                canvas.addRect((float)box.getMinX(), (float)box.getMinY(), (float)box.getWidth(), (float)box.getHeight());
                canvas.stroke();
                canvas.setNonStrokingColor(color);
                canvas.beginText();
                canvas.newLineAtOffset((float)((box.getMinX() + box.getMaxX())/2), (float)box.getMaxY());
                //canvas.showText(String.format("<%s index=%s>", "фывфывфыв", indexHere));
                canvas.endText();
                canvas.restoreGraphicsState();
                result.put(page, box);
            }
        }
        return result;
    }

    Rectangle2D showContent(int mcid, Map<Integer, PDMarkedContent> theseMarkedContents) throws IOException {
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
                    System.out.printf("<!-- text position with unexpected number of codes: %d -->", codes.length);
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
        System.out.printf("%s\n", textContent);
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
            System.out.println("Unknown font class: " + font.getClass());
        }
        if (path == null) {
            return null;
        }

        return at.createTransformedShape(path.getBounds2D());
    }

    private String getOutputFilePath(String innerDirectoryName, String fileName, String format) throws IOException {
        // Make the specified output directory
        if (null == innerDirectoryName) {
            innerDirectoryName = "";
        } else {
            innerDirectoryName = File.separator.concat(innerDirectoryName);
        }
        String outputDirectoryPath = debugDirectoryPath.toFile().getCanonicalPath();
        String outDirectoryPath = outputDirectoryPath.concat(innerDirectoryName);
        File outDirectory = new File(outDirectoryPath);

        outDirectory.mkdirs();

        File file = new File(document.getDocumentInformation().getCreator());
        return String.format("%s/%s.%s", outDirectoryPath, fileName, format);
    }

}