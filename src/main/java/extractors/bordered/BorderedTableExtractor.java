package extractors.bordered;

import extractors.AbstractTableExtractor;
import extractors.bordered.filters.BorderedTableSeparator;
import model.PDFRectangle;
import model.Page;
import model.Ruling;
import model.TextChunk;
import model.table.Cell;
import model.table.Table;
import model.table.TableType;
import utils.Utils;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public final class BorderedTableExtractor extends AbstractTableExtractor {

    private static final Comparator<Point2D> POINT_COMPARATOR = new Comparator<Point2D>() {
        @Override
        public int compare(Point2D arg0, Point2D arg1) {
            int result = 0;
            float arg0X = Utils.round(arg0.getX(), 2);
            float arg0Y = Utils.round(arg0.getY(), 2);
            float arg1X = Utils.round(arg1.getX(), 2);
            float arg1Y = Utils.round(arg1.getY(), 2);

            if (arg0Y > arg1Y) {
                result = 1;
            } else if (arg0Y < arg1Y) {
                result = -1;
            } else if (arg0X > arg1X) {
                result = 1;
            } else if (arg0X < arg1X) {
                result = -1;
            }
            return result;
        }
    };

    private static final Comparator<Point2D> X_FIRST_POINT_COMPARATOR = new Comparator<Point2D>() {
        @Override
        public int compare(Point2D arg0, Point2D arg1) {
            int result = 0;
            float arg0X = Utils.round(arg0.getX(), 2);
            float arg0Y = Utils.round(arg0.getY(), 2);
            float arg1X = Utils.round(arg1.getX(), 2);
            float arg1Y = Utils.round(arg1.getY(), 2);

            if (arg0X > arg1X) {
                result = 1;
            } else if (arg0X < arg1X) {
                result = -1;
            } else if (arg0Y > arg1Y) {
                result = 1;
            } else if (arg0Y < arg1Y) {
                result = -1;
            }
            return result;
        }
    };

    private ArrayList<Range> horizontal = new ArrayList<>();
    private ArrayList<Range> vertical = new ArrayList<>();

    private final List<Ruling> verticalRulings;
    private final List<Ruling> horizontalRulings;
    private Map<Point2D, Ruling[]> intersectionPoints;

    public BorderedTableExtractor(Page page) {
        super(page);
        verticalRulings = new ArrayList<>();
        horizontalRulings = new ArrayList<>();
        intersectionPoints = new TreeMap<>();
    }


    @Override
    public List<Table> extract() {

        List<Table> result = null;

        Page page = getPage();

        if (extractTables(page)) {

            result = new ArrayList<>();
            Iterator<Table> tableIterator = page.getTables().iterator();

            while (tableIterator.hasNext()) {
                Table table = tableIterator.next();
                if (table != null) {
                    table.setPageIndex(page.getIndex());
                    result.add(table);
                }
            }
        }

        return result.isEmpty() ? null : result;
    }

    private boolean extractTables(Page page) {
        classifyRulings();
        ArrayList<Ruling> joinedHorizontalRulings =
                (ArrayList<Ruling>) joinRulings(horizontalRulings, Ruling.DISTANCE_TOLERANCE);
        ArrayList<Ruling> joinedVerticalRulings =
                (ArrayList<Ruling>) joinRulings(verticalRulings, Ruling.DISTANCE_TOLERANCE);
        page.addJoinedRulings(joinedHorizontalRulings);
        page.addJoinedRulings(joinedVerticalRulings);
        findIntersections(joinedHorizontalRulings, joinedVerticalRulings);
        ArrayList<PDFRectangle> cells = (ArrayList<PDFRectangle>) findCells();

        for (PDFRectangle cell: cells) {
            if (cell.getWidth() > Cell.MIN_CELL_WIDTH && cell.getHeight() > Cell.MIN_CELL_HEIGHT) {
                page.addCell(cell);
            }
        }

        List<PDFRectangle> tableAreas = (ArrayList<PDFRectangle>) findTableAreas(cells);

        for (PDFRectangle area: tableAreas) {

            List<PDFRectangle> tableCells = new ArrayList<>();

            Range horizontal_range;
            Range vertical_range;

            for (PDFRectangle c: cells) {
                if (c.intersects(area)) {
                    if (c.getWidth() > Cell.MIN_CELL_WIDTH && c.getHeight() > Cell.MIN_CELL_HEIGHT) {
                        tableCells.add(c);
                    } else {
                        continue;
                    }
                }
            }

            List<PDFRectangle> filteredTableCells = new ArrayList<>();

            for (PDFRectangle c: tableCells) {
                boolean isIntersected = false;
                for (PDFRectangle c1: filteredTableCells) {
                    if (c1.intersects(c)) {
                        c1.add(c);
                        isIntersected = true;
                    }
                }
                if (!isIntersected)
                    filteredTableCells.add(c);
            }

            for (PDFRectangle c: filteredTableCells) {
                horizontal_range = new Range(c.getLeft(), c.getRight());
                join(horizontal, horizontal_range);
                vertical_range = new Range(c.getTop(), c.getBottom());
                join(vertical, vertical_range);
            }

            if (filteredTableCells.size() < Factors.MIN_CELLS_COUNT_FACTOR || horizontal.size() < 2) {
                page.addPossibleTableArea(area);
                continue;
            }

            for (PDFRectangle cell: filteredTableCells) {
                    page.addCell(cell);
            }

            Table table = new Table(area.getLeft(), area.getTop(), area.getRight(), area.getBottom(), TableType.UNKNOWN);

            Collections.sort(horizontal, new RangeComporator());
            Collections.sort(vertical, new RangeComporator());
            table.setHorizontal(horizontal);
            table.setVertical(vertical);

            ArrayList<TextChunk> chunks = new ArrayList<>();
            Iterator<TextChunk> blocksIterator = page.getBlocks();

            while (blocksIterator.hasNext()) {
                TextChunk textChunk = blocksIterator.next();
                chunks.add(textChunk);
            }

            for (PDFRectangle c: filteredTableCells) {

                List<TextChunk> cellBlocks = chunks.stream()
                        .filter(tb -> c.intersects(tb))
                        .collect(Collectors.toList());

/*                if (null != cellBlocks && cellBlocks.size() > 1) {
                    // Merge all blocks located inside the cell into the first one
                    TextChunk mergedCellBlock = mergeCellBlocks(cellBlocks);
                    // Remove merged blocks except the first one from the page
                    for (int k = 1; k < cellBlocks.size(); k++) {
                        TextChunk block = cellBlocks.get(k);
                        block.retract();
                    }
                    // Now, only one merged block go to the cell content
                    cellBlocks.clear();
                    cellBlocks.add(mergedCellBlock);
                }*/

                int startColumn = getStartColumn(c, horizontal);
                int endColumn = getEndColumn(c, horizontal);
                int startRow = getStartRow(c, vertical);
                int endRow = getEndRow(c, vertical);

                PDFRectangle bbox = new PDFRectangle(c.getLeft(), c.getBottom(), c.getRight(), c.getTop());
                Cell cell = new Cell(bbox, 0, cellBlocks, startColumn, startRow, endColumn, endRow);

                table.addCell(cell, startRow);

            }
            BorderedTableSeparator borderedTableSeparator = new BorderedTableSeparator();
            if (borderedTableSeparator.isFullBorderedTable(table)) {
                table.setType(TableType.FULL_BORDERED);
            } else {
                table.setType(TableType.PARTIAL_BORDERED);
            }
            page.addTable(table);
            horizontal.clear();
            vertical.clear();
        }

        return true;
    }

    private int getStartColumn(PDFRectangle textChunk, ArrayList<Range> horizontal) {
        Range chunkRange = new Range(textChunk.getLeft(), textChunk.getRight());
        int minColumnIndex = Integer.MAX_VALUE;
        for (int i = 0; i < horizontal.size(); i++) {
            if (chunkRange.isIntersectionRange(horizontal.get(i))) {
                if (i < minColumnIndex) {
                    minColumnIndex = i;
                }
            }
        }
        return minColumnIndex;
    }

    private int getEndColumn(PDFRectangle textChunk, ArrayList<Range> horizontal) {
        Range chunkRange = new Range(textChunk.getLeft(), textChunk.getRight());
        int maxColumnIndex = Integer.MIN_VALUE;
        for (int i = 0; i < horizontal.size(); i++) {
            if (chunkRange.isIntersectionRange(horizontal.get(i))) {
                if (i > maxColumnIndex) {
                    maxColumnIndex = i;
                }
            }
        }
        return maxColumnIndex;
    }

    private int getStartRow(PDFRectangle textChunk, ArrayList<Range> vertical) {
        Range chunkRange = new Range(textChunk.getTop(), textChunk.getBottom());
        int minRowIndex = Integer.MAX_VALUE;;
        for (int i = 0; i < vertical.size(); i++) {
            if (chunkRange.isIntersectionRange(vertical.get(i))) {
                if (i < minRowIndex) {
                    minRowIndex = i;
                }
            }
        }
        return minRowIndex;
    }

    private int getEndRow(PDFRectangle textChunk, ArrayList<Range> vertical) {
        Range chunkRange = new Range(textChunk.getTop(), textChunk.getBottom());
        int maxRowIndex = Integer.MIN_VALUE;
        for (int i = 0; i < vertical.size(); i++) {
            if (chunkRange.isIntersectionRange(vertical.get(i))) {
                if (i > maxRowIndex) {
                    maxRowIndex = i;
                }
            }
        }
        return maxRowIndex;
    }

    private boolean join(ArrayList<Range> allRanges, Range range){
        for (Range currRange: allRanges){
            if (currRange.isIntersectionRange(range)){
                currRange.setEnd(Math.min(range.getEnd(), currRange.getEnd()));
                currRange.setStart(Math.max(range.getStart(), currRange.getStart()));
                return true;
            }
        }
        allRanges.add(range);
        return false;
    }


    private void findIntersections(List<Ruling> horizontalRulings, List<Ruling> verticalRulings) {
        intersectionPoints = Ruling.findIntersections(horizontalRulings, verticalRulings);
    }

    private void classifyRulings() {

        Page page = getPage();
        Iterator<Ruling> rulingIterator = page.getBorderedTableRulings();

        while (rulingIterator.hasNext()) {

            Ruling ruling = rulingIterator.next();
            if (ruling.getRenderingType() == Ruling.RenderingType.INVISIBLE) {
                continue;
            }
            if (ruling.getLength() < 1) {
                continue;
            }
            if (ruling.isVertical()) {
                this.verticalRulings.add(ruling);
            } else if (ruling.isHorizontal()) {
                this.horizontalRulings.add(ruling);
            }
        }
    }

    private List<Ruling> joinRulings(List<Ruling> rulings, int tolerance) {
        List<Ruling> joinedRulings = new ArrayList<Ruling>();
        Collections.sort(rulings, new Comparator<Ruling>() {
            @Override
            public int compare(Ruling a, Ruling b) {
                final float diff = a.getPosition() - b.getPosition();
                return java.lang.Float.compare(diff == 0 ? a.getStart() - b.getStart() : diff, 0f);
            }
        });

        for (Ruling nextLine : rulings) {
            Ruling last = joinedRulings.isEmpty() ? null : joinedRulings.get(joinedRulings.size() - 1);
            if (last != null && Utils.feq(nextLine.getPosition(),
                    last.getPosition()) && last.nearlyIntersects(nextLine, tolerance)) {
                final float lastStart = last.getStart();
                final float lastEnd = last.getEnd();

                final boolean lastFlipped = lastStart > lastEnd;
                final boolean nextFlipped = nextLine.getStart() > nextLine.getEnd();

                boolean differentDirections = nextFlipped != lastFlipped;
                float nextS = differentDirections ? nextLine.getEnd() : nextLine.getStart();
                float nextE = differentDirections ? nextLine.getStart() : nextLine.getEnd();

                final float newStart = lastFlipped ? Math.max(nextS, lastStart) : Math.min(nextS, lastStart);
                final float newEnd = lastFlipped ? Math.min(nextE, lastEnd) : Math.max(nextE, lastEnd);
                last.setStartEnd(newStart, newEnd);
            }
            else if (nextLine.length() == 0) {
                continue;
            }
            else {
                joinedRulings.add(nextLine);
            }
        }
        return joinedRulings;
    }

    private List<PDFRectangle> findCells() {
        List<PDFRectangle> cellsFound = new ArrayList<>();
        List<Point2D> intersectionPointsList = new ArrayList<>(intersectionPoints.keySet());
        Collections.sort(intersectionPointsList, POINT_COMPARATOR);
        boolean doBreak = false;

        for (int i = 0; i < intersectionPointsList.size(); i++) {
            Point2D.Float topLeft = (Point2D.Float)intersectionPointsList.get(i);
            Ruling[] hv = intersectionPoints.get(topLeft);
            doBreak = false;

            List<Point2D> xPoints = new ArrayList<>();
            List<Point2D> yPoints = new ArrayList<>();

            for (Point2D p : intersectionPointsList.subList(i, intersectionPointsList.size())) {
                if (p.getX() == topLeft.getX() && p.getY() > topLeft.getY()) {
                    xPoints.add(p);
                }
                if (p.getY() == topLeft.getY() && p.getX() > topLeft.getX()) {
                    yPoints.add(p);
                }
            }
            outer:
            for (Point2D xPoint : xPoints) {
                if (doBreak) {
                    break;
                }

                if (!hv[1].equals(intersectionPoints.get(xPoint)[1])) {
                    continue;
                }
                for (Point2D yPoint : yPoints) {
                    if (!hv[0].equals(intersectionPoints.get(yPoint)[0])) {
                        continue;
                    }
                    Point2D.Float btmRight = new Point2D.Float((float) yPoint.getX(), (float) xPoint.getY());
                    if (intersectionPoints.containsKey(btmRight)
                            && intersectionPoints.get(btmRight)[0].equals(intersectionPoints.get(xPoint)[0])
                            && intersectionPoints.get(btmRight)[1].equals(intersectionPoints.get(yPoint)[1])) {
                        cellsFound.add(new PDFRectangle(topLeft, btmRight));
                        doBreak = true;
                        break outer;
                    }
                }
            }
        }
        return cellsFound;
    }

    public static List<? extends PDFRectangle> findTableAreas(List<? extends PDFRectangle> cells) {
        List<PDFRectangle> rectangles = new ArrayList<>();
        Set<Point2D> pointSet = new HashSet<>();
        Map<Point2D, Point2D> edgesH = new HashMap<>();
        Map<Point2D, Point2D> edgesV = new HashMap<>();
        int i = 0;

        cells = new ArrayList<>(new HashSet<>(cells));

        Collections.sort(cells, PDFRectangle.RECTANGLE_COMPARATOR);

        for (PDFRectangle cell: cells) {
            for(Point2D pt: cell.getPoints()) {
                if (pointSet.contains(pt)) {
                    pointSet.remove(pt);
                } else {
                    pointSet.add(pt);
                }
            }
        }

        List<Point2D> pointsSortX = new ArrayList<>(pointSet);
        Collections.sort(pointsSortX, X_FIRST_POINT_COMPARATOR);
        List<Point2D> pointsSortY = new ArrayList<>(pointSet);
        Collections.sort(pointsSortY, POINT_COMPARATOR);

        while (i < pointSet.size()) {
            float currY = (float) pointsSortY.get(i).getY();
            while (i < pointSet.size() && Utils.feq(pointsSortY.get(i).getY(), currY)) {
                edgesH.put(pointsSortY.get(i), pointsSortY.get(i+1));
                edgesH.put(pointsSortY.get(i+1), pointsSortY.get(i));
                i += 2;
            }
        }

        i = 0;
        while (i < pointSet.size()) {
            float currX = (float) pointsSortX.get(i).getX();
            while (i < pointSet.size() && Utils.feq(pointsSortX.get(i).getX(), currX)) {
                edgesV.put(pointsSortX.get(i), pointsSortX.get(i+1));
                edgesV.put(pointsSortX.get(i+1), pointsSortX.get(i));
                i += 2;
            }
        }

        List<List<PolygonVertex>> polygons = new ArrayList<>();
        Point2D nextVertex;
        while (!edgesH.isEmpty()) {
            ArrayList<PolygonVertex> polygon = new ArrayList<>();
            Point2D first = edgesH.keySet().iterator().next();
            polygon.add(new PolygonVertex(first, Direction.HORIZONTAL));
            edgesH.remove(first);

            while (true) {
                PolygonVertex curr = polygon.get(polygon.size() - 1);
                PolygonVertex lastAddedVertex;
                if (curr.direction == Direction.HORIZONTAL) {
                    nextVertex = edgesV.get(curr.point);
                    edgesV.remove(curr.point);
                    lastAddedVertex = new PolygonVertex(nextVertex, Direction.VERTICAL);
                    polygon.add(lastAddedVertex);
                } else {
                    nextVertex = edgesH.get(curr.point);
                    edgesH.remove(curr.point);
                    lastAddedVertex = new PolygonVertex(nextVertex, Direction.HORIZONTAL);
                    polygon.add(lastAddedVertex);
                }

                if (lastAddedVertex.equals(polygon.get(0))) {
                    polygon.remove(polygon.size() - 1);
                    break;
                }
            }

            for (PolygonVertex vertex: polygon) {
                edgesH.remove(vertex.point);
                edgesV.remove(vertex.point);
            }
            polygons.add(polygon);
        }

        for(List<PolygonVertex> poly: polygons) {
            float top = java.lang.Float.MAX_VALUE;
            float left = java.lang.Float.MAX_VALUE;
            float bottom = java.lang.Float.MIN_VALUE;
            float right = java.lang.Float.MIN_VALUE;
            for (PolygonVertex pt: poly) {
                top = (float) Math.min(top, pt.point.getY());
                left = (float) Math.min(left, pt.point.getX());
                bottom = (float) Math.max(bottom, pt.point.getY());
                right = (float) Math.max(right, pt.point.getX());
            }
            rectangles.add(new PDFRectangle(left, top, right, bottom ));
        }

        return rectangles;
    }

    private enum Direction {
        HORIZONTAL,
        VERTICAL
    }

    static class PolygonVertex {
        Point2D point;
        Direction direction;

        public PolygonVertex(Point2D point, Direction direction) {
            this.direction = direction;
            this.point = point;
        }

        @Override public boolean equals(Object other) {
            if (this == other)
                return true;
            if (!(other instanceof PolygonVertex))
                return false;
            return this.point.equals(((PolygonVertex) other).point);
        }

        @Override public int hashCode() {
            return this.point.hashCode();
        }

    }

    public class RangeComporator implements Comparator<Range> {
        @Override
        public int compare(Range o1, Range o2) {
            int retVal = 0;
            if (o1.getStart() < o2.getStart()) {
                retVal = -1;
            } else if (o1.getStart() > o2.getStart()) {
                retVal = 1;
            }
            return retVal;
        }
    }

}
