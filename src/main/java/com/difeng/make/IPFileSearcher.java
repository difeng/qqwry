package com.difeng.make;

import com.difeng.convert.Location;
import java.io.*;

/**
 * @author difeng
 * @date 2021/2/13 18:50
 * @Description
 */
public class IPFileSearcher {

    private byte[] data;

    private long firstIndexOffset;

    private long lastIndexOffset;

    static final long IP_RECORD_LENGTH = 10;

    private long totalIndexCount;

    private String filePath;


    private File file;

    public IPFileSearcher(String filePath) throws Exception {
        this.filePath = filePath;
        this.file = new File(this.filePath);
        load();
    }

    private void load() throws Exception {
        ByteArrayOutputStream out = null;
        FileInputStream in = null;
        try {
            out = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            in = new FileInputStream(file);
            while (in.read(b) != -1) {
                out.write(b);
            }
            data = out.toByteArray();
            firstIndexOffset = read4ByteAsLong(0);
            lastIndexOffset = read4ByteAsLong(4);
            totalIndexCount = (lastIndexOffset - firstIndexOffset) / IP_RECORD_LENGTH + 1;
            in.close();
            out.close();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @Description:将“.”号分隔的字符串转换为long类型的数字，字节序例如:
     *   ip:182.92.240.48  16进制表示(B6.5C.F0.30)
     *   转换后ip的16进制表示:0xB65CF030
     * @param ipStr
     * @return:long
     */
    private static long inet_pton(String ipStr) {
        if(ipStr == null){
            throw new NullPointerException("ip不能为空");
        }
        String [] arr = ipStr.split("\\.");
        long ip = (Long.parseLong(arr[0])  & 0xFFL) << 24 & 0xFF000000L;
        ip |=  (Long.parseLong(arr[1])  & 0xFFL) << 16 & 0xFF0000L;
        ip |=  (Long.parseLong(arr[2])  & 0xFFL) << 8 & 0xFF00L;
        ip |=  (Long.parseLong(arr[3])  & 0xFFL);
        return ip;
    }


    private long read4ByteAsLong(final int offset) {
        long val = data[offset] & 0xFF;
        val |= (data[offset + 1] << 8L) & 0xFF00L;
        val |= (data[offset + 2] << 16L) & 0xFF0000L;
        val |= (data[offset + 3] << 24L) & 0xFF000000L;
        return val;
    }

    private long read3ByteAsLong(final int offset) {
        long val = data[offset] & 0xFFL;
        val |= (data[offset + 1] << 8) & 0xFF00L;
        val |= (data[offset + 2] << 16) & 0xFF0000L;
        return val;
    }

    private Location searchRecord(String ip) {
        long key = inet_pton(ip);
        long low = 1;
        long high = totalIndexCount;
        long mid;
        while (low <= high) {
            mid = (low + high) >>> 1;
            long indexIP = read4ByteAsLong((int) (firstIndexOffset + (mid - 1) * IP_RECORD_LENGTH));
            long indexIPNext = read4ByteAsLong((int) (firstIndexOffset + mid * IP_RECORD_LENGTH));
            if (indexIP <= key && key < indexIPNext) {
                long contryIndex = read3ByteAsLong((int) (firstIndexOffset + (mid - 1) * IP_RECORD_LENGTH + 4));
                long areaIndex = read3ByteAsLong((int) (firstIndexOffset + (mid - 1) * IP_RECORD_LENGTH + 4 + 3));
                Location loc = new Location();
                loc.setIp(ip);
                loc.setCountry(fetchRecord((int) contryIndex));
                loc.setArea(fetchRecord((int) areaIndex));
                return loc;
            } else {
                if (key > indexIP) {
                    low = mid + 1;
                } else if (key < indexIP) {
                    high = mid - 1;
                }
            }
        }
        return null;
    }

    private class MString {

        public final String string;

        public final int byteCount;

        public MString(final String string, final int byteCount) {
            this.string = string;
            this.byteCount = byteCount;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    private MString readString(final int offset) {
        int pos = offset;
        final byte[] b = new byte[256];
        int i;
        for (i = 0, b[i] = data[pos++]; b[i] != 0; b[++i] = data[pos++]) ;

        try {
            return new MString(new String(b, 0, i, "GBK"), i + 1);
        } catch (UnsupportedEncodingException e) {
            return new MString("", 0);
        }
    }


    private String fetchRecord(final int offset) {
        String ret;
        try {
            final MString country = readString(offset);
            ret = country.string;
            return ret;
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        IPFileSearcher iPFileSearcher  = new IPFileSearcher("./test.dat");
        System.out.println(iPFileSearcher.searchRecord("124.126.138.233"));
    }
}
