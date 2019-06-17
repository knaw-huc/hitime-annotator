package nl.knaw.huc.hitimepmerger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Create new pers- and corpname elements in appropriate controlaccess element
 *
 * Structure:
 *
 * <ead>
 *   <archdesc>
 *     <descgrp> <!-- parent -->
 *       <controlaccess>  <!-- wrapper controlaccess -->
 *         <controlaccess> <!-- type control access -->
 *           <head>Personen/Persons</head> <!-- type (in dut/eng) -->
 *           <persname encodinganalog="600$a" role="subject">Janssen, Jan</persname> <!-- persname -->
 *           <persname encodinganalog="600$a" role="subject">Pietersen, Jan</persname> <!-- persname -->
 *         </controlaccess>
 *         <controlaccess> <!-- type control access -->
 *           <head>Organisaties/Organizations</head> <!-- type (in dut/eng) -->
 *           <corpname encodinganalog="610$a" role="subject">PvdA</corpname> <!-- corpname -->
 *         </controlaccess>
 *       </controlaccess>
 *     </descgrp>
 *   </archdesc>
 * </ead>
 *
 */
public class MergeService {

  private static final Logger logger = LoggerFactory.getLogger(MergeService.class);
  private final DocumentBuilder builder;
  private final String mergeFolder;

  /**
   * map<absolute-path-to-ead, xml-document>
   */
  private final Map<String, Document> docs = new HashMap<>();

  /**
   * map<item-key, item-node>
   */
  private final Map<String, Node> nodes = new HashMap<>();
  private final Path jsonDump;
  private final Path eadDir;
  private List<ItemDto> items;

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
    try {
      this.items = asList(new ObjectMapper().readValue(jsonDump.toFile(), ItemDto[].class));
    } catch (IOException e) {
      throw new RuntimeException(format("Could not read json dump [%s]", jsonDump), e);
    }

    items.forEach(i -> findNodeOfItem(eadDir, i));
    nodes.forEach(this::addNewControlAccessItem);

    saveDocs();
  }

  private void saveDocs() {
    var mergeFileDir = Paths.get(eadDir.toString(), mergeFolder);
    mergeFileDir.toFile().mkdirs();
    this.docs.forEach((originalPath, doc) -> saveDoc(mergeFileDir, Paths.get(originalPath), doc));
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

  private void findNodeOfItem(Path eadDir, ItemDto item) {
    if (item.golden == null) {
      logger.info("Skip item: field 'golden' not set");
      return;
    }

    var idParts = item.id.split("\\.xml-");
    var eadName = idParts[0] + ".xml";
    var itemN = Integer.parseInt(idParts[1]);

    var eadPath = Paths.get(eadDir.toString(), eadName);

    var doc = getDocument(eadPath);
    if (doc == null) {
      logger.error(format("Could not find document [%s]", item.input));
      return;
    }

    var xPathfactory = XPathFactory.newInstance();
    var xpath = xPathfactory.newXPath();

    try {
      var nameType = item.type.getElementName();
      var expression = format("(//%s)[%d]", nameType, itemN + 1);
      var expr = xpath.compile(expression);
      var node = (Node) expr.evaluate(doc, XPathConstants.NODE);

      if (node.getParentNode().getNodeName().equals("controlaccess")) {
        logger.info(format("skip controlaccess item [%s]", node.getTextContent()));
        return;
      }

      nodes.put(getItemKey(item), node);
      logger.info(format("Found node of item [%s]", item.id));
    } catch (NullPointerException | XPathExpressionException e) {
      logger.error(format("Could not find node of item [%s]", item.id), e);
    }
  }

  private void addNewControlAccessItem(String itemId, Node node) {
    logger.info(format("Add new controlaccess element for item [%s]", itemId));

    var item = items
      .stream()
      .filter((i) -> itemId.equals(getItemKey(i)) && !isBlank(i.golden))
      .findFirst()
      .orElseThrow();

    var selectedCandidate = item.candidates
      .stream()
      .filter(c -> c.id.equals(item.golden))
      .findFirst()
      .orElseThrow();

    var text = selectedCandidate.names.get(0);
    var doc = node.getOwnerDocument();
    var newItemNode = createNewItemNode(item, doc, text);

    // find parent element containing wrapper controlaccess element:
    var controlaccessParent = getParentByNames(node, newArrayList("archdesc", "descgrp"));
    if (controlaccessParent == null) {
      logger.error(format("No parent for new controlaccess element of item [%s]", item.input));
      return;
    }

    var itemControlaccess = getTypeControlaccess(controlaccessParent, doc, item.type);
    itemControlaccess.appendChild(newItemNode);
  }


  private Node createTypeControlaccess(Node controlaccessParent, Document doc, ItemType type) {
    Node typeControlaccess = doc.createElement("controlaccess");
    controlaccessParent.appendChild(typeControlaccess);
    var head = doc.createElement("head");
    typeControlaccess.appendChild(head);
    head.appendChild(doc.createTextNode(type.getHeadDut()));
    return typeControlaccess;
  }

  private Node createWrapperControlaccess(Node controlAccessParent) {
    Node wrapperControlaccess = controlAccessParent
      .getOwnerDocument()
      .createElement("controlaccess");
    controlAccessParent.appendChild(wrapperControlaccess);
    return wrapperControlaccess;
  }

  private Element createNewItemNode(ItemDto item, Document doc, String text) {
    var itemEl = doc.createElement(item.type.getElementName());

    itemEl.setAttribute("role", "subject");
    itemEl.setAttribute("source", "NL-AMISG");
    itemEl.setAttribute("authfilenumber", "" + item.golden);
    itemEl.setAttribute("encodinganalog", item.type.getEncodinganalog());

    logger.info("text: " + text);
    var itemText = doc.createTextNode(text);
    itemEl.appendChild(itemText);
    return itemEl;
  }

  /**
   * Find controlaccess element in wrapper controlaccess containing all pers- or corpnames (depending on type)
   */
  private Node getTypeControlaccess(Node controlAccessParent, Document doc, ItemType type) {
    var wrapper = getWrapperControlaccess(controlAccessParent);
    for (var k = 0; k < wrapper.getChildNodes().getLength(); k++) {
      var typeControlaccess = wrapper.getChildNodes().item(k);
      var head = typeControlaccess.getChildNodes().item(0);

      var matchDut = head.getTextContent().equals(type.getHeadDut());
      var matchEng = head.getTextContent().equals(type.getHeadEng());
      var isHead = head.getNodeName().equals("head");

      if (isHead && (matchDut || matchEng)) {
        return typeControlaccess;
      }
    }
    return createTypeControlaccess(wrapper, doc, type);
  }

  /**
   * Return controlaccess element around type controlaccess-elements
   */
  private Node getWrapperControlaccess(Node controlAccessParent) {
    var children = controlAccessParent.getChildNodes();
    for (var i = 0; i < children.getLength(); i++) {
      var child = children.item(i);
      if (child.getNodeName().equals("controlaccess")) {
        return child;
      }
    }
    return createWrapperControlaccess(controlAccessParent);
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
      return parent;
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
      logger.error(format("Could not convert document to file [%s]: %s", path, ex.getMessage()));
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

  private String getItemKey(ItemDto item) {
    return item.type.getType() + item.id;
  }

}

