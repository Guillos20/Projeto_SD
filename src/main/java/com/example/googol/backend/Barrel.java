package com.example.googol.backend;

import java.io.*;
import java.util.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

public class Barrel extends UnicastRemoteObject implements ClientRMI {
    private static final int MULTICAST_PORT = 12345;
    private static final String MULTICAST_GROUP = "224.0.0.1";
    static ConcurrentHashMap<String, HashSet<String>> index = new ConcurrentHashMap<>();
    private static final long serialVersionUID = 1L;

    public static void main(String[] args) {
        try {
            System.setProperty("java.rmi.server.hostname", "194.210.175.139");
            @SuppressWarnings("unused")
            Registry registry = LocateRegistry.createRegistry(7000);
            Barrel h = new Barrel();
            Naming.rebind("rmi://194.210.175.139:7000/Barrel", h);
            start();
            System.out.println("Barrel ready.");
            readIndexFromFile();

        } catch (RemoteException e) {
            System.err.println("Failed to start RMI registry:");
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

    }

    public Barrel() throws RemoteException {
        super();
    }

    @SuppressWarnings("deprecation")
    private static void start() {
        System.out.println("Barrel Ready");

        try {
            try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                socket.joinGroup(group);

                while (true) {

                    byte[] buffer = new byte[1000000000];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    byte[] ackData = "ACK".getBytes();
                    DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(),
                            packet.getPort());
                    socket.send(ackPacket);
                    byte[] data = packet.getData();
                    int length = packet.getLength();
                    Map<String, Object> pageInfo = deserializePageInfo(data, length);
                    addToIndex(pageInfo);

                }
            }
        } catch (IOException | ClassNotFoundException e) {

            e.printStackTrace();
        }
    }

    private static Map<String, Object> deserializePageInfo(byte[] data, int length)
            throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data, 0, length);
        ObjectInputStream ois = new ObjectInputStream(in);
        @SuppressWarnings("unchecked")
        Map<String, Object> pageInfo = (Map<String, Object>) ois.readObject();
        ois.close();
        return pageInfo;
    }

    @SuppressWarnings("unchecked")
    private static void addToIndex(Map<String, Object> pageInfo) {
        readIndexFromFile();
        String url = (String) pageInfo.get("url");
        String pageTitle = (String) pageInfo.get("pageTitle");
        String citation = (String) pageInfo.get("citation");
        List<String> Ligacoes = new ArrayList<>();

        List<String> words = (List<String>) pageInfo.get("words");
        List<String> links = (List<String>) pageInfo.get("links");

        for (HashSet<String> pages : index.values()) {
            for (String page : pages) {
                List<String> pageLinks = extractLinksFromEntry(page);
                for (String link : pageLinks) {
                    if (link.contains(url)) {
                        if (!Ligacoes.contains(extractUrlFromEntry(page))) {
                            Ligacoes.add(extractUrlFromEntry(page));
                        }

                    }
                }
            }
        }

        for (String word : words) {
            HashSet<String> Page = index.getOrDefault(word.toLowerCase(), new HashSet<String>());

            String urlEntry = "URL: " + url + "\nPage Title: " + pageTitle + "\nCitation: " + citation + "\nLigacoes: "
                    + Ligacoes + "\nLinks: " + links;
            HashSet<String> pageSet = index.get(word.toLowerCase());
            if (pageSet != null) {
                for (Iterator<String> iterator = pageSet.iterator(); iterator.hasNext();) {
                    String page = iterator.next();
                    String[] lines = page.split("\\r?\\n");
                    if (lines.length > 0 && lines[0].equals("URL: " + url)) {
                        iterator.remove();
                        break;
                    }
                }
            }
            Page.add(urlEntry);

            index.put(word.toLowerCase(), Page);
        }

        saveIndexToFile();
    }

    private static List<String> extractLinksFromEntry(String entry) {
        List<String> links = new ArrayList<>();
        String[] parts = entry.split("\n");

        for (String part : parts) {
            if (part.startsWith("Links: ")) {
                String linksSection = part.substring("Links: ".length());
                String[] linkArray = linksSection.split(", ");
                links.addAll(Arrays.asList(linkArray));
                break;
            }
        }

        return links;
    }

    private static String extractUrlFromEntry(String entry) {
        String[] parts = entry.split("\n");

        for (String part : parts) {
            if (part.startsWith("URL: ")) {
                return part.substring("URL: ".length());
            }
        }

        return null;
    }

    private static void saveIndexToFile() {

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("index.bin"))) {
            oos.writeObject(index);
            oos.flush();
            System.out.println("Index saved to index.bin");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({ "unchecked", "unused" })
    private static void readIndexFromFile() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("index.bin"))) {
            index = (ConcurrentHashMap<String, HashSet<String>>) ois.readObject();

            // Print index content
            System.out.println("Index loaded from index.bin:");
            for (Map.Entry<String, HashSet<String>> entry : index.entrySet()) {
                String word = entry.getKey();
                HashSet<String> Page = entry.getValue();

            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ArrayList<String> processString(String input) throws RemoteException {
        String[] words = input.split(" ");
        for (String word : words) {
            word = word.toLowerCase();
        }
        List<Map<String, Object>> pesquisa = new ArrayList<>();
        pesquisa = searchAsMap(words);

        String resultado = resultsToString(pesquisa);

        String[] parts = resultado.split("url: ");
        ArrayList<String> resultList = new ArrayList<>();

        for (int i = 1; i < parts.length; i++) {
            resultList.add("url: " + parts[i]);
        }

        Comparator<String> linkComparator = new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                int link1 = extractNumberOfLinks(s1);
                int link2 = extractNumberOfLinks(s2);
                return Integer.compare(link2, link1);
            }
        };

        Collections.sort(resultList, linkComparator);
        for (int i = 0; i < resultList.size(); i++) {
            String url = resultList.get(i);
            int lastIndex = url.lastIndexOf("Links");
            url = lastIndex >= 0 ? url.substring(0, lastIndex) : url;
            resultList.set(i, url);
        }

        return resultList;
    }

    public List<Map<String, Object>> searchAsMap(String[] words) {
        List<Map<String, Object>> results = new ArrayList<>();
        readIndexFromFile();
        ArrayList<String> addedURLs = new ArrayList<>();

        Set<String> commonUrls = null;

        for (String word : words) {
            HashSet<String> urls = index.get(word);
            if (urls != null) {
                if (commonUrls == null) {
                    commonUrls = new HashSet<>(urls);
                } else {
                    commonUrls.retainAll(urls);
                }
            } else {
                commonUrls = null;
                break;
            }
        }

        if (commonUrls != null) {
            for (String url : commonUrls) {
                if (!addedURLs.contains(url)) {
                    Map<String, Object> pageInfo = new HashMap<>();
                    pageInfo.put("url", url);
                    results.add(pageInfo);
                    addedURLs.add(url);
                }
            }
        }

        return results;
    }

    public String resultsToString(List<Map<String, Object>> results) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> pageInfo : results) {
            for (Map.Entry<String, Object> entry : pageInfo.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static int extractNumberOfLinks(String str) {
        String[] lines = str.split("\n");
        for (String line : lines) {
            if (line.startsWith("Ligacoes: ")) {
                line = line.substring("Ligacoes: ".length()).trim();
                if (line.equals("[]")) {
                    return 0;
                } else {
                    String[] ligacoesArray = line.substring(1, line.length() - 1).split(",");

                    return ligacoesArray.length;
                }
            }
        }
        return 0;
    }

    @Override
    public boolean isServerActive() throws RemoteException {
        return true;

    }

}