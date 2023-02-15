package model;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.geom.Rectangle2D;
import java.nio.file.Paths;
import java.util.UUID;

public class PDFImage {

    private Page page;
    private PDImageXObject image;
    private String uuid;
    private String fileName;
    private String pathOut;

    public PDFImage(PDImageXObject image, Page page, String path){
        this.image = image;
        this.page = page;
        UUID uuid = UUID.randomUUID();
        this.uuid = String.format("fig_%s", uuid);
        this.fileName = String.format("%s.png", this.uuid);
        this.pathOut = Paths.get(path, this.fileName).toString();
    }

    public String getUuid(){
        return this.uuid;
    }
    public String getFileName(){
        return this.fileName;
    }

    public String getPathOut(){
        return this.pathOut;
    }

    public Page getPage() {
        return this.page;
    }

    /*
    public Rectangle2D.Float getBBox(){
        this.image.get
    }
*/

}
