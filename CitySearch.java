

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class CitySearch {

    static class Edge {
        String destination;
        int cost;

        public Edge(String destination, int cost) {
            this.destination = destination;
            this.cost = cost;
        }
    }

    static class PathNode {
        String city;
        List<String> path;
        int cost;

        public PathNode(String city, List<String> path, int cost) {
            this.city = city;
            this.path = path;
            this.cost = cost;
        }
    }

    static Map<String, List<Edge>> graph = new HashMap<>();

    public static void addEdge(String source, String destination, int cost) {
        graph.putIfAbsent(source, new ArrayList<>());
        graph.putIfAbsent(destination, new ArrayList<>());

        graph.get(source).add(new Edge(destination, cost));
        graph.get(destination).add(new Edge(source, cost)); // undirected graph
    }

    public static void loadGraphFromCSV(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false; // skip header
                    continue;
                }

                String[] parts = line.split(",");
                String city1 = parts[0].trim();
                String city2 = parts[1].trim();
                int cost = Integer.parseInt(parts[2].trim());

                addEdge(city1, city2, cost);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printGraph() {
        for (String city : graph.keySet()) {
            System.out.print(city + " -> ");
            for (Edge edge : graph.get(city)) {
                System.out.print("(" + edge.destination + ", " + edge.cost + ") ");
            }
            System.out.println();
        }
    }

    public static List<String> bfs(String start, String goal) {
        Queue<List<String>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        List<String> startPath = new ArrayList<>();
        startPath.add(start);

        queue.offer(startPath);
        visited.add(start);

        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String current = path.get(path.size() - 1);

            if (current.equals(goal)) {
                return path;
            }

            for (Edge edge : graph.get(current)) {
                String neighbor = edge.destination;

                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);

                    List<String> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);

                    queue.offer(newPath);
                }
            }
        }

        return null;
    }

    public static int calculatePathCost(List<String> path) {
        if (path == null || path.size() < 2) {
            return 0;
        }

        int totalCost = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            String current = path.get(i);
            String next = path.get(i + 1);

            for (Edge edge : graph.get(current)) {
                if (edge.destination.equals(next)) {
                    totalCost += edge.cost;
                    break;
                }
            }
        }

        return totalCost;
    }

    public static PathNode ucs(String start, String goal) {
        PriorityQueue<PathNode> pq = new PriorityQueue<>(Comparator.comparingInt(node -> node.cost));
        Map<String, Integer> minCost = new HashMap<>();
        Set<String> visited = new HashSet<>();

        List<String> startPath = new ArrayList<>();
        startPath.add(start);

        pq.offer(new PathNode(start, startPath, 0));
        minCost.put(start, 0);

        while (!pq.isEmpty()) {
            PathNode currentNode = pq.poll();
            String currentCity = currentNode.city;
            int currentCost = currentNode.cost;

            if (visited.contains(currentCity)) continue;
            visited.add(currentCity);

            if (currentCity.equals(goal)) {
                return currentNode;
            }

            for (Edge edge : graph.get(currentCity)) {
                String neighbor = edge.destination;
                int newCost = currentCost + edge.cost;

                if (!minCost.containsKey(neighbor) || newCost < minCost.get(neighbor)) {
                    minCost.put(neighbor, newCost);

                    List<String> newPath = new ArrayList<>(currentNode.path);
                    newPath.add(neighbor);

                    pq.offer(new PathNode(neighbor, newPath, newCost));
                }
            }
        }

        return null;
    }

    public static List<String> bidirectionalBFS(String start, String goal) {
        if (start.equals(goal)) {
            return Arrays.asList(start);
        }

        Queue<String> queueStart = new LinkedList<>();
        Queue<String> queueGoal = new LinkedList<>();

        Map<String, String> parentStart = new HashMap<>();
        Map<String, String> parentGoal = new HashMap<>();

        Set<String> visitedStart = new HashSet<>();
        Set<String> visitedGoal = new HashSet<>();

        queueStart.offer(start);
        queueGoal.offer(goal);

        visitedStart.add(start);
        visitedGoal.add(goal);

        parentStart.put(start, null);
        parentGoal.put(goal, null);

        while (!queueStart.isEmpty() && !queueGoal.isEmpty()) {
            String meetingNode = expandBFS(queueStart, visitedStart, visitedGoal, parentStart);
            if (meetingNode != null) {
                return buildPath(meetingNode, parentStart, parentGoal);
            }

            meetingNode = expandBFS(queueGoal, visitedGoal, visitedStart, parentGoal);
            if (meetingNode != null) {
                return buildPath(meetingNode, parentStart, parentGoal);
            }
        }

        return null;
    }

    public static String expandBFS(Queue<String> queue,
                                   Set<String> visitedThisSide,
                                   Set<String> visitedOtherSide,
                                   Map<String, String> parent) {
        if (queue.isEmpty()) {
            return null;
        }

        String current = queue.poll();

        for (Edge edge : graph.get(current)) {
            String neighbor = edge.destination;

            if (!visitedThisSide.contains(neighbor)) {
                visitedThisSide.add(neighbor);
                parent.put(neighbor, current);
                queue.offer(neighbor);

                if (visitedOtherSide.contains(neighbor)) {
                    return neighbor;
                }
            }
        }

        return null;
    }

    public static List<String> buildPath(String meetingNode,
                                         Map<String, String> parentStart,
                                         Map<String, String> parentGoal) {
        List<String> pathFromStart = new ArrayList<>();
        String current = meetingNode;

        while (current != null) {
            pathFromStart.add(current);
            current = parentStart.get(current);
        }
        Collections.reverse(pathFromStart);

        List<String> pathToGoal = new ArrayList<>();
        current = parentGoal.get(meetingNode);

        while (current != null) {
            pathToGoal.add(current);
            current = parentGoal.get(current);
        }

        pathFromStart.addAll(pathToGoal);
        return pathFromStart;
    }

    public static PathNode bidirectionalUCS(String start, String goal) {
        PriorityQueue<PathNode> pqStart = new PriorityQueue<>(Comparator.comparingInt(node -> node.cost));
        PriorityQueue<PathNode> pqGoal = new PriorityQueue<>(Comparator.comparingInt(node -> node.cost));

        Map<String, Integer> costStart = new HashMap<>();
        Map<String, Integer> costGoal = new HashMap<>();

        Map<String, String> parentStart = new HashMap<>();
        Map<String, String> parentGoal = new HashMap<>();

        pqStart.offer(new PathNode(start, new ArrayList<>(Arrays.asList(start)), 0));
        pqGoal.offer(new PathNode(goal, new ArrayList<>(Arrays.asList(goal)), 0));

        costStart.put(start, 0);
        costGoal.put(goal, 0);

        parentStart.put(start, null);
        parentGoal.put(goal, null);

        String meetingNode = null;
        int bestCost = Integer.MAX_VALUE;

        while (!pqStart.isEmpty() && !pqGoal.isEmpty()) {
            meetingNode = expandUCS(pqStart, costStart, costGoal, parentStart);
            if (meetingNode != null) {
                int totalCost = costStart.get(meetingNode) + costGoal.get(meetingNode);
                if (totalCost < bestCost) {
                    bestCost = totalCost;
                }
            }

            meetingNode = expandUCS(pqGoal, costGoal, costStart, parentGoal);
            if (meetingNode != null) {
                int totalCost = costStart.get(meetingNode) + costGoal.get(meetingNode);
                if (totalCost < bestCost) {
                    bestCost = totalCost;
                }
            }
        }

        if (bestCost == Integer.MAX_VALUE) {
            return null;
        }

        String bestMeetingNode = null;
        for (String city : costStart.keySet()) {
            if (costGoal.containsKey(city)) {
                int total = costStart.get(city) + costGoal.get(city);
                if (total == bestCost) {
                    bestMeetingNode = city;
                    break;
                }
            }
        }

        List<String> fullPath = buildPath(bestMeetingNode, parentStart, parentGoal);
        return new PathNode(bestMeetingNode, fullPath, bestCost);
    }

    public static String expandUCS(PriorityQueue<PathNode> pq,
                                   Map<String, Integer> thisCost,
                                   Map<String, Integer> otherCost,
                                   Map<String, String> parent) {
        if (pq.isEmpty()) {
            return null;
        }

        PathNode currentNode = pq.poll();
        String currentCity = currentNode.city;
        int currentCost = currentNode.cost;

        for (Edge edge : graph.get(currentCity)) {
            String neighbor = edge.destination;
            int newCost = currentCost + edge.cost;

            if (!thisCost.containsKey(neighbor) || newCost < thisCost.get(neighbor)) {
                thisCost.put(neighbor, newCost);
                parent.put(neighbor, currentCity);

                List<String> newPath = new ArrayList<>(currentNode.path);
                newPath.add(neighbor);

                pq.offer(new PathNode(neighbor, newPath, newCost));
            }
        }

        if (otherCost.containsKey(currentCity)) {
            return currentCity;
        }

        return null;
    }
    
    public static void visualizeGraph(String outputFile, String startCity, String goalCity) {

        int width = 1500;
        int height = 1000;
        int nodeRadius = 24;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(248, 250, 252));
        g.fillRect(0, 0, width, height);

        Map<String, Point> positions = new HashMap<>();

        // ===== Layered Layout =====
        // Layer 1
        positions.put("S", new Point(120, 500));

        // Layer 2
        positions.put("A", new Point(350, 300));
        positions.put("B", new Point(350, 500));
        positions.put("C", new Point(350, 700));
        positions.put("I", new Point(350, 850));

        // Layer 3
        positions.put("D", new Point(600, 200));
        positions.put("E", new Point(600, 350));
        positions.put("F", new Point(600, 500));
        positions.put("H", new Point(600, 600));
        positions.put("J", new Point(600, 760));
        positions.put("K", new Point(550, 920));
        positions.put("R", new Point(650, 730));

        // Layer 4
        positions.put("L", new Point(880, 140));
        positions.put("M", new Point(880, 50));
        positions.put("N", new Point(880, 320));
        positions.put("O", new Point(880, 480));
        positions.put("P", new Point(880, 640));
        positions.put("Q", new Point(880, 760));
        positions.put("T", new Point(880, 950));
        positions.put("U", new Point(880, 850));
        positions.put("V", new Point(880, 240));

        // Layer 5
        positions.put("W", new Point(1150, 560));
        positions.put("X", new Point(1150, 200));
        positions.put("Y", new Point(1150, 650));
        positions.put("Z", new Point(1150, 420));

        // Layer 6
        positions.put("G", new Point(1350, 500));

        Set<String> allCities = new HashSet<>(graph.keySet());
        List<String> cities = new ArrayList<>(allCities);
        Collections.sort(cities);

        // ===== Draw edges =====
        Set<String> drawnEdges = new HashSet<>();

        for (String from : cities) {

            for (Edge edge : graph.getOrDefault(from, new ArrayList<>())) {

                String to = edge.destination;

                String key1 = from + "-" + to;
                String key2 = to + "-" + from;

                if (drawnEdges.contains(key1) || drawnEdges.contains(key2))
                    continue;

                drawnEdges.add(key1);

                Point p1 = positions.get(from);
                Point p2 = positions.get(to);

                if (p1 == null || p2 == null)
                    continue;

                int dx = p2.x - p1.x;
                int dy = p2.y - p1.y;

                double length = Math.sqrt(dx * dx + dy * dy);
                if (length == 0)
                    continue;

                int x1 = (int) (p1.x + (dx / length) * nodeRadius);
                int y1 = (int) (p1.y + (dy / length) * nodeRadius);

                int x2 = (int) (p2.x - (dx / length) * nodeRadius);
                int y2 = (int) (p2.y - (dy / length) * nodeRadius);

                g.setColor(new Color(170, 180, 190));
                g.setStroke(new BasicStroke(2f));
                g.drawLine(x1, y1, x2, y2);

                int midX = (x1 + x2) / 2;
                int midY = (y1 + y2) / 2;

                int offsetX = (int) (-dy / length * 18);
                int offsetY = (int) (dx / length * 18);

                String weight = String.valueOf(edge.cost);

                g.setFont(new Font("Arial", Font.BOLD, 18));
                FontMetrics fm = g.getFontMetrics();

                int w = fm.stringWidth(weight);
                int h = fm.getAscent();

                int labelX = midX + offsetX;
                int labelY = midY + offsetY;

                g.setColor(new Color(255, 255, 255, 240));
                g.fillRoundRect(labelX - w / 2 - 6, labelY - h + 4, w + 12, h + 10, 12, 12);

                g.setColor(new Color(200, 210, 220));
                g.drawRoundRect(labelX - w / 2 - 6, labelY - h + 4, w + 12, h + 10, 12, 12);

                g.setColor(new Color(30, 41, 59));
                g.drawString(weight, labelX - w / 2, labelY + 4);
            }
        }

        // ===== Draw nodes =====
        for (String city : cities) {

            Point p = positions.get(city);
            if (p == null)
                continue;

            if (city.equals(startCity)) {
                g.setColor(new Color(20, 184, 166)); // teal
            } else if (city.equals(goalCity)) {
                g.setColor(new Color(244, 63, 94)); // rose
            } else {
                g.setColor(new Color(79, 70, 229)); // indigo
            }

            g.fillOval(p.x - nodeRadius, p.y - nodeRadius, nodeRadius * 2, nodeRadius * 2);

            g.setColor(new Color(30, 41, 59));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(p.x - nodeRadius, p.y - nodeRadius, nodeRadius * 2, nodeRadius * 2);

            if (city.equals(startCity) || city.equals(goalCity)) {
                g.drawOval(p.x - nodeRadius - 5, p.y - nodeRadius - 5,
                        (nodeRadius + 5) * 2, (nodeRadius + 5) * 2);
            }

            g.setFont(new Font("Arial", Font.BOLD, 15));
            FontMetrics fm = g.getFontMetrics();

            int textW = fm.stringWidth(city);
            int textH = fm.getAscent();

            g.setColor(Color.WHITE);
            g.drawString(city, p.x - textW / 2, p.y + textH / 4);
        }

        g.dispose();

        try {
            ImageIO.write(image, "png", new File(outputFile));
            System.out.println("Visualization saved as: " + outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
       
    
    public static boolean isEdgeInPath(String from, String to, List<String> path) {
        if (path == null || path.size() < 2) return false;

        for (int i = 0; i < path.size() - 1; i++) {
            String a = path.get(i);
            String b = path.get(i + 1);

            if ((a.equals(from) && b.equals(to)) || (a.equals(to) && b.equals(from))) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        loadGraphFromCSV("map_data.csv");

        String start = "S";
        String goal = "G";

        long bfsStartTime = System.nanoTime();
        List<String> bfsPath = bfs(start, goal);
        long bfsEndTime = System.nanoTime();

        long ucsStartTime = System.nanoTime();
        PathNode ucsResult = ucs(start, goal);
        long ucsEndTime = System.nanoTime();

        long biBfsStartTime = System.nanoTime();
        List<String> biBfsPath = bidirectionalBFS(start, goal);
        long biBfsEndTime = System.nanoTime();

        long biUcsStartTime = System.nanoTime();
        PathNode biUcsResult = bidirectionalUCS(start, goal);
        long biUcsEndTime = System.nanoTime();

        System.out.println("===== Part 1 =====");
        System.out.println("BFS Path: " + bfsPath);
        System.out.println("BFS Cost: " + calculatePathCost(bfsPath));
        System.out.println("BFS Runtime: " + (bfsEndTime - bfsStartTime) + " ns");

        System.out.println();

        System.out.println("UCS Path: " + ucsResult.path);
        System.out.println("UCS Cost: " + ucsResult.cost);
        System.out.println("UCS Runtime: " + (ucsEndTime - ucsStartTime) + " ns");

        System.out.println();
        System.out.println("===== Part 2 =====");

        System.out.println("Bidirectional BFS Path: " + biBfsPath);
        System.out.println("Bidirectional BFS Cost: " + calculatePathCost(biBfsPath));
        System.out.println("Bidirectional BFS Runtime: " + (biBfsEndTime - biBfsStartTime) + " ns");

        System.out.println();

        System.out.println("Bidirectional UCS Path: " + biUcsResult.path);
        System.out.println("Bidirectional UCS Cost: " + biUcsResult.cost);
        System.out.println("Bidirectional UCS Runtime: " + (biUcsEndTime - biUcsStartTime) + " ns");
        
        visualizeGraph("city_map.png", start, goal);
    }
}
