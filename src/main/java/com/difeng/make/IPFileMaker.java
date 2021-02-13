package com.difeng.make;

import com.difeng.convert.Location;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author difeng
 * @date 2021/2/13 18:50
 * @Description
 */
public class IPFileMaker {

    static final int IP_RECORD_LENGTH = 10;

    private int pos;

    private byte[] data;

    private File infile;

    private File outFile;


    private IPFileMaker(File infile,
                        File outFile) {
        this.infile = infile;
        this.outFile = outFile;
        data = new byte[1024 * 1024 * 1000];
        this.pos = 8;
    }

    private List<Location> prepareIpData() throws Exception {
        List<Location> records = new ArrayList<>();
        FileInputStream fis = new FileInputStream(infile);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));

        String line;
        while ((line = br.readLine()) != null) {
            Location record = new Location();
            String[] cols = line.split(",");
            if (cols.length == 3) {
                record.setIp(cols[0].trim());
                record.setCountry(cols[1].trim());
                record.setArea(cols[2].trim());
                records.add(record);
            }
        }
        br.close();
        return records;
    }

    private void handleIpData(List<Location> records) throws Exception {
        //1. ip排序
         records.sort((o1, o2) -> (int)(inet_pton(o1.ip) - inet_pton(o2.ip)));
        Map<String, Integer> contryPosMap = new HashMap<>();
        Map<String, Integer> areaPosMap = new HashMap<>();

        //2. contry去重
        List<String> contryList = records.stream().map(x -> x.getCountry().trim()).distinct().collect(Collectors.toList());

        //area去重
        List<String> areaList = records.stream().map(x -> x.getArea().trim()).distinct().collect(Collectors.toList());

        //数据区域开始
        //contry数据写入
        for (String contry : contryList) {
            contryPosMap.put(contry, this.pos);
            System.arraycopy(contry.getBytes("GBK"), 0, data, pos, contry.getBytes("GBK").length);
            pos += contry.getBytes("GBK").length;
            //write end char
            data[pos++] = '\0';
        }

        //地区数据写入
        for (String area : areaList) {
            areaPosMap.put(area, this.pos);
            System.arraycopy(area.getBytes("GBK"), 0, data, pos, area.getBytes("GBK").length);
            pos += area.getBytes("GBK").length;
            //write end char
            data[pos++] = '\0';
        }
        //索引区数据写入
        int indexStart = pos;
        for (Location record : records) {
            long ip = inet_pton(record.getIp());
            int contryIndex = contryPosMap.get(record.getCountry());
            int areaIndex = areaPosMap.get(record.getArea());

            ip = ip & 0xFFFFFFFF;
            data[pos++] = (byte) (ip & 0xFF);
            data[pos++] = (byte) (ip >> 8 & 0xFF);
            data[pos++] = (byte) (ip >> 16 & 0xFF);
            data[pos++] = (byte) (ip >> 24 & 0xFF);

            contryIndex = contryIndex & 0xFFFFFFFF;
            data[pos++] = (byte) (contryIndex & 0xFF);
            data[pos++] = (byte) (contryIndex >> 8 & 0xFF);
            data[pos++] = (byte) (contryIndex >> 16 & 0xFF);

            areaIndex = areaIndex & 0xFFFFFFFF;
            data[pos++] = (byte) (areaIndex & 0xFF);
            data[pos++] = (byte) (areaIndex >> 8 & 0xFF);
            data[pos++] = (byte) (areaIndex >> 16 & 0xFF);
        }
        int indexLast = pos - IP_RECORD_LENGTH;
        data[0] = (byte) (indexStart & 0xFF);
        data[1] = (byte) (indexStart >> 8 & 0xFF);
        data[2] = (byte) (indexStart >> 16 & 0xFF);
        data[3] = (byte) (indexStart >> 24 & 0xFF);

        //write the last ip index position
        data[4] = (byte) (indexLast & 0xFF);
        data[5] = (byte) (indexLast >> 8 & 0xFF);
        data[6] = (byte) (indexLast >> 16 & 0xFF);
        data[7] = (byte) (indexLast >> 24 & 0xFF);
    }

    public void make() throws Exception {
        List<Location> records = this.prepareIpData();
        this.handleIpData(records);
        this.genIpDatFile();
    }

    /**
     * @param ipStr
     * @Description:将“.”号分隔的字符串转换为long类型的数字，字节序例如: ip:182.92.240.48  16进制表示(B6.5C.F0.30)
     * 转换后ip的16进制表示:0xB65CF030
     * @return:long
     */
    private static long inet_pton(String ipStr) {
        if (ipStr == null) {
            throw new NullPointerException("ip不能为空");
        }
        String[] arr = ipStr.split("\\.");
        long ip = (Long.parseLong(arr[0]) & 0xFFL) << 24 & 0xFF000000L;
        ip |= (Long.parseLong(arr[1]) & 0xFFL) << 16 & 0xFF0000L;
        ip |= (Long.parseLong(arr[2]) & 0xFFL) << 8 & 0xFF00L;
        ip |= (Long.parseLong(arr[3]) & 0xFFL);
        return ip;
    }

    public void genIpDatFile() throws Exception {
        FileOutputStream fileOut;
        fileOut = new FileOutputStream(this.outFile);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(data, 0, pos);
        out.writeTo(fileOut);
        out.close();
        fileOut.close();
    }

    public static void main(String[] args) throws Exception {
        File infile = new File("./ip_data.txt");
        File outFile = new File("test.dat");
        IPFileMaker iPFileMaker = new IPFileMaker(
                infile,
                outFile
        );
        iPFileMaker.make();

    }
}
