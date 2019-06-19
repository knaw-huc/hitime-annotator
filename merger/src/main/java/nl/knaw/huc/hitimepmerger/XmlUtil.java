package nl.knaw.huc.hitimepmerger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.lang.String.format;

public class XmlUtil {

  private static final Logger logger = LoggerFactory.getLogger(XmlUtil.class);

  public static Node getParentByNames(Node node, List<String> names) {
    var parent = node.getParentNode();
    if (parent == null) {
      return null;
    } else if (names.contains(parent.getNodeName())) {
      return parent;
    } else {
      return getParentByNames(parent, names);
    }
  }

  public static Element findChildNodeByTextContent(Node node, String textContent) {
    var childNodes = node.getChildNodes();
    for (var i = 0; i < childNodes.getLength(); i++) {
      var item = childNodes.item(i);
      if (item.getTextContent().trim().equals(textContent.trim())) {
        return (Element) item;
      }
    }
    return null;
  }

  public static Node findChildNodeByName(Node typeControlaccess, String name) {
    var childNodes = typeControlaccess.getChildNodes();
    for (var i = 0; i < childNodes.getLength(); i++) {
      var childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(name)) {
        return childNode;
      }
    }
    return null;
  }

  public static void toFile(Document doc, Path path) {
    try {
      var transformer = getTransformer();
      var writer = new FileWriter(path.toFile());
      var result = new StreamResult(writer);
      transformer.transform(new DOMSource(doc), result);
    } catch (TransformerException | IOException ex) {
      logger.error(format("Could not convert document to file [%s]: %s", path, ex.getMessage()));
    }
  }

  public static Transformer getTransformer() throws TransformerConfigurationException {
    var tf = TransformerFactory.newInstance();
    var transformer = tf.newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    return transformer;
  }

}
