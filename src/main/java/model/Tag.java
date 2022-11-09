package model;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Tag {

    private TagsName name;
    private Rectangle2D rect;

    public Tag(TagsName name, Rectangle2D rect) {
        this.rect = rect;
        this.name = name;
    }

    public Rectangle2D getRect(){
        return rect;
    }

    public TagsName getName() {
        return name;
    }
}
