package ua.lokha.determinebestblacklistips;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class ProxyChecker {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String... args) throws Exception {
        File proxyFile = new File("proxy-ips.txt");

        if (!proxyFile.exists()) {
            proxyFile.createNewFile();
        }

        List<String> ips = new ArrayList<>();
        try (LineIterator iterator = FileUtils.lineIterator(proxyFile)) {
            while (iterator.hasNext()) {
                String ip = iterator.next();
                if (!isValid(ip)) {
                    continue;
                }
                ips.add(ip);
            }
        }

//        int[] proxyPorts = new int[]{1081, 3838, 8118, 83, 11337, 30588, 54321, 28643, 443, 8888, 3129, 8123, 9999, 1085, 6667, 3629, 8081, 53281, 999, 4153, 1080, 38801, 80, 5836, 3128, 4145, 8080};
        int[] proxyPorts = new int[]{443, 8081, 4153, 1080, 80, 4145, 8080};

        ExecutorService service = Executors.newFixedThreadPool(1000);
        Set<String> detectProxy = new HashSet<>();

        for (int proxyPort : proxyPorts) {
            System.out.print("check port " + proxyPort);
            Map<String, List<Future<Boolean>>> futureMap = new LinkedHashMap<>();
            for (String ip : ips) {
                if (detectProxy.contains(ip)) {
                    continue;
                }
                List<Future<Boolean>> futures = futureMap.computeIfAbsent(ip, s -> new ArrayList<>());
                futures.add(portIsOpen(service, ip, proxyPort, 2000));
            }
            AtomicInteger count = new AtomicInteger();
            futureMap.forEach((ip, futures) -> {
                boolean result = futures.stream()
                        .anyMatch(booleanFuture -> {
                            try {
                                return booleanFuture.get();
                            } catch (Exception e) {
                                return false;
                            }
                        });
                if (result) {
                    detectProxy.add(ip);
                    count.incrementAndGet();
                }
            });
            System.out.println(" count " + count.get());
        }

        System.out.println("result: ");
        for (String ip : ips) {
            System.out.println(" ip " + ip + " - " + detectProxy.contains(ip));
        }
        System.out.println("percent " + String.format("%.2f", ((double)detectProxy.size() / ips.size()) * 100) + "%");

        service.awaitTermination(2, TimeUnit.SECONDS);
        service.shutdown();
    }

    public static Future<Boolean> portIsOpen(ExecutorService service, String ip, int port, int timeout) {
        return service.submit(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), timeout);
                socket.close();
                return true;
            } catch (Exception ex) {
                return false;
            }
        });
    }

    private static Pattern ipPattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");

    public static boolean isValid(String ip) {
        return ipPattern.matcher(ip).matches();
    }

}
