package model;

public class PDFFont {
    private String name;
    private float height;
    private boolean bold;
    private boolean italic;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getHeight() {
        return this.height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public boolean isNormal() {
        return !(this.bold || this.italic);
    }

    /**
     * @return the bold
     */
    public boolean isBold() {
        return this.bold;
    }

    /**
     * @param bold the bold to set
     */
    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return this.italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    @Override
    public String toString() {
        String sName = "\"" + this.name + "\"";

        String sHeight = String.format("H=%s", Float.toString(this.height));
        String sBold = String.format("B=%b", this.bold);
        String sItalic = String.format("I=%b", this.italic);

        return String.format("CFont [%s %s %s %s]", sName, sHeight, sBold, sItalic);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PDFFont PDFFont = (PDFFont) o;

        if (height != PDFFont.height) return false;
        if (bold != PDFFont.bold) return false;
        if (italic != PDFFont.italic) return false;

        return name.equals(PDFFont.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (int) height;
        result = 31 * result + (bold ? 1 : 0);
        result = 31 * result + (italic ? 1 : 0);

        return result;
    }
}
