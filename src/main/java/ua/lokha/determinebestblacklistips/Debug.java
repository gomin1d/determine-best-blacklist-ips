package ua.lokha.determinebestblacklistips;

public class Debug {
    public static void main(String[] args) {
        String ipString = "3.0.0.0";
        int ip = ip2int(ipString);
        int mask = 16;

        System.out.println(int2ip(ip & (0xffffffff << (32 - mask))));
        System.out.println(int2ip(ip | (0xffffffff >>> (mask))));
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
