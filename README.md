# qqwry
纯真IP地址库qqwry.dat解析代码
使用两种方式实现：
* 使用内存映射文件方式读取
* 使用byte数组读取

两种实现，使用方法一致:
```java
final IPLocation ipLocation = new IPLocation(filePath);
Location loc = ipl.location("182.92.240.50");
System.out.printf("%s %s",loc.country,loc.area);
```
两种实现方式都不用重启就可实现升级qqwry.dat文件，只要用新文件将旧文件覆盖，程序会自动检查文件更新时间，从而重新加载加载，通过修改IPLocation类中的常量enableFileWatch=true来开启，默认是关闭的.