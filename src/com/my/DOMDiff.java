package com.oracle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;


public class DOMDiff {


    private HashMap<String, String> getHashMap(NamedNodeMap nMap){

        HashMap<String, String> hMap = new HashMap<String,String>();

        if(nMap == null)
            return hMap;


        for(int i = 0; i<nMap.getLength(); i++){
            Node n = nMap.item(i);
            hMap.put(n.getNodeName(), n.getNodeValue());
        }

        return hMap;

    }


    private boolean matchStrings(String source, String target){

        if(source == null && target == null)
            return true;
        if(source == null)
            return false;
        if(target == null)
            return false;

        if(source.equals(target))
            return true;

        return false;
    }

    private int getNumMatches(Node source, Node target){

        int numMatches = 0;

        if(matchStrings(source.getNodeName(), target.getNodeName()))
            numMatches++;

        if(matchStrings(source.getNodeValue(), target.getNodeValue()))
            numMatches++;


        HashMap<String, String> sourceAttributesMap = getHashMap(source.getAttributes());
        HashMap<String, String> targetAttributesMap = getHashMap(target.getAttributes());

        for(String k: sourceAttributesMap.keySet()) {
            if(matchStrings(sourceAttributesMap.get(k), targetAttributesMap.get(k)))
                numMatches++;
        }

        return numMatches;

    }

    private boolean exactMatch(Node source, Node target){
        int numMatches = getNumMatches(source, target);
        int expectedSourceMatches = 2;

        if(source.getAttributes() != null)
            expectedSourceMatches += source.getAttributes().getLength();

        int expectedTargetMatches = 2;
        if(target.getAttributes() != null)
            expectedTargetMatches += target.getAttributes().getLength();

        if(expectedSourceMatches != expectedTargetMatches)
            return false;

        if(numMatches != expectedSourceMatches)
            return false;

        return true;
    }

    private void mark(Node source, Node target){

        String ignore = "false";
        if(exactMatch(source, target))
            ignore = "true";


        ((Element) source).setAttribute("ignore", ignore);

        if(ignore.equals("true"))
            process(source, target);
    }

    private void process(Node source, Node target){
        HashMap<Node, Node> reflection = new HashMap<Node, Node>();

        NodeList sourceChildren = source.getChildNodes();
        NodeList targetChildren = target.getChildNodes();

        for(int i = 0; i<sourceChildren.getLength(); i++){

            Node candidateSourceNode = sourceChildren.item(i);

            if(candidateSourceNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            int maxNumMatches = 0;
            Node matchingTargetNode = null;

            for(int j = 0; j<targetChildren.getLength(); j++) {

                Node candidateTargetNode = targetChildren.item(j);
                if(candidateTargetNode.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                int numMatches = getNumMatches(candidateSourceNode, candidateTargetNode);

                if(numMatches > maxNumMatches){
                    maxNumMatches = numMatches;
                    matchingTargetNode = candidateTargetNode;
                }

            }

            reflection.put(candidateSourceNode, matchingTargetNode);
        }


        for(Node sourceNode : reflection.keySet()){
            Node targetNode = reflection.get(sourceNode);
            mark(sourceNode, targetNode);
        }
    }

    public static void main(String args[]) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        String sourceFile = "/Users/cghai/Documents/Code/LRG_projects/source.xml";
        String targetFile = "/Users/cghai/Documents/Code/LRG_projects/target.xml";
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document sourceDoc = db.parse(new File(sourceFile));
            Document targetDoc = db.parse(new File(targetFile));


            Node sourceNode = sourceDoc.getDocumentElement();
            Node targetNode = targetDoc.getDocumentElement();


            DOMDiff d = new DOMDiff();
            d.process(sourceNode, targetNode);

            DOMSource source = new DOMSource(sourceNode);
            FileWriter writer = new FileWriter(new File("/Users/cghai/Documents/Code/LRG_projects/output.xml"));
            StreamResult result = new StreamResult(writer);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(source, result);


        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}