package writers;

import model.Document;
import model.PDFImage;
import model.Page;
import model.TextChunk;
import model.table.Cell;
import model.table.Row;
import model.table.Table;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;


public class JsonWordDocumentWriter extends JsonDocumentWriter{
    public JsonWordDocumentWriter(Document document) {
        super(document);
    }

    public JsonWordDocumentWriter(Document document, int startPage, int endPage) {
        super(document, startPage, endPage);
    }

    public JSONObject writePage(Page page) {
        //page.sortLines();
        JSONObject jsonPage = new JSONObject();
        JSONArray jsonBlocks = new JSONArray();
        JSONArray jsonWords = new JSONArray();
        JSONArray jsonTables = new JSONArray();
        JSONArray jsonImages = new JSONArray();

        jsonPage.put("number", page.getIndex());
        jsonPage.put("width", page.getWidth());
        jsonPage.put("height", page.getHeight());

        List<TextChunk> outsideWords = page.getOutsideWords();
        for (TextChunk word: outsideWords) {
            JSONObject jsonWord = new JSONObject();
            jsonWord.put("x_top_left", (int)word.getLeft());
            jsonWord.put("y_top_left", (int)word.getTop());
            jsonWord.put("width", (int)word.getWidth());
            int height = (int)word.getHeight() < 0 ? 0: (int)word.getHeight();
            jsonWord.put("height", height);
            jsonWord.put("text", word.getText());
            jsonWord.put("italic", word.getFont().isItalic());
            jsonWord.put("normal", word.getFont().isNormal());
            jsonWord.put("bold", word.getFont().isBold());
            jsonWord.put("font_type", word.getFont().getName());
            jsonWord.put("font_size", word.getFont().getFontSize());
            jsonWords.put(jsonWord);

        }
        jsonPage.put("words",jsonWords);

        for (Table table: page.getTables()) {
            JSONObject jsonTable = new JSONObject();
            jsonTable.put("x_top_left", (int)table.getLeft());
            jsonTable.put("y_top_left", (int)table.getTop());
            jsonTable.put("width", (int)table.getWidth());
            jsonTable.put("height", (int)table.getHeight());
            jsonTable.put("order", 10000 * (page.getIndex()+1) + table.getOrder());
            JSONArray cellProperties = new JSONArray();
            JSONArray jsonRows = new JSONArray();
            for (int i = 0; i < table.getNumOfRows(); i++) {
                JSONArray jsonRow = new JSONArray();
                JSONArray jsonPropertiesRow = new JSONArray();
                Row row = table.getRow(i);
                for (Cell cell: row.getCells()){
                    JSONObject cellText = new JSONObject();
                    cellText.put("text", cell.getText());
                    JSONArray cellBlocks = new JSONArray();
                    int start = 0;
                    for(TextChunk tb: page.getTextLines()) {
                        for (TextChunk.TextLine tl: tb.getWords()){
                            if (cell.intersects(tl.getBbox())){
                                JSONObject cellBlock = new JSONObject();
                                cellBlock.put("x_top_left", (int)tl.getBbox().getLeft());
                                cellBlock.put("y_top_left", (int)tl.getBbox().getTop());
                                cellBlock.put("width", (int)tl.getBbox().getWidth());
                                cellBlock.put("height", (int)tl.getBbox().getHeight());
                                cellBlock.put("start", start);
                                int len = tb.getText().length();
                                cellBlock.put("end", start + len);
                                start = start + len + 1;
                                cellBlocks.put(cellBlock);

                            }
                        }
                    }

                    cellText.put("cell_blocks", cellBlocks);
                    jsonRow.put(cellText);
                    JSONObject jsonProp = new JSONObject();
                    int rowSpan = cell.getRb() - cell.getRt() + 1;
                    jsonProp.put("row_span", rowSpan);
                    int colSpan = cell.getCr() - cell.getCl() + 1;
                    jsonProp.put("col_span", colSpan);
                    jsonProp.put("invisible", cell.getInvisiable());
                    jsonPropertiesRow.put(jsonProp);
                }
                if (!row.getCells().isEmpty()) {
                    jsonRows.put(jsonRow);
                    cellProperties.put(jsonPropertiesRow);
                }
            }
            jsonTable.put("rows", jsonRows);
            jsonTable.put("cell_properties", cellProperties);
            jsonTables.put(jsonTable);

        }

        jsonPage.put("tables",jsonTables);
        for (PDFImage image: page.getImages()) {
            JSONObject jsonImage = new JSONObject();
            jsonImage.put("original_name", image.getFileName());
            jsonImage.put("tmp_file_path", image.getPathOut());
            jsonImage.put("uuid", image.getUuid());
            jsonImage.put("x_top_left", image.getXPosition());
            jsonImage.put("y_top_left", image.getYPosition());
            jsonImage.put("width", image.getWidth());
            jsonImage.put("height", image.getHeight());
            jsonImage.put("page_num", image.getPageNumber());
            jsonImages.put(jsonImage);
        }
        jsonPage.put("images",jsonImages);
        return jsonPage;
    }
}
