package ua.lokha.determinebestblacklistips;

import lombok.SneakyThrows;
import net.sf.saxon.z.IntHashSet;
import net.sf.saxon.z.IntRangeSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    @SuppressWarnings({"ResultOfMethodCallIgnored", "DuplicatedCode", "ConstantConditions", "StringConcatenationInsideStringBufferAppend"})
    public static void main(String[] args) throws Exception {
        File goodFile = new File("good-ips.txt");
        File badFile = new File("bad-ips.txt");
        File blacklistsDir = new File("blacklists");

        if (!blacklistsDir.exists()) {
            blacklistsDir.mkdir();
        }
        if (!goodFile.exists()) {
            goodFile.createNewFile();
        }
        if (!badFile.exists()) {
            badFile.createNewFile();
        }

        List<Integer> badIps = readToIpList(badFile);
        System.out.println("badNets " + badIps.size());

        List<Integer> goodIps = readToIpList(goodFile);
        goodIps.removeAll(badIps);
        System.out.println("goodNets " + goodIps.size());

        for (File blacklistFile : blacklistsDir.listFiles(File::isFile)) {
            StringBuilder log = new StringBuilder();
            log.append("\n" + "load " + blacklistFile.getName());
            IntRangeSet blackNets = new IntRangeSet();
            IntHashSet blackIps = new IntHashSet(8, 0);

            IntHashSet goodMatch = new IntHashSet(8, 0);
            IntHashSet badMatch = new IntHashSet(8, 0);

            try (LineIterator blackIterator = FileUtils.lineIterator(blacklistFile)) {
                while (blackIterator.hasNext()) {
                    String blackIpString = blackIterator.next();
                    if (!isValid(blackIpString)) {
                        continue;
                    }
                    int mask = 32;
                    if (blackIpString.contains("/")) {
                        String[] data = blackIpString.split("/");
                        mask = Integer.parseInt(data[1]);
                        blackIpString = data[0];
                    }
                    int blackIp = ip2int(blackIpString);
                    if (mask == 32) {
                        blackIps.add(blackIp);
                    } else {
                        int down = blackIp & (0xffffffff << (32 - mask));
                        int up = blackIp | (0xffffffff >>> (mask));
                        blackNets.addRange(down, up);
                    }
                }
            }

            log.append("\n" + "check bad " + blacklistFile.getName());
            int badCount = 0;
            for (Integer badIp : badIps) {
                if (blackIps.contains(badIp) || blackNets.contains(badIp)) {
                    badCount++;
                    badMatch.add(badIp);
                }
            }

            log.append("\n" + "check good " + blacklistFile.getName());
            int goodCount = 0;
            for (Integer goodIp : goodIps) {
                if (blackIps.contains(goodIp) || blackNets.contains(goodIp)) {
                    goodCount++;
                    goodMatch.add(goodIp);
                }
            }

            double badPercent = ((double) badCount / badIps.size()) * 100;
            log.append("\n" + "blacklist " + blacklistFile.getName() + ": \n" +
                    "  badCount " + badCount + " (" + String.format("%.2f", badPercent) + "%): " + IntStream.of(badMatch.getValues()).mapToObj(Main::int2ip).collect(Collectors.joining(", ")) + "\n" +
                    "  goodCount " + goodCount + " (" + String.format("%.2f", ((double)goodCount / goodIps.size()) * 100) + "%): " + IntStream.of(goodMatch.getValues()).mapToObj(Main::int2ip).collect(Collectors.joining(", ")));
            if (badPercent > 70) {
                System.out.println(log);
            }
        }
    }

    @SneakyThrows
    public static List<Integer> readToIpList(File file) {
        List<Integer> ips = new ArrayList<>();
        try (LineIterator iterator = FileUtils.lineIterator(file)) {
            while (iterator.hasNext()) {
                String ip = iterator.next();
                if (!isValid(ip)) {
                    continue;
                }
                ips.add(ip2int(ip));
            }
        }
        return ips;
    }

    private static Pattern netPattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+(/[0-9]+)?");

    public static boolean isValid(String ip) {
        return netPattern.matcher(ip).matches();
    }

    public static int ip2int(String ip) {
        String[] data = ip.split("\\.");
        return Integer.parseInt(data[0]) << 24 | Integer.parseInt(data[1]) << 16 | Integer.parseInt(data[2]) << 8 | Integer.parseInt(data[3]);
    }

    public static String int2ip(int ip) {
        return String.format("%d.%d.%d.%d",
                (ip >> 24 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 8 & 0xff),
                (ip & 0xff));
    }
}
