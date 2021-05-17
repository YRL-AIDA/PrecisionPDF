package writers;

import model.TextChunk;
import model.table.Cell;
import model.table.Row;
import model.table.Table;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HtmlTableWriter {

    public List<String> write(List<Table> tables)
            throws IOException, TransformerException, ParserConfigurationException {
        List<String> result = new ArrayList<>();
        for (Table table : tables) {
            result.add(write(table));
        }
        return result;
    }

    public String write(Table table) throws ParserConfigurationException, TransformerException, IOException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element root = doc.createElement("table");
        root.setAttribute("border", "1px solid black");
        doc.appendChild(root);

        handleTable(table, doc, root);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        transformer.transform(source, new StreamResult(writer));

        return writer.getBuffer().toString();
    }

    private void handleTable(Table table, Document doc, Element root) {
        for (int rowNumber = 0; rowNumber < table.getNumOfRows(); rowNumber++) {
            Row row = table.getRow(rowNumber);
            if (!row.getCells().isEmpty()) {
                Element tr = doc.createElement("tr");
                for (Cell cell : row.getCells()) {
                    tr.appendChild(createCellElement(doc, cell));
                }
                root.appendChild(tr);
            }
        }
    }

    private Element createCellElement(Document doc, Cell cell) {
        Element td = doc.createElement("td");
        handleSpan(cell, td);
        handleTextWithIDs(doc, cell, td);
        return td;
    }

    private void handleSpan(Cell cell, Element td) {
        int rowSpan = cell.getRb() - cell.getRt();
        if (rowSpan > 0) {
            td.setAttribute("rowspan", String.valueOf(rowSpan + 1));
        }
        int colSpan = cell.getCr() - cell.getCl();
        if (colSpan > 0) {
            td.setAttribute("colspan", String.valueOf(colSpan + 1));
        }
    }

    private void handleText(Document doc, Cell cell, Element td) {
        String[] lines = cell.getText().split("\n");
        if (lines.length > 0) {
            td.appendChild(doc.createTextNode(lines[0]));
            if (lines.length > 1) {
                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i];
                    if (!(line.matches("\\s*") || line.isEmpty())) {
                        Element br = doc.createElement("br");
                        td.appendChild(br);
                        td.appendChild(doc.createTextNode(line));
                    }
                }
            }
        }
    }

    private void handleTextWithIDs(Document doc, Cell cell, Element td) {

        Iterator<TextChunk> blocks = cell.getBlocks();
        if (null == blocks) return;

        while (blocks.hasNext()) {
            TextChunk block = blocks.next();
            Element div = doc.createElement("div");
            div.setAttribute("id", block.getCode());
            String[] textLines = block.getText().split(System.lineSeparator());
            if (textLines.length > 0) {
                div.appendChild(doc.createTextNode(textLines[0]));
                if (textLines.length > 1) {
                    for (int i = 1; i < textLines.length; i++) {
                        String line = textLines[i];
                        if (!(line.matches("\\s*") || line.isEmpty())) {
                            Element br = doc.createElement("br");
                            div.appendChild(br);
                            div.appendChild(doc.createTextNode(line));
                        }
                    }
                }
            }

            td.appendChild(div);
        }
    }

}