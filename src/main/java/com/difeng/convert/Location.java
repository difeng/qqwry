package com.difeng.convert;

import java.io.UnsupportedEncodingException;

/**
 * @Description:ip位置
 * @author:difeng
 * @date:2016年12月13日
 */
public class Location {
	
	public String ip;
	
    public String country;
    
    public String area;
    
	@Override
	public String toString() {
		return "Location [country=" + country + ", area=" + area + "]";
	}
	
	public void changeEnCode() throws UnsupportedEncodingException {
		this.country = new String(this.country.trim().getBytes(),"GBK");
		this.area = new String(this.area.trim().getBytes(),"GBK");
	}
    
}

