import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.io.File;
import java.io.IOException;


public class TextParserTest {

    @InjectMocks
    private DedocTableExtractor dedocTableExtractor;

    @Test
    public void testSaveJson() throws IOException {
        String[] args = {
                "-i", "./data/prospectus_part.pdf",
                "-s", "./data/outdata.json",
        };

        dedocTableExtractor.main(args);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(new File("./data/outdata.json"));
        Assertions.assertEquals(node.get("document").asText(), "prospectus_part.pdf");
    }


}