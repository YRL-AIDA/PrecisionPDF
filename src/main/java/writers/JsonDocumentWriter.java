package writers;

import model.Document;
import model.Page;
import model.TextChunk;
import model.table.Cell;
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

    public JsonDocumentWriter(Document document){
        this.document = document;
        this.json = new JSONObject();
        this.json.put("document", this.document.getSourceFile().getName());
    }

    public String write(){
        JSONArray jsonPages = new JSONArray();
        for (Iterator<Page> it = this.document.getPages(); it.hasNext(); ) {
            Page page = it.next();
            jsonPages.put(writePage(page));
        }
        json.put("pages", jsonPages);
        return json.toString();
    }

    private JSONObject writePage(Page page){
        JSONObject jsonPage = new JSONObject();
        JSONArray jsonBlocks = new JSONArray();
        JSONArray jsonTables = new JSONArray();
        jsonPage.put("number", page.getIndex());
        for (TextChunk block: page.getOutsideBlocks()) {
            JSONObject jsonBlock = new JSONObject();
            jsonBlock.put("order", block.getId());
            jsonBlock.put("xPos", block.getLeft());
            jsonBlock.put("yPos", block.getTop());
            jsonBlock.put("text", block.getText());
            jsonBlocks.put(jsonBlock);
        }
        jsonPage.put("blocks",jsonBlocks);
        for (Table table: page.getTables()) {
            JSONObject jsonTable = new JSONObject();
            JSONArray jsonRows = new JSONArray();
            JSONArray jsonRow = new JSONArray();
            for (int i = 0; i < table.getNumOfRows(); i++) {
                for (Cell cell: table.getRow(i).getCells()){
                    jsonRow.put(cell.getText());
                }
                jsonRows.put(jsonRow);
            }
            jsonTables.put(jsonRows);
        }
        jsonPage.put("tables",jsonTables);
        return jsonPage;
    }

}
