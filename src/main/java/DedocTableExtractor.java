import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import debug.DebugDrawer;
import exceptions.EmptyArgumentException;
import extractors.BlockComposer;
import extractors.ExtractionManager;
import extractors.filters.YInterLineSpaceCompositionFilter;
import model.Document;
import model.Page;
import model.table.Table;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import writers.HtmlTableWriter;
import writers.JsonDocumentWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;


public class DedocTableExtractor {

    // CLI params
    @Option(name = "-i", aliases = {"--input"}, required = true, metaVar = "PATH", usage = "specify a file")
    private String inArg;
    private File inputFile;
    private Path inputPath;

    @Option(name = "-o", aliases = {"--output"}, metaVar = "PATH", usage = "specify a directory for extracted data")
    private String outArg;
    private File outputFile;
    private Path outputPath;

    @Option(name = "-sp", aliases = {"--start"}, usage = "specify a start page")
    private String sPage;
    private int startPage;

    @Option(name = "-ep", aliases = {"--end"}, usage = "specify a end page")
    private String ePage;
    private int endPage;

    @Option(name = "-?", aliases = {"--help"}, usage = "show this message")
    private boolean help = false;

    public static void main(String[] args) {
        new DedocTableExtractor().run(args);
    }

    public void run(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);

            if (help) {
                parser.printUsage(System.err);
                System.exit(0);
            }

            throwIfEmpty(inArg);
            inputFile = new File(inArg);
            inputPath = inputFile.isFile() ? inputFile.getParentFile().toPath() : inputFile.toPath();

            if (isEmptyArg(outArg)) {
                outputFile = inputPath.resolve("output").toFile();
            } else {
                outputFile = new File(outArg);
            }

            outputFile.mkdirs();
            outputPath = outputFile.toPath();

            if (inputFile.isFile()) {

                if (sPage != null && !sPage.isEmpty() && ePage != null && !ePage.isEmpty()) {
                    startPage = Integer.parseInt(sPage);
                    endPage = Integer.parseInt(ePage);
                    //if (startPage <= endPage) {
                    extract(inputFile.toPath(), startPage - 1, endPage - 1);
                    //}
                } else {
                    extract(inputFile.toPath());
                }
            } else {
                final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.{pdf,PDF}");
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(inputPath, "*.pdf")) {
                    for (Path file : directoryStream) {
                        if (matcher.matches(file.getFileName())) {
                            extract(file);
                        }
                    }
                }
                catch (IOException | DirectoryIteratorException ex) {
                    ex.printStackTrace();
                }
            }

        } catch (CmdLineException | IOException e) {
            e.printStackTrace();
        }
    }

    public void extract(Path path, int startPage, int endPage) throws IOException {

        Document document = null;
        DebugDrawer debugDrawer = null;

        document = Document.load(path, startPage, endPage);
        int lastPageIndex = document.getPageCnt();
        if (startPage > lastPageIndex)
            return;
        startPage = startPage < 0 ? 0: startPage;
        endPage = endPage > lastPageIndex ? lastPageIndex : endPage;
        BlockComposer bc = new BlockComposer();
        bc.compose(document, startPage, endPage);

        ExtractionManager em = new ExtractionManager(document);
        List<Table> tables = em.extract(startPage, endPage);
        if (tables != null) {
            for (Table table: tables) {
                table.splitCells();
                table.completeRows();
                table.removeEmptyRows();
            }
        }

        writeTables(document);
        printJSON(document, startPage, endPage);

        /*        Path debugDirPath = outputPath.resolve("debug");
        DebugDrawer.Builder debugDrawerBuilder = new DebugDrawer.Builder(debugDirPath)
                .setChunkDirectoryName("Chunks")
                .setChunkFileNameSuffix("CHUNKS")
                .setCharDirectoryName("Chars")
                .setCharFileNameSuffix("CHARS")
                .setWordDirectoryName("Words")
                .setWordFileNameSuffix("WORDS")
                .setBlockDirectoryName("Blocks")
                .setBlockFileNameSuffix("BLOCKS")
                .setRulingDirectoryName("Rulings")
                .setRulingFileNameSuffix("RULINGS")
                .setBorderedTableDirectoryName("BorderedTables")
                .setBorderedTableFileNameSuffix("BORDERED");

        debugDrawer = debugDrawerBuilder.createDebugDrawer(document);
        debugDrawer.drawBeforeRecomposing();*/

    }


    public void extract(Path path) throws IOException {
        Document document = null;
        DebugDrawer debugDrawer = null;
        PDDocument pdDocument = Loader.loadPDF(path.toFile());
        document = Document.load(path, 0, pdDocument.getPages().getCount() - 1);

        BlockComposer bc = new BlockComposer();
        bc.compose(document);

        ExtractionManager em = new ExtractionManager(document);
        List<Table> tables = em.extract();
        if (tables != null) {
            for (Table table: tables) {
                table.splitCells();
                table.completeRows();
                table.removeEmptyRows();
            }
        }

/*        Path debugDirPath = outputPath.resolve("debug");
        DebugDrawer.Builder debugDrawerBuilder = new DebugDrawer.Builder(debugDirPath)
                .setChunkDirectoryName("Chunks")
                .setChunkFileNameSuffix("CHUNKS")
                .setCharDirectoryName("Chars")
                .setCharFileNameSuffix("CHARS")
                .setWordDirectoryName("Words")
                .setWordFileNameSuffix("WORDS")
                .setBlockDirectoryName("Blocks")
                .setBlockFileNameSuffix("BLOCKS")
                .setRulingDirectoryName("Rulings")
                .setRulingFileNameSuffix("RULINGS")
                .setBorderedTableDirectoryName("BorderedTables")
                .setBorderedTableFileNameSuffix("BORDERED");

        debugDrawer = debugDrawerBuilder.createDebugDrawer(document);
        debugDrawer.drawBeforeRecomposing();

        writeTables(document);*/
        printJSON(document);
    }

    private void printJSON(Document document) {
        JsonDocumentWriter writer = new JsonDocumentWriter(document);
        System.out.println(writer.write());
    }

    private void printJSON(Document document, int startPage, int endPage) {
        JsonDocumentWriter writer = new JsonDocumentWriter(document, startPage, endPage);
        System.out.println(writer.write());
    }

    private void printTables(Document document) throws IOException {
        Path output = outputPath.resolve("tables");
        Files.createDirectories(output);

        HtmlTableWriter writer = new HtmlTableWriter();
        StringBuilder html = new StringBuilder();

        final String fileName = FilenameUtils.getBaseName(document.getSourceFile().getName());

        html.append("<!DOCTYPE html>").append(System.lineSeparator());
        html.append("<html>").append(System.lineSeparator());

        // Write head
        html.append("<head>").append(System.lineSeparator());
        html.append("<meta charset=\"UTF-8\">").append(System.lineSeparator());
        html.append("<title>").append(fileName).append("</title>").append(System.lineSeparator());
        html.append("</head>").append(System.lineSeparator());

        // Write body
        html.append("<body>").append(System.lineSeparator());

        try {
            Iterator<Page> pages = document.getPagesItrerator();
            while (pages.hasNext()) {
                Page page = pages.next();
                html.append("<h1>Page ").append(page.getIndex()).append("</h1>").append(System.lineSeparator());

                List<Table> tables = page.getTables();
                if (null == tables || tables.isEmpty())
                    continue;
                tables.sort(Comparator.comparing(Table::getPageIndex).thenComparing(Table::getTop));
                for (Table table: tables) {
                    table.splitCells();
                    table.completeRows();
                    table.removeEmptyRows();
                    if (table.isContinued())
                        continue;
                    else {
                        html.append("<p>").append(System.lineSeparator());
                        html.append(writer.write(table));
                        html.append("</p>").append(System.lineSeparator());
                    }
                }
            }

            html.append("</body>").append(System.lineSeparator());
            html.append("</html>");

            System.out.println(html.toString());

        }
        catch (ParserConfigurationException | TransformerException | IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTables(Document document) throws IOException {
        Path output = outputPath.resolve("tables");
        Files.createDirectories(output);

        HtmlTableWriter writer = new HtmlTableWriter();
        StringBuilder html = new StringBuilder();

        final String fileName = FilenameUtils.getBaseName(document.getSourceFile().getName());

        html.append("<!DOCTYPE html>").append(System.lineSeparator());
        html.append("<html>").append(System.lineSeparator());

        // Write head
        html.append("<head>").append(System.lineSeparator());
        html.append("<meta charset=\"UTF-8\">").append(System.lineSeparator());
        html.append("<title>").append(fileName).append("</title>").append(System.lineSeparator());
        html.append("</head>").append(System.lineSeparator());

        // Write body
        html.append("<body>").append(System.lineSeparator());

        try {
            Iterator<Page> pages = document.getPagesItrerator();
            while (pages.hasNext()) {
                Page page = pages.next();
                html.append("<h1>Page ").append(page.getIndex()).append("</h1>").append(System.lineSeparator());

                List<Table> tables = page.getTables();
                if (null == tables || tables.isEmpty())
                    continue;
                tables.sort(Comparator.comparing(Table::getPageIndex).thenComparing(Table::getTop));
                for (Table table: tables) {
                    if (table.isContinued())
                        continue;
                    else {
                        html.append("<p>").append(System.lineSeparator());
                        html.append(writer.write(table));
                        html.append("</p>").append(System.lineSeparator());
                    }
                }
            }

            html.append("</body>").append(System.lineSeparator());
            html.append("</html>");

            String fn = String.format("%s.html", fileName);
            Path p = output.resolve(fn);

            Writer fileWriter = new OutputStreamWriter(new FileOutputStream(p.toFile()), StandardCharsets.UTF_8);
            fileWriter.write(html.toString());
            fileWriter.close();
        }
        catch (ParserConfigurationException | TransformerException | IOException e) {
            e.printStackTrace();
        }
    }

    private void throwIfEmpty(String arg) {
        if (isEmptyArg(arg)) {
            throw new EmptyArgumentException("A required option was not specified");
        }
    }

    private boolean isEmptyArg(String arg) {
        return arg == null || arg.isEmpty();
    }



}
