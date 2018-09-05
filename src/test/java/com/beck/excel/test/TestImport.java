package com.beck.excel.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;
import org.springframework.util.ResourceUtils;

import com.smartwater.edm.ExcelUtil;
import com.smartwater.edm.entity.Alarm;

public class TestImport {


	static final String path_excel_2003 = "classpath:excel/alarm.xls";
	static final String path_excel_2007 = "classpath:excel/alarm.xlsx";
	
	@Test
	public void testImport() {
		
		try {
			ExcelUtil<Alarm> excelUtil = new ExcelUtil<>() ;

			File file =   ResourceUtils.getFile(path_excel_2003);
			
			List<Alarm> acfForms = excelUtil.importExcel("alarm", file , Alarm.class);
			
			for (Alarm a : acfForms) {
				System.out.println(a.toString());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	
}
