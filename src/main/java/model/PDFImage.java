package model;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

public class PDFImage {

    private Page page;
    private PDImageXObject image;
    private String uuid;
    private String fileName;
    private String pathOut;
    private Rectangle2D bbox;

    private int pageNumber;

    public PDFImage(PDImageXObject image, Rectangle2D bbox, Page page, String path){
        this.image = image;
        this.page = page;
        UUID uuid = UUID.randomUUID();
        this.uuid = String.format("fig_%s", uuid);
        this.fileName = String.format("%s.png", this.uuid);
        this.pathOut = Paths.get(path, this.fileName).toString();
        this.bbox = bbox;
        this.pageNumber = page.getIndex();
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

    public PDImageXObject getImage() {
        return this.image;
    }

    public Rectangle2D getBbox(){
        return this.bbox;
    }

    public int getWidth(){
        return (int) this.bbox.getWidth();
    }

    public int getHeight(){
        return (int) this.bbox.getHeight();
    }

    public int getXPosition(){
        return (int) this.bbox.getX();
    }

    public int getYPosition(){
        return (int) this.bbox.getY();
    }

    public void save() throws IOException {
        File out = new File(getPathOut());
        String fileExt = FilenameUtils.getExtension(getPathOut());
        ImageIO.write(image.getImage(), fileExt, out);
    }

    public int getPageNumber() {
        return this.pageNumber;
    }

}
