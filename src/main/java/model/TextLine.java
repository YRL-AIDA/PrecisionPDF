package model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextLine extends TextChunk{
    public TextLine(double left, double top, double right, double bottom, String text, Page page) {
        super(left, top, right, bottom, text, page);
    }

    public TextLine(PDFRectangle boundingBox, String text, Page page) {
        super(boundingBox, text, page);
    }

    public TextLine(Point2D.Float leftTopPoint, Point2D.Float rightBottomPoint, String text, Page page) {
        super(leftTopPoint, rightBottomPoint, text, page);
    }

    public void addWors(List words){
        this.getWords().addAll(words);
        Collections.sort(this.getWords(), (c1, c2) -> {return  (int) (c1.getBbox().getLeft() - c2.getBbox().getLeft());});
    }

    public void retract(){
        getPage().getTextLines().remove(this);
    }
}
