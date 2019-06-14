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
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public class MergeService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final DocumentBuilder builder;
  private final Map<String, Document> docs = new HashMap<>();
  private final Map<String, Node> nodes = new HashMap<>();

  public MergeService() {
    var factory = DocumentBuilderFactory.newInstance();
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Could not create xml document builder", e);
    }
  }

  public void merge(Path jsonDump, Path eadDir) {
    List<ItemDto> items;
    try {
      items = asList(new ObjectMapper().readValue(jsonDump.toFile(), ItemDto[].class));
    } catch (IOException e) {
      throw new RuntimeException(format("Could not read json dump [%s]", jsonDump), e);
    }

    items.forEach(i -> findNodeOfItem(eadDir, i));
    items.forEach(this::addNewControlAccessItem);

  }

  private void findNodeOfItem(Path eadDir, ItemDto i) {
    var idParts = i.id.split("\\.xml-");
    var eadName = idParts[0] + ".xml";
    var itemN = Integer.parseInt(idParts[1]);

    logger.info(format("handling id [%s]: file [%s] item [%d] input [%s]", i.id, eadName, itemN, i.input));

    var eadPath = Paths.get(eadDir.toString(), eadName);

    var doc = getDocument(eadPath);
    if(doc == null) {
      logger.error(format("Could not find document [%s]", i.input));
      return;
    }

    var xPathfactory = XPathFactory.newInstance();
    var xpath = xPathfactory.newXPath();

    try {
      var nameType = i.type.getType() + "name";
      var expression = format("(//%s)[%d]", nameType, itemN + 1);
      logger.info(format("expression [%s]", expression));
      var expr = xpath.compile(expression);
      var node = (Node) expr.evaluate(doc, XPathConstants.NODE);

      if(node.getParentNode().getNodeName().equals("controlaccess")) {
        logger.info(format("skip node in controlaccess [%s]", node.getTextContent()));
        return;
      }
      logger.info(format("text [%s]", node.getTextContent()));

      nodes.put(i.id, node);
    } catch (NullPointerException | XPathExpressionException e) {
      logger.error("Could not compile xpath");
    }
  }

  private void addNewControlAccessItem(ItemDto i) {
    /*
     * <controlaccess>
     *   <controlaccess>
     *     <persname encodinganalog="600$a" role="subject" source="NL-AMISG"
     * authfilenumber="123456">Bos, Dennis</persname>
     *   </controlaccess>
     * </controlaccess>
     */
    var node = nodes.get(i.id);
    if(node == null) {
      logger.warn(format("no node found for item [%s]", i.id));
      return;
    }
    var controlAccessParent = getParentByNames(node, newArrayList("archdesc", "descgrp"));
    if(controlAccessParent == null) {
      logger.error(format("no parent for new controlaccess element of item [%s]", i.input));
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

    logger.info("with new controlaccess elements: \n " + toString(doc));
  }

  private Document getDocument(Path eadPath) {
    var key = eadPath.toString();
    if(docs.containsKey(key)) {
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
    logger.info("get parent of node " + node.getNodeName());
    var parent = node.getParentNode();
    if(parent == null) {
      return null;
    } else if(names.contains(parent.getNodeName())) {
      return node;
    } else {
      return getParentByNames(parent, names);
    }
  }

  private static String toString(Document doc) {
    try {
      var sw = new StringWriter();
      var tf = TransformerFactory.newInstance();
      var transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.transform(new DOMSource(doc), new StreamResult(sw));
      return sw.toString();
    } catch (Exception ex) {
      throw new RuntimeException("Error converting to String", ex);
    }
  }
}

