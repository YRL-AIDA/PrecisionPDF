package writers;

import model.Document;
import model.Page;
import model.TextChunk;
import model.table.Cell;
import model.table.Row;
import model.table.Table;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.xml.soap.Text;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JsonDocumentWriter {

    private Document document;
    JSONObject json = new JSONObject();
    int startPage = 0;
    int endPage = 0;
    boolean partiacalExtraction = false;

    public JsonDocumentWriter(Document document){
        this.document = document;
        this.json = new JSONObject();
        this.json.put("document", this.document.getSourceFile().getName());
        this.partiacalExtraction = false;
    }

    public JsonDocumentWriter(Document document, int startPage, int endPage){
        this.document = document;
        this.json = new JSONObject();
        this.json.put("document", this.document.getSourceFile().getName());
        this.startPage = startPage;
        this.endPage = endPage;
        this.partiacalExtraction = true;
    }

    public String write(){
        JSONArray jsonPages = new JSONArray();
        if (partiacalExtraction) {
            for (int i = startPage; i <= endPage; i++) {
                Page page = document.getPage(i);
                jsonPages.put(writePage(page));
            }
        } else {
            for (Iterator<Page> it = this.document.getPagesItrerator(); it.hasNext(); ) {
                Page page = it.next();
                jsonPages.put(writePage(page));
            }
        }
        json.put("pages", jsonPages);
        return json.toString();
    }

    private JSONObject writePage(Page page){
        JSONObject jsonPage = new JSONObject();
        JSONArray jsonBlocks = new JSONArray();
        JSONArray jsonTables = new JSONArray();
        jsonPage.put("number", page.getIndex());
        jsonPage.put("width", page.getWidth());
        jsonPage.put("height", page.getHeight());
        for (TextChunk block: page.getOutsideBlocks()) {
            JSONObject jsonBlock = new JSONObject();
            JSONArray jsonAnnotations = new JSONArray();
            jsonBlock.put("order", block.getId());
            jsonBlock.put("x_top_left", (int)block.getLeft());
            jsonBlock.put("y_top_left", (int)block.getTop());
            jsonBlock.put("width", (int)block.getWidth());
            jsonBlock.put("height", (int)block.getHeight());
            jsonBlock.put("text", block.getText());
            jsonBlock.put("is_bold", block.getFont().isBold());
            jsonBlock.put("is_italic", block.getFont().isItalic());
            jsonBlock.put("is_normal", block.getFont().isNormal());
            jsonBlock.put("font_name", block.getFont().getName());
            jsonBlock.put("font_size", (int)block.getFont().getFontSize());
            int start = 0;
            for (TextChunk.TextLine chunk: block.getTextLines()){
                JSONObject annotation = new JSONObject();
                annotation.put("text", chunk.getText());
                annotation.put("is_bold", chunk.getFont().isBold());
                annotation.put("is_italic", chunk.getFont().isItalic());
                annotation.put("is_normal", chunk.getFont().isNormal());
                annotation.put("font_name", chunk.getFont().getName());
                annotation.put("font_size", (int)block.getFont().getFontSize());
                annotation.put("x_top_left", (int)chunk.getBbox().getLeft());
                annotation.put("y_top_left", (int)chunk.getBbox().getTop());
                annotation.put("width", (int)chunk.getBbox().getWidth());
                annotation.put("height", (int)chunk.getBbox().getHeight());
                annotation.put("start", start);
                int len = chunk.getText().length();
                annotation.put("end", start + len);
                start = start + len + 1;
                jsonAnnotations.put(annotation);
            }
            jsonBlock.put("annotations", jsonAnnotations);
            jsonBlocks.put(jsonBlock);
        }
        jsonPage.put("blocks",jsonBlocks);
        for (Table table: page.getTables()) {
            JSONObject jsonTable = new JSONObject();
            jsonTable.put("x_top_left", (int)table.getLeft());
            jsonTable.put("y_top_left", (int)table.getTop());
            jsonTable.put("width", (int)table.getWidth());
            jsonTable.put("height", (int)table.getHeight());
            JSONArray jsonRows = new JSONArray();
            for (int i = 0; i < table.getNumOfRows(); i++) {
                JSONArray jsonRow = new JSONArray();
                Row row = table.getRow(i);
                for (Cell cell: row.getCells()){
                    jsonRow.put(cell.getText());
                }
                if (!row.getCells().isEmpty()) {
                    jsonRows.put(jsonRow);;
                }
            }
            jsonTable.put("rows", jsonRows);
            jsonTables.put(jsonTable);
        }
        jsonPage.put("tables",jsonTables);
        return jsonPage;
    }

}
