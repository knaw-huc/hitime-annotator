package nl.knaw.huc.hitimepmerger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public class MergeService {

  private static final Logger logger = LoggerFactory.getLogger(MergeService.class);
  private final DocumentBuilder builder;
  private final String mergeFolder;

  /**
   * map<absolute-path-to-ead, xml-document>
   */
  private final Map<String, Document> docs = new HashMap<>();

  /**
   * map<item-id, item-node>
   */
  private final Map<String, Node> nodes = new HashMap<>();
  private final Path jsonDump;
  private final Path eadDir;

  public MergeService(Path jsonDump, Path eadDir, String mergeFolder) {
    this.jsonDump = jsonDump;
    this.eadDir = eadDir;
    this.mergeFolder = mergeFolder;

    var factory = DocumentBuilderFactory.newInstance();
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Could not create xml document builder", e);
    }
  }

  public void merge() {
    List<ItemDto> items;
    try {
      items = asList(new ObjectMapper().readValue(jsonDump.toFile(), ItemDto[].class));
    } catch (IOException e) {
      throw new RuntimeException(format("Could not read json dump [%s]", jsonDump), e);
    }

    items.forEach(i -> findNodeOfItem(eadDir, i));
    items.forEach(this::addNewControlAccessItem);

    saveDocs();
  }

  private void saveDocs() {
    var mergeFileDir = Paths.get("/", eadDir.toString(), mergeFolder);
    mergeFileDir.toFile().mkdirs();

    this.docs.forEach((originalPath, doc) -> {
      saveDoc(mergeFileDir, Paths.get(originalPath), doc);
    });
  }

  private void saveDoc(Path mergeFileDir, Path originalPath, Document doc) {
    var newMergeFileName = originalPath.getFileName();
    var newMergeFilePath = Paths.get(mergeFileDir.toString(), newMergeFileName.toString());

    try {
      newMergeFilePath.toFile().createNewFile();
    } catch (IOException e) {
      logger.error(format("Could not create merge file [%s]", newMergeFilePath), e);
      return;
    }

    toFile(doc, newMergeFilePath);
  }

  private void findNodeOfItem(Path eadDir, ItemDto i) {
    var idParts = i.id.split("\\.xml-");
    var eadName = idParts[0] + ".xml";
    var itemN = Integer.parseInt(idParts[1]);

    logger.info(format("Find node of item [%s]", i.id));

    var eadPath = Paths.get(eadDir.toString(), eadName);

    var doc = getDocument(eadPath);
    if (doc == null) {
      logger.error(format("Could not find document [%s]", i.input));
      return;
    }

    var xPathfactory = XPathFactory.newInstance();
    var xpath = xPathfactory.newXPath();

    try {
      var nameType = i.type.getType() + "name";
      var expression = format("(//%s)[%d]", nameType, itemN + 1);
      var expr = xpath.compile(expression);
      var node = (Node) expr.evaluate(doc, XPathConstants.NODE);

      if (node.getParentNode().getNodeName().equals("controlaccess")) {
        logger.info(format("skip controlaccess item [%s]", node.getTextContent()));
        return;
      }

      nodes.put(i.id, node);
    } catch (NullPointerException | XPathExpressionException e) {
      logger.error(format("Could not find node of item [%s]", i.id), e);
    }
  }

  private void addNewControlAccessItem(ItemDto i) {
    var node = nodes.get(i.id);
    if (node == null) {
      logger.warn(format("No node found for item [%s]", i.id));
      return;
    }
    var controlAccessParent = getParentByNames(node, newArrayList("archdesc", "descgrp"));
    if (controlAccessParent == null) {
      logger.error(format("No parent for new controlaccess element of item [%s]", i.input));
      return;
    }

    var doc = node.getOwnerDocument();
    var wrapperControlaccess = doc.createElement("controlaccess");
    controlAccessParent.appendChild(wrapperControlaccess);
    var itemControlaccess = doc.createElement("controlaccess");
    wrapperControlaccess.appendChild(itemControlaccess);
    var item = doc.createElement("persname");
    itemControlaccess.appendChild(item);

    item.setAttribute("role", "subject");
    item.setAttribute("source", "NL-AMISG");
    item.setAttribute("authfilenumber", "" + i.golden);
    item.setAttribute("encodinganalog", i.type.getEncodinganalog());

    var candidate = i.candidates
      .stream()
      .filter(c -> c.id.equals(i.golden))
      .findFirst()
      .orElseThrow();
    var itemText = doc.createTextNode(candidate.names.get(0));
    item.appendChild(itemText);
  }

  private Document getDocument(Path eadPath) {
    var key = eadPath.toString();
    if (docs.containsKey(key)) {
      return docs.get(key);
    }
    try {
      docs.put(key, builder.parse(eadPath.toFile()));
      return docs.get(key);
    } catch (SAXException | IOException e) {
      logger.error(format("Could not parse document [%s]: %s", eadPath.getFileName(), e.getMessage()));
    }
    return null;
  }

  private Node getParentByNames(Node node, List<String> names) {
    var parent = node.getParentNode();
    if (parent == null) {
      return null;
    } else if (names.contains(parent.getNodeName())) {
      return node;
    } else {
      return getParentByNames(parent, names);
    }
  }

  private static void toFile(Document doc, Path path) {
    try {
      var transformer = getTransformer();
      var writer = new FileWriter(path.toFile());
      var result = new StreamResult(writer);
      transformer.transform(new DOMSource(doc), result);
    } catch (TransformerException | IOException ex) {
      logger.error(format("Could not convert document to file [%s]", path), ex);
    }
  }

  private static Transformer getTransformer() throws TransformerConfigurationException {
    var tf = TransformerFactory.newInstance();
    var transformer = tf.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    return transformer;
  }

}

