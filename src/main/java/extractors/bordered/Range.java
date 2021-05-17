package extractors.bordered;

import java.util.ArrayList;

public class Range {

    private final double ERROR = 1f;
    private double start;
    private double end;

    public Range(double start, double end){
        this.start = start;
        this.end = end;
    }

    public void setEnd(double end){
        this.end = end;
    }

    public double getEnd(){
        return this.end;
    }

    public void setStart(double start){
        this.start = start;
    }

    public double getStart(){
        return this.start;
    }

    public double length(){
        return getEnd() - getStart();
    }

    public boolean isIntersectionRange(Range range){
        double dist = Math.min(this.end, range.end) - Math.max(this.start, range.start);
        if (dist > ERROR) return true;
        return false;
    }

    public int inRange(ArrayList<Range> allRanges){
        int ind = 0;
        for (Range currRange: allRanges){
            if(currRange.getStart() < this.getStart() && currRange.getEnd() > this.getEnd()){
                return ind;
            }
            ind++;
        }
        return 0;
    }

}
