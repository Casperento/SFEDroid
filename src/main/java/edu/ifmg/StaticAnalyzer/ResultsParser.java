package edu.ifmg.StaticAnalyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ResultsParser is a class for handling FlowDroid's Taint Analysis results. So
 * it parses the results generated in the XML format.
 *
 * @author Casperento
 *
 */
public class ResultsParser {
    private static final Logger logger = LoggerFactory.getLogger(ResultsParser.class);
    private static ResultsParser instance;
    private List<String> sinksMethodsSigs = new ArrayList<>();

    public static ResultsParser getInstance() {
        if (ResultsParser.instance == null) {
            ResultsParser.instance = new ResultsParser();
        }
        return instance;
    }

    public void parse(File inputFile) {
        sinksMethodsSigs.clear();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeName().equals("Results")) {
                    NodeList resultsList = node.getChildNodes();
                    for (int j = 0; j < resultsList.getLength(); j++) {
                        Node result = resultsList.item(j);
                        if (result.getNodeName().equals("Result")) {
                            NodeList sinksList = result.getChildNodes();
                            for (int k = 0; k < sinksList.getLength(); k++) {
                                Node sink = sinksList.item(k);
                                if (sink.getNodeName().equals("Sink")) {
                                    sinksMethodsSigs.add(sink.getAttributes().getNamedItem("MethodSourceSinkDefinition").getTextContent());
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public List<String> getSinksMethodsSigs() {
        return sinksMethodsSigs;
    }
}
