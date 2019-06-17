package nl.knaw.huc.hitimepmerger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static nl.knaw.huc.hitimepmerger.XmlUtil.getNode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeServiceTest {

  @Test
  void testMerge_createsMergeFileWithControlaccessPersname() throws Exception {
    var eadName = "ead-01-minimal.xml";
    var dumpMinimal = getTestResourcePath("dump-01-minimal.json");
    var eadPath = getTestResourcePath("FINAL/").getParent();

    var mergeService = new MergeService(dumpMinimal, eadPath, "MERGED");
    mergeService.merge();

    var mergedFile = Paths.get(getTestResourcePath("MERGED").toString(), eadName);
    assertTrue(mergedFile.toFile().exists());
    var node = getNode(mergedFile, "(/ead/archdesc/descgrp[@type='context']/controlaccess/controlaccess/persname)[1]");
    assertThat(node.getTextContent()).isEqualTo("Janssen, Jan");
  }

  @Test
  void testMerge_putsNamesOfSameDescgrpInSameControlaccessTag() throws Exception {
    var eadName = "ead-02-two-persnames.xml";
    var dumpMinimal = getTestResourcePath("dump-02-two-persnames.json");
    var eadPath = getTestResourcePath("FINAL/").getParent();

    var mergeService = new MergeService(dumpMinimal, eadPath, "MERGED");
    mergeService.merge();

    var mergedFile = Paths.get(getTestResourcePath("MERGED").toString(), eadName);
    assertTrue(mergedFile.toFile().exists());
    var node = getNode(mergedFile, "/ead/archdesc/descgrp[@type='content_and_structure']/controlaccess");
    assertThat(node.getChildNodes().getLength()).isEqualTo(2);
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


