package pdreaders;

import model.PDFImage;
import model.Page;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.PDFStreamEngine;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;

public class ImageExtractor extends PDFStreamEngine {

    private PDDocument document;
    private final List<PDFImage> images;
    private Page currentPage;
    private File sourceFile;

    public ImageExtractor(PDDocument document, File sourceFile) throws IOException {
        addOperator(new Concatenate());
        addOperator(new DrawObject());
        addOperator(new SetGraphicsStateParameters());
        addOperator(new Save());
        addOperator(new Restore());
        addOperator(new SetMatrix());
        this.document = document;
        this.images = new ArrayList<>(5);
        this.sourceFile = sourceFile;
    }

    private void release() {
        this.images.clear();
        this.currentPage = null;
    }

    public void process(Page page) throws IOException {
        if (null == page) {
            throw new IllegalArgumentException("Page cannot be null");
        } else {
            this.currentPage = page;
            processPage(page.getPDPage());
            try {
                page.addImages(this.images);
            } finally {
                release();
            }
        }
    }

    @Override
    protected void processOperator( Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.getName();
        if( "Do".equals(operation)) {
            COSName objectName = (COSName) operands.get( 0 );
            PDXObject xobject = getResources().getXObject( objectName );
            if( xobject instanceof PDImageXObject) {
                PDImageXObject image = (PDImageXObject)xobject;
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();
                Matrix ctmNew = getGraphicsState().getCurrentTransformationMatrix();
                Rectangle2D.Float bbox = new Rectangle2D.Float(ctmNew.getTranslateX(), ctmNew.getTranslateY(),
                        imageWidth, imageHeight);
                PDFImage pdfImage = new PDFImage(image, bbox, currentPage, this.sourceFile.getParent());
                this.images.add(pdfImage);
                pdfImage.save();
            }
            else if(xobject instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject)xobject;
                showForm(form);
            }
        } else {
            super.processOperator( operator, operands);
        }
    }
}