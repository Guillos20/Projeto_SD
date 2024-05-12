package com.example.googol.backend;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Random;

public class Gateway {
    private static Map<String, Integer> searchCounts = new HashMap<>();
    private static int totalCalls = 0;
    private static long totalDuration = 0;
    private static double averageDuration = 0;
    private static boolean isServer1Active;
    private static boolean isServer2Active;
    static int currentPage;
    static String stringInput;
    static long startTime;
    static int pageSize = 10;
    static URLQueue urlQueue = new URLQueue();
    private static int[] servers = new int[2];

    public static void start() {

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome to Gogool!");

            startThreads();

            while (true) {

                System.out.println("1. Input a URL");
                System.out.println("2. Input a string of words");
                System.out.println("3.Display System Information");
                String input = scanner.nextLine();
                if (input.equals("q")) {
                    break;
                }
                if (input.equals("1")) {

                    System.out.println("Please enter a URL:");
                    String stringInput = scanner.nextLine();
                    urlQueue.enqueue(stringInput);

                } else if (input.equals("2")) {
                    search();

                } else if (input.equals("3")) {
                    displaySystemInformation();
                } else {
                    System.out.println("Invalid choice! Please enter either '1','2' or '3'.");
                }
            }
        }
    }

    private static void search() {
        try (Scanner scanner = new Scanner(System.in)) {
            currentPage = 0;
            int selectedServer;
            int activeServerCount = 0;
            ClientRMI server1;
            ClientRMI server2;
            try {
                server1 = (ClientRMI) Naming.lookup("rmi://localhost:7000/Barrel");
                if (server1.isServerActive()) {
                    activeServerCount++;
                }
                try {
                    server2 = (ClientRMI) Naming.lookup("rmi://194.210.36.188:7000/Barrel");
                    if (server2.isServerActive()) {
                        activeServerCount++;
                    }
                    if (activeServerCount == 2) {
                        Random random = new Random();
                        int selectedIndex = random.nextInt(2);
                        selectedServer = servers[selectedIndex];
                        if (selectedServer == 1) {
                            System.out.println("Please enter a string of words:");
                            stringInput = scanner.nextLine();
                            searchCounts.put(stringInput, searchCounts.getOrDefault(stringInput, 0) + 1);
                            startTime = System.nanoTime();
                            ArrayList<String> response = server1.processString(stringInput);
                            processing(response);
                        }
                        if (selectedServer == 2) {
                            System.out.println("Please enter a string of words:");
                            stringInput = scanner.nextLine();
                            searchCounts.put(stringInput, searchCounts.getOrDefault(stringInput, 0) + 1);
                            startTime = System.nanoTime();
                            ArrayList<String> response = server2.processString(stringInput);
                            processing(response);
                        }
                    }
                } catch (Exception e) {
                    // System.out.println("Server Closed During Search");
                    search();
                }
                System.out.println("Please enter a string of words:");
                stringInput = scanner.nextLine();
                searchCounts.put(stringInput, searchCounts.getOrDefault(stringInput, 0) + 1);
                startTime = System.nanoTime();
                ArrayList<String> response = server1.processString(stringInput);
                processing(response);
            } catch (Exception e) {
                try {
                    server2 = (ClientRMI) Naming.lookup("rmi://194.210.36.188:7000/Barrel");
                    if (server2.isServerActive()) {
                        activeServerCount++;
                    }
                    System.out.println("Please enter a string of words:");
                    stringInput = scanner.nextLine();
                    searchCounts.put(stringInput, searchCounts.getOrDefault(stringInput, 0) + 1);
                    startTime = System.nanoTime();
                    ArrayList<String> response = server2.processString(stringInput);
                    processing(response);
                } catch (Exception ex) {
                    // System.out.println("No server Available. Retrying...");
                    search();
                }
            }
        }

    }

    private static void startThreads() {
        Thread heartbeatThread = new Thread(() -> {
            while (true) {
                try {
                    ClientRMI h = (ClientRMI) Naming.lookup("rmi://localhost:7000/Barrel");
                    while (true) {
                        Thread.sleep(5000);
                        isServer1Active = sendHeartbeat(h);
                    }
                } catch (RemoteException e) {
                    // System.out.println("No server Available. Retrying...");
                    while (true) {
                        try {
                            Thread.sleep(5000);
                            ClientRMI h = (ClientRMI) Naming.lookup("rmi://localhost:7000/Barrel");
                            isServer1Active = sendHeartbeat(h);
                            break;
                        } catch (Exception ex) {
                            // System.out.println("Retry failed. Exception occurred: " + ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    // System.out.println("Other exception occurred in heartbeat thread: " +
                    // e.getMessage());
                }
            }
        });
        heartbeatThread.start();
        Thread heartbeatThread2 = new Thread(() -> {
            while (true) {
                try {
                    ClientRMI h1 = (ClientRMI) Naming.lookup("rmi://192.168.1.129:7000/Barrel");
                    while (true) {
                        Thread.sleep(5000);
                        isServer2Active = sendHeartbeat(h1);
                    }
                } catch (RemoteException e) {
                    // System.out.println("RemoteException occurred in heartbeat thread: " +
                    // e.getMessage());

                    while (true) {
                        try {
                            Thread.sleep(5000);
                            ClientRMI h1 = (ClientRMI) Naming.lookup("rmi://192.168.1.129:7000/Barrel");
                            isServer2Active = sendHeartbeat(h1);

                            break;
                        } catch (Exception ex) {
                            // System.out.println("Retry failed. Exception occurred: " + ex.getMessage());

                        }
                    }
                } catch (Exception e) {
                    // System.out.println("Other exception occurred in heartbeat thread: " +
                    // e.getMessage());
                }
            }
        });
        heartbeatThread2.start();

        Thread downloaderThread1 = new Thread(() -> {
            while (true) {
                try {
                    String url = (String) urlQueue.dequeue();
                    Downloader.download(url, urlQueue);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread downloaderThread2 = new Thread(() -> {
            while (true) {
                try {
                    String url = (String) urlQueue.dequeue();
                    Downloader.download(url, urlQueue);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        downloaderThread1.start();
        downloaderThread2.start();
    }

    public static void displayPage(ArrayList<String> response, int currentPage, int pageSize) {
        int startIdx = currentPage * pageSize;
        int endIdx = Math.min((currentPage + 1) * pageSize, response.size());

        System.out.println(
                "Page " + (currentPage + 1) + " of " + (int) Math.ceil((double) response.size() / pageSize) + ":");

        for (int i = startIdx; i < endIdx; i++) {
            System.out.println(response.get(i));
        }
    }

    public static void displayLinks(ArrayList<String> ligacoes, int currentPage, int pageSize, String index) {
        if ((currentPage * pageSize) + Integer.parseInt(index) <= ligacoes.size()) {
            System.out.println(ligacoes.get((currentPage * pageSize) + Integer.parseInt(index) - 1));
        } else {
            System.out.println("Links not available for this index.");
        }

    }

    public static void displaySystemInformation() {
        displayMostCommonSearches();
        System.out.println("\nActive Barrels:");
        displayActiveBarrels();
        displayAverageResponseTime();
    }

    public static void displayMostCommonSearches() {

        List<Map.Entry<String, Integer>> sortedSearchCounts = new ArrayList<>(searchCounts.entrySet());
        sortedSearchCounts.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        System.out.println("Top 10 Most Searched Strings:");
        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedSearchCounts) {
            if (count >= 10) {
                break;
            }
            System.out.println(entry.getKey() + ": " + entry.getValue() + " times");
            count++;
        }
    }

    public static void displayActiveBarrels() {
        if (isServer1Active == true) {
            System.out.println("Barrel 1 Ativo");
        } else {
            System.out.println("Barrel Não Ativo");
        }
        if (isServer2Active == true) {
            System.out.println("Barrel 2 Ativo");
        } else {
            System.out.println("Barrel 2 Não Ativo");
        }
    }

    public static void displayAverageResponseTime() {

        System.out.println("Average duration of searching: " + averageDuration + " milliseconds\n");

    }

    public static boolean sendHeartbeat(ClientRMI h) {
        try {
            return h.isServerActive();
        } catch (RemoteException e) {
            return false;
        }
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
                    System.out.println(ligacoesArray.length);
                    return ligacoesArray.length;
                }
            }
        }
        return 0;
    }

    static Comparator<String> linkComparator = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            int link1 = extractNumberOfLinks(s1);
            int link2 = extractNumberOfLinks(s2);
            return Integer.compare(link2, link1);
        }
    };

    public static void processing(ArrayList<String> response) {
        try (Scanner scanner = new Scanner(System.in)) {
            ArrayList<String> linksLines = new ArrayList<>();
            for (int i = 0; i < response.size(); i++) {
                String url = response.get(i);
                int lastIndex = url.lastIndexOf("Ligacoes:");
                if (lastIndex >= 0) {
                    String linkLine = url.substring(lastIndex);
                    linksLines.add(linkLine);
                    url = url.substring(0, lastIndex);
                    response.set(i, url);
                }
            }
            long endTime = System.nanoTime();
            long durationInMillis = (endTime - startTime) / 1_000_000;
            totalDuration += durationInMillis;
            totalCalls++;
            averageDuration = (double) totalDuration / totalCalls;
            displayPage(response, currentPage, pageSize);
            while (true) {
                System.out.println("Enter 'n' for next page, 'p' for previous page, or 'q' to quit:");
                String indicator = scanner.nextLine();

                if (indicator.equalsIgnoreCase("n")) {
                    if ((currentPage + 1) * pageSize < response.size()) {
                        currentPage++;
                        displayPage(response, currentPage, pageSize);
                    } else {
                        System.out.println("No next page available.");
                    }
                } else if (indicator.equalsIgnoreCase("p")) {
                    if (currentPage > 0) {
                        currentPage--;
                        displayPage(response, currentPage, pageSize);
                    } else {
                        System.out.println("No previous page available.");
                    }
                } else if (indicator.equalsIgnoreCase("q")) {
                    System.out.println("Quitting...");
                    break;
                } else if (indicator.equalsIgnoreCase("1")) {
                    displayLinks(linksLines, currentPage, pageSize, indicator);
                } else if (indicator.equalsIgnoreCase("2")) {
                    displayLinks(linksLines, currentPage, pageSize, indicator);
                } else if (indicator.equalsIgnoreCase("3")) {
                    displayLinks(linksLines, currentPage, pageSize, indicator);
                } else if (indicator.equalsIgnoreCase("4")) {
                    displayLinks(linksLines, currentPage, pageSize, indicator);
                } else if (indicator.equalsIgnoreCase("5")) {
                    displayLinks(linksLines, currentPage, pageSize, indicator);
                } else if (indicator.equalsIgnoreCase("6")) {
                    displayLinks(linksLines, currentPage, pageSize, indicator);
                } else if (indicator.equalsIgnoreCase("7")) {
                    displayLinks(linksLines, currentPage, pageSize, indicator);
                } else if (indicator.equalsIgnoreCase("8")) {
                    displayLinks(linksLines, currentPage, pageSize, indicator);
                } else if (indicator.equalsIgnoreCase("9")) {
                    displayLinks(linksLines, currentPage, pageSize, indicator);
                } else if (indicator.equalsIgnoreCase("10")) {
                    displayLinks(linksLines, currentPage, pageSize, indicator);
                } else {
                    System.out.println("Invalid input. Please enter 'n', 'p', or 'q'.");
                }
            }
        }
    }

}
