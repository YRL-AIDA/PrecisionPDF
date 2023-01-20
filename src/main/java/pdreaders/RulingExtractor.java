package pdreaders;

import model.Ruling;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RulingExtractor extends PDFGraphicsStreamEngine {

    private final float pageHeight;

    private List<Ruling> rulings;
    private List<Ruling> tmpRulings;
    private Point2D.Float currentPoint;
    private Point2D.Float lastMoveToPoint;

    {
        rulings = new ArrayList<>();
        tmpRulings = new ArrayList<>();
        currentPoint = new Point2D.Float(0f, 0f);
        lastMoveToPoint = new Point2D.Float(0f, 0f);
    }

    public RulingExtractor(PDPage page) {
        super(page);
        pageHeight = getPage().getBBox().getHeight();
    }

    public List<Ruling> getRulings() {
        run();
        return rulings.isEmpty() ? null : rulings;
    }

    private void run() {
        try {
            processPage(getPage());
        }
        catch (IOException | IllegalArgumentException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
        float p0x = (float) p0.getX();
        float p0y = pageHeight - (float) p0.getY();
        float p1x = (float) p1.getX();
        float p1y = pageHeight - (float) p1.getY();
        float p2x = (float) p2.getX();
        float p2y = pageHeight - (float) p2.getY();
        float p3x = (float) p3.getX();
        float p3y = pageHeight - (float) p3.getY();
        tmpRulings.add(new Ruling(p0x, p0y, p1x, p1y));
        tmpRulings.add(new Ruling(p1x, p1y, p2x, p2y));
        tmpRulings.add(new Ruling(p2x, p2y, p3x, p3y));
        tmpRulings.add(new Ruling(p3x, p3y, p0x, p0y));
    }

    @Override
    public void drawImage(PDImage pdImage) throws IOException {
    }

    @Override
    public void clip(int windingRule) throws IOException {
    }

    @Override
    public void moveTo(float x, float y) throws IOException {
        currentPoint.setLocation(x, y);
        lastMoveToPoint.setLocation(x, y);
    }

    @Override
    public void lineTo(float x, float y) throws IOException {
        //getGraphicsState().getCurrentTransformationMatrix().transform(currentPoint);
        float x1 = currentPoint.x;
        float y1 = pageHeight - currentPoint.y;
        float x2 = x;
        float y2 = pageHeight - y;

        Ruling ruling = new Ruling(x1, y1, x2, y2);
        tmpRulings.add(ruling);
        currentPoint.setLocation(x, y);
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
    }

    @Override
    public Point2D getCurrentPoint() throws IOException {
        return currentPoint;
    }

    @Override
    public void closePath() throws IOException {
        float x1 = currentPoint.x;
        float y1 = pageHeight - currentPoint.y;
        float x2 = lastMoveToPoint.x;
        float y2 = pageHeight - lastMoveToPoint.y;

        Ruling ruling = new Ruling(x1, y1, x2, y2);
        tmpRulings.add(ruling);
        tmpRulings.clear();

        if (getGraphicsState().getNonStrokeAlphaConstant() == getGraphicsState().getAlphaConstant() ||
                getGraphicsState().getNonStrokingColor().equals(getGraphicsState().getStrokingColor()) ||
                getGraphicsState().getNonStrokingColorSpace().equals(getGraphicsState().getStrokingColorSpace()) ||
                getGraphicsState().getNonStrokingJavaComposite().equals(getGraphicsState().getStrokingJavaComposite())) {
            for (Ruling r: tmpRulings) {
                r.setRenderingType(Ruling.RenderingType.INVISIBLE);
            }
        }
    }

    @Override
    public void endPath() throws IOException {

        if (getGraphicsState().getNonStrokeAlphaConstant() == getGraphicsState().getAlphaConstant() ||
                getGraphicsState().getNonStrokingColor().equals(getGraphicsState().getStrokingColor()) ||
                        getGraphicsState().getNonStrokingColorSpace().equals(getGraphicsState().getStrokingColorSpace()) ||
                        getGraphicsState().getNonStrokingJavaComposite().equals(getGraphicsState().getStrokingJavaComposite())) {
            for (Ruling r: tmpRulings) {
                r.setRenderingType(Ruling.RenderingType.INVISIBLE);
            }
        }

        rulings.addAll(tmpRulings);
        tmpRulings.clear();
    }

    @Override
    public void strokePath() throws IOException {
        rulings.addAll(tmpRulings);
        tmpRulings.clear();
    }

    @Override
    public void fillPath(int windingRule) throws IOException {
        rulings.addAll(tmpRulings);
        tmpRulings.clear();
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException {
        rulings.addAll(tmpRulings);
        tmpRulings.clear();
    }

    @Override
    public void shadingFill(COSName shadingName) throws IOException {
        //System.out.println(shadingName.getName());
    }

}
