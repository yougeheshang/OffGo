package org.tinkerhub.offgo.util;

import org.tinkerhub.offgo.entity.MapLocation;
import org.tinkerhub.offgo.entity.MapRoad;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class OsmParser {
    private Map<String, MapLocation> nodes = new HashMap<>();
    private List<MapRoad> ways = new ArrayList<>();

    public void parseOsmFile(String filePath) throws ParserConfigurationException, IOException, SAXException {
        File osmFile = new File(filePath);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(osmFile);
        doc.getDocumentElement().normalize();

        // 解析所有节点
        parseNodes(doc);
        // 解析所有道路
        parseWays(doc);
    }

    private void parseNodes(Document doc) {
        NodeList nodeList = doc.getElementsByTagName("node");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            String id = node.getAttribute("id");
            double lat = Double.parseDouble(node.getAttribute("lat"));
            double lon = Double.parseDouble(node.getAttribute("lon"));

            MapLocation location = new MapLocation();
            location.setId(Long.parseLong(id));
            location.setLatitude(lat);
            location.setLongitude(lon);

            // 解析标签信息
            NodeList tagList = node.getElementsByTagName("tag");
            for (int j = 0; j < tagList.getLength(); j++) {
                Element tag = (Element) tagList.item(j);
                String k = tag.getAttribute("k");
                String v = tag.getAttribute("v");

                switch (k) {
                    case "name":
                        location.setName(v);
                        break;
                    case "description":
                        location.setDescription(v);
                        break;
                    case "amenity":
                    case "building":
                    case "leisure":
                        location.setType(v);
                        break;
                }
            }

            // 所有节点都插入
                nodes.put(id, location);
        }
    }

    private void parseWays(Document doc) {
        NodeList wayList = doc.getElementsByTagName("way");
        for (int i = 0; i < wayList.getLength(); i++) {
            Element way = (Element) wayList.item(i);
            String id = way.getAttribute("id");
            
            // 检查是否是道路
            boolean isRoad = false;
            String roadType = null;
            NodeList tagList = way.getElementsByTagName("tag");
            for (int j = 0; j < tagList.getLength(); j++) {
                Element tag = (Element) tagList.item(j);
                String k = tag.getAttribute("k");
                String v = tag.getAttribute("v");

                if (k.equals("highway")) {
                    isRoad = true;
                    roadType = v;
                    break;
                }
            }

            if (isRoad) {
                MapRoad road = new MapRoad();
                road.setId(Long.parseLong(id));
                road.setRoadType(roadType);

                // 获取道路名称
                for (int j = 0; j < tagList.getLength(); j++) {
                    Element tag = (Element) tagList.item(j);
                    if (tag.getAttribute("k").equals("name")) {
                        road.setName(tag.getAttribute("v"));
                        break;
                    }
                }

                // 获取路径点
                NodeList ndList = way.getElementsByTagName("nd");
                if (ndList.getLength() > 0) {
                    Element firstNd = (Element) ndList.item(0);
                    Element lastNd = (Element) ndList.item(ndList.getLength() - 1);
                    road.setStartPointId(Long.parseLong(firstNd.getAttribute("ref")));
                    road.setEndPointId(Long.parseLong(lastNd.getAttribute("ref")));

                    // 构建路径点字符串
                    StringBuilder pathPoints = new StringBuilder();
                    for (int j = 0; j < ndList.getLength(); j++) {
                        Element nd = (Element) ndList.item(j);
                        String nodeId = nd.getAttribute("ref");
                        MapLocation node = nodes.get(nodeId);
                        if (node != null) {
                            pathPoints.append(node.getLatitude())
                                    .append(",")
                                    .append(node.getLongitude())
                                    .append(";");
                        }
                    }
                    road.setPathPoints(pathPoints.toString());
                }

                ways.add(road);
            }
        }
    }

    public List<MapLocation> getLocations() {
        return new ArrayList<>(nodes.values());
    }

    public List<MapRoad> getRoads() {
        return ways;
    }
} 