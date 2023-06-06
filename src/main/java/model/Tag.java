package model;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Tag {

    private TagsName name;
    private Rectangle2D rect;

    private String url;

    public Tag(TagsName name, Rectangle2D rect, String url) {
        this.rect = rect;
        this.name = name;
        this.url = url;
    }

    public Rectangle2D getRect(){
        return rect;
    }

    public TagsName getName() {
        return name;
    }

    public String getUrl() {
        return this.url;
    }
}
