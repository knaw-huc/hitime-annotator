package nl.knaw.huc.hitimepmerger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.nio.file.Path;

public class XmlUtil {

  private static final DocumentBuilder builder;

  static {
    var factory = DocumentBuilderFactory.newInstance();
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException("Could not create xml document builder", e);
    }

  }

  public static Node getNode(
    Path path,
    String xpathExpression
  ) throws XPathExpressionException, IOException, SAXException {
    var document = getDocument(path);
    return getNode(document, xpathExpression);
  }

  private static Node getNode(Document doc, String xpathExpression) throws XPathExpressionException {
    var xPathfactory = XPathFactory.newInstance();
    var xpath = xPathfactory.newXPath();
    var expr = xpath.compile(xpathExpression);
    return (Node) expr.evaluate(doc, XPathConstants.NODE);
  }

  private static Document getDocument(Path path) throws IOException, SAXException {
    return builder.parse(path.toFile());
  }

}
