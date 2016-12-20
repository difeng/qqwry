/**
 * ip文件转化，将原有的数据根式转化为新的格式文件
 * 
 * 新格式的文件存储格式如下：
 * 0-3： 第一个索引位置
 * 4-7： 最后一个索引位置
 * 
 * 索引格式：
 * 4 byte| 3 byte          | 3 byte
 * ip    | country position| area position
 * 
 * 新格式解析类IPLocation，位于本包下
 */
package com.difeng.convert;