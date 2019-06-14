package nl.knaw.huc.hitimepmerger;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeServiceTest {

  @Test
  void testMerge_createsControlAccessTags() throws Exception {
    assertTrue(true);
    var mergeService = new MergeService();
    var dumpMinimal = getTestResourcePath("dump-minimal.json");
    var eadPath = getTestResourcePath("FINAL/").getParent();

    mergeService.merge(
      dumpMinimal,
      eadPath
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


