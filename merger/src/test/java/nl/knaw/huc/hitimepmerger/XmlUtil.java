package nl.knaw.huc.hitimepmerger;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.nio.file.Path;

import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.STRING;

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

  public static Object evaluate(
    Path path,
    String xpathExpression
  ) throws XPathExpressionException, IOException, SAXException {
    return evaluate(path, xpathExpression, STRING);
  }

  public static Object evaluate(
    Path path,
    String xpathExpression,
    QName qname
  ) throws XPathExpressionException, IOException, SAXException {
    var document = getDocument(path);
    return evaluate(document, xpathExpression, qname);
  }

  private static Object evaluate(Document doc, String xpathExpression, QName qname) throws XPathExpressionException {
    var xPathfactory = XPathFactory.newInstance();
    var xpath = xPathfactory.newXPath();
    var expr = xpath.compile(xpathExpression);
    return expr.evaluate(doc, qname);
  }

  private static Document getDocument(Path path) throws IOException, SAXException {
    return builder.parse(path.toFile());
  }

}
