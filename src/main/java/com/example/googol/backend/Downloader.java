package com.example.googol.backend;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Downloader {

    private static final String MULTICAST_GROUP = "224.0.0.1";
    private static final int MULTICAST_PORT = 12345;
    static ConcurrentHashMap<String, HashSet<String>> index = new ConcurrentHashMap<>();
    static HashSet<String> visitedUrls = new HashSet<>();

    public static void download(String url, URLQueue urlQueue) {

        try (MulticastSocket socket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            visitedUrls.add(url);
            try {
                Document doc = Jsoup.connect(url).get();
                String title = doc.title();
                String text = doc.body().text();
                List<String> words = Arrays.asList(text.split("\\W+"));

                // System.out.println("URL: " + url);
                // System.out.println("Title: " + title);
                // System.out.println("Text snippet: " + text.substring(0, Math.min(100,
                // text.length())) + "...");

                Elements links = doc.select("a[href]");
                int numberOfLinks = links.size();

                List<String> linkList = new ArrayList<>();
                for (Element link : links) {
                    String newUrl = link.attr("abs:href");
                    if (!visitedUrls.contains(newUrl)) {
                        urlQueue.enqueue(newUrl);
                    }
                    linkList.add(newUrl);
                }
                ConcurrentHashMap<String, Object> pageInfo = new ConcurrentHashMap<>();
                pageInfo.put("url", url);
                pageInfo.put("pageTitle", title);
                pageInfo.put("citation", text.substring(0, Math.min(100, text.length())) + "...");
                pageInfo.put("words", words);
                pageInfo.put("numberOfLinks", numberOfLinks);
                pageInfo.put("links", linkList);

                byte[] data = serializePageInfo(pageInfo);

                DatagramPacket packet = new DatagramPacket(data, data.length, group, MULTICAST_PORT);
                socket.send(packet);

            } catch (IOException e) {
                // System.err.println("Too much data from: " + url);
            } catch (IllegalArgumentException e) {
                // System.err.println("Wrong Url Format: " + url);
            }
        } catch (IOException e) {
            System.err.println("Servidor Nao recebeu: " + url);
            download(url, urlQueue);
            e.printStackTrace();
        }
    }

    private static byte[] serializePageInfo(Map<String, Object> pageInfo) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(pageInfo);
        oos.flush();
        byte[] data = baos.toByteArray();
        oos.close();
        return data;
    }

}