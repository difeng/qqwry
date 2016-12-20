package com.difeng.qqwry2;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.difeng.qqwry2.IPLocation;
import junit.framework.Assert;
import junit.framework.TestCase;
/**
 * @Description:TODO
 * @author:difeng
 * @date:2016年12月14日
 */
public class IPLocationTest extends TestCase {
	
    public void testThreadSafe() throws Exception {
    	final IPLocation ipl = new IPLocation(IPLocation.class.getResource("/qqwry.dat").getPath());
		int num = 4;
		ExecutorService es = Executors.newFixedThreadPool(num);
		long start = System.currentTimeMillis();
		for (int i = 0; i < num;i++) {
			es.execute(new Runnable(){
				final Random rd = new Random();
				public void run(){
					int n = 10000;
					for(int i = 0;i < n;i++){
						String ip = (rd.nextInt(253) + 1) + "." + rd.nextInt(255) + "." + rd.nextInt(255) + "." + (rd.nextInt(253) + 1);	
						ipl.fetchIPLocation(ip);
					}
				}
			});
		}
		es.shutdown();
		while(!es.isTerminated()){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println(System.currentTimeMillis() - start);
	}
    
    public void testLocation() throws Exception { 
    	final IPLocation ipLocation = new IPLocation(IPLocation.class.getResource("/qqwry.dat").getPath());
    	Location loc = ipLocation.fetchIPLocation("255.92.240.50");
    	System.out.println(loc);
    	Assert.assertNotNull(loc);
    }
}

