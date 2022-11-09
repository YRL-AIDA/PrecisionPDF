package operators;

import org.apache.pdfbox.contentstream.operator.MissingOperandException;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.OperatorProcessor;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.documentinterchange.markedcontent.PDPropertyList;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroup;
import org.apache.pdfbox.text.PDFMarkedContentExtractor;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class BeginMarkedContent extends OperatorProcessor {

    public BeginMarkedContent() {
    }

    @Override
    public void process(Operator operator, List<COSBase> arguments) throws IOException {

        if (arguments.size() < 2) {
            throw new MissingOperandException(operator, arguments);
        } else {

            COSName tag = null;
            COSDictionary properties = null;
            Iterator var5 = arguments.iterator();

            while (var5.hasNext()) {
                COSBase argument = (COSBase) var5.next();
                if (argument instanceof COSName) {
                    tag = (COSName) argument;
                } else if (argument instanceof COSDictionary) {
                    properties = (COSDictionary) argument;
                  /*  for (COSBase value : properties.getValues()) {
                        if (value instanceof COSName) {
                            COSName name = (COSName) value;
                            PDXObject xobject = this.context.getResources().getXObject(name);
                            System.out.println(xobject);
                        }
                    }*/
                }
            }

            this.context.beginMarkedContentSequence(tag, properties);
        }
    }

    @Override
    public String getName() {
        return "BDC";
    }
}
