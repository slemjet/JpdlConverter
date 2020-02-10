package com.slemjet.jpdlconverter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JpdlToUmlConverter {
    private static final Logger logger = LoggerFactory.getLogger(JpdlToUmlConverter.class);
    public static final ImmutableList<String> START_NODES = ImmutableList.of("start", "custom", "decision", "end", "state");
    public static final ImmutableList<String> DATA_NODES = ImmutableList.of("handler", "transition");

    public void convertToUml(File inFile) {
        logger.info(String.format("Converting inFile %s to UML", inFile.getAbsolutePath()));

        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        HashMap<String, Node> nodes = Maps.newHashMap();
        try {
            XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(inFile));
            while (eventReader.hasNext()) {
                XMLEvent xmlEvent = eventReader.nextEvent();
                if (xmlEvent.isStartDocument() || xmlEvent.isEndDocument()) {
                    logger.info(String.format("Parsing %s of DOCUMENT ", xmlEvent.isStartDocument() ? "START" : "END"));
                } else if (xmlEvent.isStartElement()) {
                    Node node = processElement(eventReader, xmlEvent);
                    nodes.put(node.getName(), node);
                } else {
                    logger.info(String.format("Parsing OTHER element %s", xmlEvent.toString()));
                }
            }
        } catch (XMLStreamException | FileNotFoundException e) {
            logger.warn(e.getMessage());
        }
        logger.info(String.format("Nodes map successfully created %s ", nodes));

        logger.info(String.format("Populating state diagram source inFile %s ", nodes));

        String stateDiagram = populateStateSource(nodes);

        String outFile = "F:\\IDEA_Projects\\JpdlConverter\\src\\main\\java\\com\\slemjet\\jpdlconverter\\uml\\converted.puml";
        System.out.println("Writing to inFile: " + outFile);
        // Files.newBufferedWriter() uses UTF-8 encoding by default
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFile))) {
            writer.write(stateDiagram);
        } // the inFile will be automatically closed
        catch (IOException e) {
            logger.error(String.format("could not save file %s", nodes));
        }
        System.out.println(stateDiagram);

    }

    private String populateStateSource(HashMap<String, Node> nodes) {
        StringBuilder sb = new StringBuilder("@startuml").append(System.lineSeparator());
        sb.append("scale 600 width").append(System.lineSeparator());
        Node startNode = nodes.get("start");
        Node root = new Node();
        root.setName("[*]");
        sb.append(populateForNode(root, startNode, "", nodes));

        sb.append("End").append(" -> ").append("[*]").append(System.lineSeparator());
        sb.append("@enduml");
        return sb.toString();

    }

    private String populateForNode(Node from, Node to, String command, HashMap<String, Node> nodes) {
        if (to == null || StringUtils.isBlank(to.getName())) {
            return StringUtils.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        String concatenatedFromName = Stream.of(from.getName().split(" ")).map(StringUtils::capitalize).collect(Collectors.joining());
        String concatenatedToName = Stream.of(to.getName().split(" ")).map(StringUtils::capitalize).collect(Collectors.joining());
        command = StringUtils.isBlank(command) ? "none" : command;
        builder.append(concatenatedFromName).append(" --> ").append(concatenatedToName).append(" : ").append(command).append(System.lineSeparator());

        Set<Decision> decisions = to.getDecisions();

//        boolean addState = decisions.size() > 1;
//        if (addState)
//            builder.append("state ").append(concatenatedToName).append(" {").append(System.lineSeparator());

        for (Decision decision : decisions) {
            Node subNode = nodes.get(decision.getTo());
            builder.append(populateForNode(to, subNode, decision.getName(), nodes));
        }
//        if (addState)
//            builder.append("} ").append(System.lineSeparator());

        return builder.toString();
    }

    private Node processElement(XMLEventReader eventReader, XMLEvent xmlEvent) {

        StartElement startElement = xmlEvent.asStartElement();
        QName name = startElement.getName();
        String localPart = name.getLocalPart();
        logger.info(String.format("Parsing start element %s", localPart));

        Node node = new Node();
        if (StringUtils.isNotBlank(localPart) && START_NODES.contains(localPart)) {
            Attribute nodeClass = startElement.getAttributeByName(new QName("class"));
            node.setHandler(nodeClass != null ? nodeClass.getValue() : StringUtils.EMPTY);
            Attribute nodeName = startElement.getAttributeByName(new QName("name"));
            node.setName(nodeName != null ? nodeName.getValue() : localPart);
            node.setDecisions(Sets.newHashSet());
            while (eventReader.hasNext()) {
                try {
                    XMLEvent peek = eventReader.peek();
                    if (peek.isStartElement()) {
                        if (DATA_NODES.contains(peek.asStartElement().getName().getLocalPart())) {
                            StartElement attribute = eventReader.nextEvent().asStartElement();
                            String attributeName = attribute.getName().getLocalPart().trim();
                            switch (attributeName) {
                                case "handler":
                                    Attribute handlerTo = attribute.getAttributeByName(new QName("to"));
                                    node.setHandler(handlerTo != null ? handlerTo.getValue() : StringUtils.EMPTY);
                                    break;
                                case "transition":
                                    Attribute decisionName = attribute.getAttributeByName(new QName("name"));
                                    Attribute decisionTo = attribute.getAttributeByName(new QName("to"));
                                    Decision decision = new Decision(decisionName != null ? decisionName.getValue() : StringUtils.EMPTY, decisionTo != null ? decisionTo.getValue() : StringUtils.EMPTY);
                                    node.getDecisions().add(decision);
                                    break;
                            }
                        } else {
                            break;
                        }
                    } else {
                        eventReader.nextEvent();
                    }

                } catch (XMLStreamException e) {
                    logger.warn(e.getMessage());
                }
            }
            logger.info(String.format("Created element %s", node));
        }
        return node;
    }
}
