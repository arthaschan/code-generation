package com.lance.code.generation.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lance.code.generation.common.ConfigConstants;
import com.lance.code.generation.common.JavaBeanHandler;
import com.lance.code.generation.domain.ColumnInfo;
import com.lance.code.generation.domain.TableInfo;
import com.lance.code.generation.mapper.TablesMapper;

@Service
public class TableServiceImpl implements TableService {
	Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private TablesMapper tablesMapper;

	@Override
	public List<TableInfo> findAll(String schema) {
		return tablesMapper.findAll(schema);
	}
	
	public List<ColumnInfo> findColumn(String tableName, String schema) {
		return tablesMapper.findColumn(tableName, schema);
	}

	public void run() {
		List<TableInfo> list = findAll(ConfigConstants.SCHEMA);
		for(TableInfo info: list) {
			logger.info("tableName: {}, tableComment:{}", info.getTableName(), info.getTableComment());
			handlerTableColumn(info);
		}
	}
	
	/**
	 * 处理表字段
	 * @param info
	 * 2016年8月16日下午1:47:28
	 */
	private void handlerTableColumn(TableInfo info) {
		List<ColumnInfo> columns = findColumn(info.getTableName(), ConfigConstants.SCHEMA);
		
		//创建Domain
		String domain = JavaBeanHandler.createDomain(info, columns);
		String domainClassName=JavaBeanHandler.domainClassName(info.getTableName());
		// 只对满足表名前缀要求的表进行生成。如果前缀为空，则生成全部。
		if(StringUtils.isEmpty(domainClassName))
			return ;
		writeFile(JavaBeanHandler.domainPath(), domainClassName+".java", domain);
		
		//创建Mapper
		String mapper = JavaBeanHandler.createMapper(info);

		writeFile(JavaBeanHandler.mapperPath(), JavaBeanHandler.className(info.getTableName(), ConfigConstants.MAPPER_PACKAGE)+".java", mapper);
		
		//创建Mapper.xml
		String xmlMapper = JavaBeanHandler.createXMLMapper(info, columns);
		writeFile(JavaBeanHandler.xmlPath(), JavaBeanHandler.className(info.getTableName(), ConfigConstants.SQL_MAPPER_SUFFIX)+".xml", xmlMapper);
		
		//创建接口service
		String service = JavaBeanHandler.createService(info);
		writeFile(JavaBeanHandler.servicePath(), JavaBeanHandler.className(info.getTableName(), ConfigConstants.SERVICE_PACKAGE)+".java", service);
		
		//创建接口serviceImpl
		String serviceImpl = JavaBeanHandler.createServiceImpl(info);
		writeFile(JavaBeanHandler.servicePath(), JavaBeanHandler.className(info.getTableName(), ConfigConstants.SERVICE_impl_PACKAGE)+".java", serviceImpl);
	}
	
	/**
	 * 写文件
	 * @param dir
	 * @param fileName
	 * @param content
	 * 2016年8月16日下午2:50:55
	 */
	private void writeFile(String dir, String fileName, String content){
		File dic = new File(dir);
		if(!dic.exists()){
			dic.mkdirs();
		}
		
		try {
			Path file = Paths.get(dir+"/"+fileName);
			Files.deleteIfExists(file);
			file = Files.createFile(file);
			
			BufferedWriter writer = Files.newBufferedWriter(file, Charset.forName("utf-8"));
			writer.write(content);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
