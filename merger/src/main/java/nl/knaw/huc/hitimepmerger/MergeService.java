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
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static nl.knaw.huc.hitimepmerger.ItemType.CORP;
import static nl.knaw.huc.hitimepmerger.ItemType.GEOG;
import static nl.knaw.huc.hitimepmerger.ItemType.PERS;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Create new pers- and corpname elements in appropriate controlaccess element
 * <p>
 * Structure:
 *
 * <ead>
 *   <archdesc>
 *     <descgrp> <!-- parent contains items and controlaccess -->
 *       <controlaccess>  <!-- wrapper controlaccess -->
 *         <controlaccess> <!-- type controlaccess -->
 *           <head>Personen/Persons</head> <!-- head marks type of controlaccess (in dut or eng) -->
 *           <persname encodinganalog="600$a" role="subject">Janssen, Jan</persname> <!-- persname -->
 *           <persname encodinganalog="600$a" role="subject">Pietersen, Jan</persname> <!-- persname -->
 *         </controlaccess>
 *         <controlaccess> <!-- type control access -->
 *           <head>Organisaties/Organizations</head> <!-- corp type (in dut or eng) -->
 *           <corpname encodinganalog="610$a" role="subject">PvdA</corpname> <!-- corpname -->
 *         </controlaccess>
 *       </controlaccess>
 *     </descgrp>
 *   </archdesc>
 * </ead>
 */
public class MergeService {

  private static final Logger logger = LoggerFactory.getLogger(MergeService.class);

  /**
   * Elements that can contain a controlaccess section
   */
  private static final List<String> parentNames = newArrayList("did", "descgrp", "dsc");

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
      logger.info(format("Skip item [%s][%s]: field 'golden' not set", item.type.getType(), item.id));
      return;
    }
    if (item.controlaccess) {
      logger.info(format("Skip item [%s][%s]: is in controlaccess", item.type.getType(), item.id));
      return;
    }

    var idParts = item.id.split("\\.xml-");
    var eadName = idParts[0] + ".xml";
    var itemN = Integer.parseInt(idParts[1]);

    var eadPath = Paths.get(eadDir.toString(), eadName);
    var doc = getDocument(eadPath);
    if (doc == null) {
      logger.error(format("Could not find document of [%s]", eadDir));
      return;
    }

    var xPathfactory = XPathFactory.newInstance();
    var xpath = xPathfactory.newXPath();

    var nameType = item.type.getElementName();
    var expression = format("(//%s)[%d]", nameType, itemN + 1);

    Node node;
    try {
      var expr = xpath.compile(expression);
      node = (Node) expr.evaluate(doc, XPathConstants.NODE);
    } catch (NullPointerException | XPathExpressionException e) {
      logger.error(format("Could not find item [%s][%s]: ", item.type.getType(), item.id), e);
      return;
    }

    if (node == null) {
      logger.error(format("Could not find item [%s][%s]: ", item.type.getType(), item.id));
      return;
    }

    nodes.put(getItemKey(item), node);
    logger.info(format("Found xml node of item [%s][%s]", item.type.getType(), item.id));
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

    var candidateName = selectedCandidate.names.get(0);
    var doc = node.getOwnerDocument();

    var controlaccessParent = getParentByNames(node, parentNames);
    if (controlaccessParent == null) {
      logger.error(format("No parent for new controlaccess element of item [%s]", item.input));
      return;
    }

    var itemControlaccess = getTypeControlaccess(controlaccessParent, item.type);
    var itemEl = findChildNodeByTextContent(itemControlaccess, candidateName);
    if (itemEl != null) {
      setItemAttributes(item, itemEl);
    } else {
      var newItemNode = createNewItemNode(item, doc, candidateName);
      itemControlaccess.appendChild(newItemNode);
    }
  }

  private Element createNewItemNode(ItemDto item, Document doc, String text) {
    var itemEl = doc.createElement(item.type.getElementName());
    setItemAttributes(item, itemEl);
    var itemText = doc.createTextNode(text);
    itemEl.appendChild(itemText);
    return itemEl;
  }

  private void setItemAttributes(ItemDto item, Element itemEl) {
    itemEl.setAttribute("role", "subject");
    itemEl.setAttribute("source", "NL-AMISG");
    itemEl.setAttribute("authfilenumber", "" + item.golden);
    itemEl.setAttribute("encodinganalog", item.type.getEncodinganalog());
  }

  private Node getTypeControlaccess(Node controlAccessParent, ItemType type) {
    var wrapper = getWrapperControlaccess(controlAccessParent);
    for (var i = 0; i < wrapper.getChildNodes().getLength(); i++) {
      var typeControlaccess = wrapper.getChildNodes().item(i);
      var head = findChildNodeByName(typeControlaccess, "head");

      if (head == null || !head.getNodeName().equals("head")) {
        continue;
      }

      var matchDut = head.getTextContent().equals(type.getHeadDut());
      var matchEng = head.getTextContent().equals(type.getHeadEng());
      if (matchDut || matchEng) {
        return typeControlaccess;
      }
    }
    return createTypeControlaccess(wrapper, type);
  }

  private Node createTypeControlaccess(Node wrapper, ItemType type) {
    var doc = wrapper.getOwnerDocument();
    Node typeControlaccess = doc.createElement("controlaccess");
    addTypeControlaccessToWrapper(wrapper, typeControlaccess, type);
    var head = doc.createElement("head");
    typeControlaccess.appendChild(head);
    String headTxt = getHeadText(type, doc);
    head.appendChild(doc.createTextNode(headTxt));
    return typeControlaccess;
  }

  /**
   * persname: controlaccess should be added after geogname
   * corpname: controlaccess should be added after geogname and persname
   */
  private void addTypeControlaccessToWrapper(Node wrapper, Node typeControlaccess, ItemType type) {
    ArrayList<ItemType> nonprependable = null;
    if (type == CORP) {
      nonprependable = newArrayList(GEOG, PERS);
    } else if(type == PERS) {
      nonprependable = newArrayList(GEOG);
    }

    var toPrepend = new AtomicReference<Node>();
    for (var i = 0; i < wrapper.getChildNodes().getLength(); i++) {
      var wrapperChild = wrapper.getChildNodes().item(i);
      var head = findChildNodeByName(wrapperChild, "head");
      if(head == null){
        continue;
      }
      var headText = head.getTextContent().trim();
      nonprependable.forEach((np) -> {
        var npHeadText = getHeadText(np, wrapper.getOwnerDocument());
        if(!npHeadText.equals(headText)) {
          toPrepend.set(wrapperChild);
        }
      });
    }
    if(toPrepend.get() != null) {
      wrapper.insertBefore(typeControlaccess, toPrepend.get());
    } else {
      wrapper.appendChild(typeControlaccess);
    }
  }

  private String getHeadText(ItemType type, Document doc) {
    var language = getLanguage(doc);
    return language.equals("dut")
      ? type.getHeadDut()
      : type.getHeadEng();
  }

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

  private Node createWrapperControlaccess(Node controlAccessParent) {
    Node wrapperControlaccess = controlAccessParent
      .getOwnerDocument()
      .createElement("controlaccess");
    controlAccessParent.appendChild(wrapperControlaccess);
    return wrapperControlaccess;
  }

  private Element findChildNodeByTextContent(Node node, String textContent) {
    var childNodes = node.getChildNodes();
    for (var i = 0; i < childNodes.getLength(); i++) {
      var item = childNodes.item(i);
      if (item.getTextContent().trim().equals(textContent.trim())) {
        return (Element) item;
      }
    }
    return null;
  }

  private Node findChildNodeByName(Node typeControlaccess, String name) {
    var childNodes = typeControlaccess.getChildNodes();
    for (var i = 0; i < childNodes.getLength(); i++) {
      var childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(name)) {
        return childNode;
      }
    }
    return null;
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

  private String getLanguage(Document doc) {
    return doc
      .getElementsByTagName("language")
      .item(0)
      .getAttributes()
      .getNamedItem("langcode")
      .getNodeValue();
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
    return format("%s-%s", item.type.getType(), item.id);
  }

}
