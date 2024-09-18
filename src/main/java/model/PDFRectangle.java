package model;

import org.apache.pdfbox.text.TextPosition;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;

public class PDFRectangle extends Rectangle2D.Double {


    public static final Comparator<PDFRectangle> RECTANGLE_COMPARATOR =
            Comparator.comparing(PDFRectangle::getTop).reversed()
                    .thenComparing(PDFRectangle::getLeft);


    public PDFRectangle(double left, double top, double right, double bottom) {
        super(left, top, right - left , bottom - top);
    }

    public PDFRectangle(TextPosition tp) {
        super(tp.getXDirAdj(), tp.getYDirAdj() - tp.getHeightDir(), tp.getWidthDirAdj() , tp.getHeightDir());
    }

    public PDFRectangle(Point2D.Float topLeft, Point2D.Float bottomRight) {
        setLeft((float) topLeft.getX());
        setTop((float) topLeft.getY());
        setRight((float) bottomRight.getX());
        setBottom((float) bottomRight.getY());
    }

    public double getLeft() {
        return super.getMinX();
    }

    public double getTop() {
        return super.getMinY();
    }

    public double getRight() {
        return getLeft() + super.getWidth();
    }

    public double getBottom() {
        return getTop() + super.getHeight();
    }

    public double getWidth() {
        return super.getWidth();
    }

    public double getHeight() {
        return super.getHeight();
    }

    public void setLeft(double left) {
        super.x = left;
    }

    public void setBottom(double bottom) {
        super.setRect(super.x, super.y, getWidth(), bottom - super.y);
    }

    public void setRight(double right) {
        super.setRect(super.x, super.y, right - super.x, getHeight());
    }

    public void setTop(double top) {
        super.y = top;
    }

    public void setPDFRectangle(PDFRectangle pdfRectangle){
        super.setRect(pdfRectangle.x, pdfRectangle.y, pdfRectangle.getWidth(), pdfRectangle.getHeight());
    }

    public Point2D[] getPoints() {
        return new Point2D[] {
                new Point2D.Double(this.getLeft(), this.getTop()),
                new Point2D.Double(this.getRight(), this.getTop()),
                new Point2D.Double(this.getRight(), this.getBottom()),
                new Point2D.Double(this.getLeft(), this.getBottom())
        };
    }
}

