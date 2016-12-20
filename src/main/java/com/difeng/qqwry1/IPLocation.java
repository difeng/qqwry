package com.difeng.qqwry1;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
/**
 * @Description:ip定位查找工具(使用内存映射文件方式读取，线程安全)
 * @author:difeng
 * @date:2016年12月11日
 */
public class IPLocation {

	private static final int IP_RECORD_LENGTH = 7;

	private static final byte REDIRECT_MODE_1 = 0x01;

	private static final byte REDIRECT_MODE_2 = 0x02;

	private MappedByteBuffer mbbFile;

	private static Long lastModifyTime = 0L;

	public static boolean enableFileWatch = false;

	private static ReentrantLock lock = new ReentrantLock();

	private File qqwryFile;

	private long firstIndexOffset;

	private long lastIndexOffset;

	private long totalIndexCount;

	public IPLocation(String filePath) throws Exception {
		this.qqwryFile = new File(filePath);
		load();
		if (enableFileWatch) {
			watch();
		}
	}

	private void watch(){
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				long time = qqwryFile.lastModified();
				if (time > lastModifyTime) {
					lastModifyTime = time;
					try {
						load();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}, 1000L, 30000L, TimeUnit.MILLISECONDS);
	}

	public long read4ByteAsLong(long pos) {
		mbbFile.position((int)pos);
		return 0xFFFFFFFFL & mbbFile.getInt();
	}

	public long read3ByteAsLong(long pos){
		mbbFile.position((int)pos);
		return 0xFFFFFFL & mbbFile.getInt();
	}


	@SuppressWarnings("resource")
	private void load() throws Exception {
		lastModifyTime = qqwryFile.lastModified();
		lock.lock();
		try {
			mbbFile =  new RandomAccessFile(qqwryFile, "r")
					.getChannel()
					.map(FileChannel.MapMode.READ_ONLY, 0, qqwryFile.length());
			mbbFile.order(ByteOrder.LITTLE_ENDIAN);
			firstIndexOffset = read4ByteAsLong(0);
			lastIndexOffset = read4ByteAsLong(4);
			totalIndexCount = (lastIndexOffset - firstIndexOffset) / IP_RECORD_LENGTH + 1;
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * @Description:将“.”号分隔的字符串转换为long类型的数字，字节序例如:
	 *   ip:182.92.240.48  16进制表示(B6.5C.F0.30)
	 *   转换后ip的16进制表示:0xB65CF030
	 * @param ipStr
	 * @return
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

	private long search(long ip) {
		long low = 0;
		long high = totalIndexCount;
		long mid = 0;
		while(low <= high) {
			mid = (low + high) >>> 1 ;
		    long indexIP = read4ByteAsLong(firstIndexOffset + (mid - 1) * IP_RECORD_LENGTH);
		    long nextIndexIP =  read4ByteAsLong(firstIndexOffset + mid * IP_RECORD_LENGTH);
		    if(indexIP <= ip && ip < nextIndexIP) {
		    	return read3ByteAsLong(firstIndexOffset + (mid - 1) * IP_RECORD_LENGTH + 4);
		    } else {
		    	if(ip > indexIP) {
		    		low = mid + 1;
		    	} else if(ip < indexIP) {
		    		high = mid - 1;
		    	}
		    }
		}
		return -1;
	}

	private Location readIPLocation(long offset) {
		try {
			mbbFile.position((int)offset + 4);
			Location loc = new Location();
			byte redirectMode = mbbFile.get();
			if (redirectMode == REDIRECT_MODE_1) {
				long countryOffset = read3ByteAsLong((int)offset + 5);
				mbbFile.position((int)countryOffset);
				redirectMode = mbbFile.get();
				if (redirectMode == REDIRECT_MODE_2) {
					loc.country = readString(read3ByteAsLong(countryOffset + 1));
					mbbFile.position((int)countryOffset + 4);
				} else {
					loc.country = readString(countryOffset);
				}
				loc.area = readArea(mbbFile.position());
			} else if (redirectMode == REDIRECT_MODE_2) {
				loc.country = readString(read3ByteAsLong((int)offset + 5));
				loc.area = readArea((int)offset + 8);
			} else {
				loc.country = readString(mbbFile.position() - 1);
				loc.area = readArea(mbbFile.position());
			}
			return loc;
		} catch (Exception e) {
			return null;
		}
	}

	private String readArea(int offset) {
		mbbFile.position(offset);
		byte redirectMode = mbbFile.get();
		if (redirectMode == REDIRECT_MODE_1 || redirectMode == REDIRECT_MODE_2) {
			long areaOffset = read3ByteAsLong((int)offset + 1);
			if (areaOffset == 0){
				return "";
			} else {
				return readString(areaOffset);
			}
		} else {
			return readString(offset);
		}
	}

	private String readString(long offset) {
		try {
			mbbFile.position((int)offset);
			byte[] buf = new byte[128];
			int i;
			for (i = 0, buf[i] = mbbFile.get(); buf[i] != 0; buf[++i] = mbbFile.get());
			
			if (i != 0){
			    return new String(buf, 0, i, "GBK");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public  Location fetchIPLocation(String ip) {
		lock.lock();
		try {
			long offset = search(inet_pton(ip));
			if(offset != -1){
				return readIPLocation(offset);
			}
		} finally {
			lock.unlock();
		}
		return null;
	}
}

