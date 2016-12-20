# qqwry
纯真IP地址库qqwry.dat解析代码
使用两种方式实现：
* 使用内存映射文件方式读取(位于com.difeng.qqwry1包下)
* 使用byte数组读取(位于com.difeng.qqwry2包下)

两种实现，使用方法一致:
```java
final IPLocation ipLocation = new IPLocation(filePath);
Location loc = ipl.fetchIPLocation("182.92.240.50");
System.out.printf("%s %s",loc.country,loc.area);
```
两种实现方式都不用重启就可实现升级qqwry.dat文件，只要用新文件将旧文件覆盖，程序会自动检查文件最后修改时间，若最后修改时间大于上一次时间，则重新加载数据文件，通过修改IPLocation类中的常量enableFileWatch=true来开启，默认是关闭的。

> ####建议  
不需要热升级IP数据库文件时，使用方式二，效率较高  
此外，使用两种方式都可以，效率上没有太大差别

### 新添转化功能，将原文件格式转换为新格式，新格式比较简单去掉了原来的重定向标志。
新格式如下：
```sh
+----------+
|   文件头       |  (8字节) 
+----------+
|   记录区       |  (不定长)
+----------+
|   索引区       |  (大小由文件头决定)
+----------+
文件头：
+------------------------------+-----------------------------+
| first index position(4 bytes)|last index position(4 bytes) | 
+------------------------------+-----------------------------+
记录区：
+------------------+----------+------------------+----------+-----
| country1(n bytes)|\0(1 byte)| country2(n bytes)|\0(1 byte)|...
+------------------+----------+------------------+----------+-----
+------------------+----------+------------------+----------+-----
| area1(n bytes)   |\0(1 byte)| area2(n bytes)   |\0(1 byte)|...
+------------------+----------+------------------+----------+-----
索引区：
+------------+-------------------------+------------------------+
|ip1(4 bytes) |country position(3 bytes)| area position(3 bytes)|...
+------------+-------------------------+------------------------+
```
转换方法：
```java
final IPFileConvertor convertor = new IPFileConvertor(IPFileConvertor.class.getResource("/qqwry.dat").getPath(),"./qqwry.dat");
convertor.convert();
```
新格式使用方法和之前一致，使用com.difeng.convert包下的解析类IPLocation解析即可。
