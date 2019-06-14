package nl.knaw.huc.hitimepmerger;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeServiceTest {

  @Test
  void testMerge_createsMergeFileWithControlaccessPersname() throws Exception {

    var eadName = "minimal-ead.xml";

    var dumpMinimal = getTestResourcePath("dump-minimal.json");
    var eadPath = getTestResourcePath("FINAL/").getParent();
    var mergeService = new MergeService(dumpMinimal, eadPath, "MERGED");

    mergeService.merge();

    var mergedFile = Paths.get(getTestResourcePath("MERGED").toString(), eadName);
    assertTrue(mergedFile.toFile().exists());
    var mergedString = FileUtils.readFileToString(mergedFile.toFile(), UTF_8);
    assertThat(mergedString)
      .contains("<controlaccess><persname " +
        "authfilenumber=\"460147\" " +
        "encodinganalog=\"600$a\" " +
        "role=\"subject\" " +
        "source=\"NL-AMISG\">" +
        "Janssen, Jan" +
        "</persname>" +
        "</controlaccess>"
      );
  }

  private static Path getTestResourcePath(String fileName) throws URISyntaxException {
    return Paths.get(
      Thread
        .currentThread()
        .getContextClassLoader()
        .getResource(fileName)
        .toURI());
  }
}


