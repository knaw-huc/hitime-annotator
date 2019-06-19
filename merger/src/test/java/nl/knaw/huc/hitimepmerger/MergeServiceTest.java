package nl.knaw.huc.hitimepmerger;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.collect.Lists.newArrayList;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static javax.xml.xpath.XPathConstants.NUMBER;
import static nl.knaw.huc.hitimepmerger.XmlTestUtil.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class MergeServiceTest {

  @Test
  void testMerge_createsMergeFileWithControlaccessPersname() throws Exception {
    var eadName = "ead-01-minimal.xml";
    var dumpMinimal = getTestResourcePath("dump-01-minimal.json");
    var eadPath = getTestResourcePath("FINAL/").getParent();

    var mergeService = new MergeService(dumpMinimal, eadPath, "MERGED");
    mergeService.merge();

    var mergedFile = Paths.get(getTestResourcePath("MERGED").toString(), eadName);
    assertThat(mergedFile.toFile()).exists();
    var node = (Node) evaluate(mergedFile, "(/ead/archdesc/descgrp[@type='context']/controlaccess/controlaccess/persname)[1]", NODE);
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
    assertThat(mergedFile.toFile()).exists();
    var count = evaluate(mergedFile, "count(/ead/archdesc/descgrp[@type='content_and_structure']/controlaccess/controlaccess/persname)", NUMBER);
    assertThat(count).isEqualTo(2.0);
  }

  @Test
  void testMerge_putsNamesOfPersAndCorpInDifferentControlaccessGroups() throws Exception {
    var eadName = "ead-03-pers-and-corp.xml";
    var dumpMinimal = getTestResourcePath("dump-03-pers-and-corp.json");
    var eadPath = getTestResourcePath("FINAL/").getParent();

    var mergeService = new MergeService(dumpMinimal, eadPath, "MERGED");
    mergeService.merge();

    var mergedFile = Paths.get(getTestResourcePath("MERGED").toString(), eadName);
    assertThat(mergedFile.toFile()).exists();
    var countHeads = evaluate(mergedFile, "count(/ead/archdesc/descgrp[@type='content_and_structure']/controlaccess/controlaccess)", NUMBER);
    assertThat(countHeads).isEqualTo(2.0);
    var heads = (NodeList) evaluate(mergedFile, "/ead/archdesc/descgrp[@type='content_and_structure']/controlaccess/controlaccess/head", NODESET);
    assertThat(heads.getLength()).isEqualTo(2);
    var expectedHeads = newArrayList("Personen", "Organisaties");
    assertThat(heads.item(0).getTextContent()).isIn(expectedHeads);
    assertThat(heads.item(1).getTextContent()).isIn(expectedHeads);
  }

  @Test
  void testMerge_shouldCreateEnglishHead_whenNotDutch() throws Exception {
    var eadName = "ead-04-italian.xml";
    var dumpMinimal = getTestResourcePath("dump-04-italian.json");
    var eadPath = getTestResourcePath("FINAL/").getParent();

    var mergeService = new MergeService(dumpMinimal, eadPath, "MERGED");
    mergeService.merge();

    var mergedFile = Paths.get(getTestResourcePath("MERGED").toString(), eadName);
    assertThat(mergedFile.toFile()).exists();
    var head = (Node) evaluate(mergedFile, "/ead/archdesc/descgrp/controlaccess/controlaccess/head", NODE);
    assertThat(head.getTextContent()).isEqualTo("Persons");
  }

  @Test
  void testMerge_shouldIgnoreItemsInControlaccess_whenFindingByItemId() throws Exception {
    var eadName = "ead-05-with-controlaccess.xml";
    var dumpMinimal = getTestResourcePath("dump-05-with-controlaccess.json");
    var eadPath = getTestResourcePath("FINAL/").getParent();

    var mergeService = new MergeService(dumpMinimal, eadPath, "MERGED");
    mergeService.merge();

    var mergedFile = Paths.get(getTestResourcePath("MERGED").toString(), eadName);
    assertThat(mergedFile.toFile()).exists();

    var count = evaluate(mergedFile, "count(/ead/archdesc/descgrp[@type='context']/controlaccess/controlaccess/persname)", NUMBER);
    assertThat(count).isEqualTo(1.0);
    var node = (Node) evaluate(mergedFile, "(/ead/archdesc/descgrp[@type='context']/controlaccess/controlaccess/persname)[1]", NODE);
    assertThat(node.getTextContent()).isEqualToIgnoringWhitespace("Janssen, Jan");

    var node2 = (Node) evaluate(mergedFile, "(/ead/archdesc/descgrp[@type='content_and_structure']/controlaccess/controlaccess/persname)[1]", NODE);
    assertThat(node2.getTextContent()).isEqualToIgnoringWhitespace("Janssen, Jan");
  }

  @Test
  void testMerge_shouldMergeExistingPersCorpnameElementWithNewItems() throws Exception {
    var eadName = "ead-06-existing-persname.xml";
    var dumpMinimal = getTestResourcePath("dump-06-existing-persname.json");
    var eadPath = getTestResourcePath("FINAL/").getParent();

    var mergeService = new MergeService(dumpMinimal, eadPath, "MERGED");
    mergeService.merge();

    var mergedFile = Paths.get(getTestResourcePath("MERGED").toString(), eadName);
    assertThat(mergedFile.toFile()).exists();

    var count = evaluate(mergedFile, "count(/ead/archdesc/descgrp[@type='context']/controlaccess/controlaccess/persname)", NUMBER);
    assertThat(count).isEqualTo(1.0);
    var node = (Node) evaluate(mergedFile, "(/ead/archdesc/descgrp[@type='context']/controlaccess/controlaccess/persname)[1]", NODE);
    assertThat(node.getTextContent()).isEqualToIgnoringWhitespace("Janssen, Jan");
    assertThat(node.getAttributes().getNamedItem("authfilenumber").getTextContent()).isEqualToIgnoringWhitespace("460147");
    assertThat(node.getAttributes().getNamedItem("encodinganalog").getTextContent()).isEqualToIgnoringWhitespace("600$a");
    assertThat(node.getAttributes().getNamedItem("role").getTextContent()).isEqualToIgnoringWhitespace("subject");
    assertThat(node.getAttributes().getNamedItem("source").getTextContent()).isEqualToIgnoringWhitespace("NL-AMISG");

  }

  @Test
  void testMerge_insertsControlaccessInCorrectOrder() throws Exception {
    var eadName = "ead-07-geog-corp.xml";
    var dumpMinimal = getTestResourcePath("dump-07-geog-corp.json");
    var eadPath = getTestResourcePath("FINAL/").getParent();

    var mergeService = new MergeService(dumpMinimal, eadPath, "MERGED");
    mergeService.merge();

    var mergedFile = Paths.get(getTestResourcePath("MERGED").toString(), eadName);
    assertThat(mergedFile.toFile()).exists();

    // persname should be second controllaccess element (and not first or third)
    var node = (Node) evaluate(mergedFile, "(/ead/archdesc/descgrp[@type='context']/controlaccess/controlaccess[2]/persname)[1]", NODE);
    assertThat(node.getTextContent()).isEqualTo("Janssen, Jan");
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
