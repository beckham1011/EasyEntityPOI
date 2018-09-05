package com.smartwater.edm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ExcelUtil<T> {

	private Workbook workbook;

	public List<T> importExcel(String sheetName, File f, Class<T> item) {
		List<T> list = null;

		try {
			//支持2003/2007/2010版本
			workbook = WorkbookFactory.create(new FileInputStream(f));
			
			Sheet sheet = workbook.getSheet(sheetName);
			if (!"".equals(sheetName.trim())) {
				sheet = workbook.getSheet(sheetName);// 如果指定sheet名,则取指定sheet中的内容.
			}
			if (sheet == null) {
				sheet = workbook.getSheetAt(0); // 如果传入的sheet名不存在则默认指向第1个sheet.
			}

			// 获取数据
			list = dispatch(sheet, item);
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (EncryptedDocumentException e) {
			e.printStackTrace();
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		}

		return list;
	}

	/** 生成实体类的 List<T> 
	 *  由于采取反射技术，所以要传入 实体类的类型 ,例如Entity.class
	 * 
	 */
	public List<T> dispatch(Sheet sheet, Class<T> clazz) {
		List<T> instances = new ArrayList<>();

		List<Map<String, String>> sheetValue = parseExcelSheet(sheet);

		for (int i = 0; i < sheetValue.size(); i++) {
			Map<String, String> map = sheetValue.get(i);
			Field[] fields = clazz.getDeclaredFields();

			try {
				T t2 = clazz.newInstance();
				for (Field field : fields) {
					if("serialVersionUID".equals(field.getName()))
						continue ;
					field.setAccessible(true);
					Type type = field.getType();
					field.set(t2, getTypeValue(map.get(field.getName()), type.getTypeName()));
				}
				instances.add(t2);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		return instances;
	}

	//赋值判断类型
	public Object getTypeValue(String value, String type) {
		Object obj = new Object();
		if ("int".equals(type) || type.indexOf("Integer") > -1) {
			obj = Integer.valueOf(value);
		} else if ("short".equals(type) || type.indexOf("Short") > -1) {
			obj = Short.valueOf(value);
		} else if ("long".equals(type) || type.indexOf("Long") > -1) {
			obj = Long.valueOf(value);
		} else if ("float".equals(type) || type.indexOf("Float") > -1) {
			obj = Float.valueOf(value);
		} else if ("double".equals(type) || type.indexOf("Double") > -1) {
			obj = Double.valueOf(value);
		} else if ("boolean".equals(type) || type.indexOf("Boolean") > -1) {
			obj = Boolean.valueOf(value);
		} else {
			obj = value;
		}
		return obj;
	}
	/**
	 * 解析Excel第一行，并生成以第一行表头为key，每一行的值为value的map
	 * 例如: excel内容如下
	 * id	alarm
		1	abcd1
		2	abcd2
		3	abcd3
		那么List<Map>就是的内容就是
		List: map1( id: 1, alarm: abcd1 ),map2(( id: 2, alarm: abcd2 ).....
	 */
	public List<Map<String, String>> parseExcelSheet(Sheet sheet) {
		List<Map<String, String>> result = new ArrayList<>();
		Map<String, String> rowValue = null;
		int rows = sheet.getPhysicalNumberOfRows();
		String[] headers = getHeaderValue(sheet.getRow(0));

		for (int i = 1; i < rows; i++) {
			rowValue = new HashMap<>();
			Row row = sheet.getRow(i);
			for (int kk = 0; kk < headers.length; kk++) {
				rowValue.put(headers[kk], String.valueOf(getCellValue(row.getCell(kk))));
			}
			result.add(rowValue);
		}
		return result;
	}

	/** 获取第一行，表头，也就是实体类的字段，支持中英文，及下划线，忽略大小写，但是绝笔不能重复，表头有重复字段则不能解析  **/
	private String[] getHeaderValue(Row rowHeader) {
		int colNum = rowHeader.getPhysicalNumberOfCells();
		String[] headValue = new String[rowHeader.getPhysicalNumberOfCells()];
		for (int i = 0; i < colNum; i++) {
			headValue[i] = rowHeader.getCell(i).getStringCellValue();
		}
		return headValue;
	}

	@SuppressWarnings("deprecation")
	public Object getCellValue(Cell cell) {
		Object value = null;
		switch (cell.getCellType()) {
		case Cell.CELL_TYPE_NUMERIC: // 数字
			// 如果为时间格式的内容
			if (DateUtil.isCellDateFormatted(cell)) {
				// 注：format格式 yyyy-MM-dd hh:mm:ss
				// 中小时为12小时制，若要24小时制，则把小h变为H即可，yyyy-MM-dd HH:mm:ss
				SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd hh:mm:ss");
				value = sdf.format( DateUtil.getJavaDate(cell.getNumericCellValue())).toString();
				break;
			} else {
				value = new DecimalFormat("0").format(cell.getNumericCellValue());
			}
			break;
		case Cell.CELL_TYPE_STRING: // 字符串
			value = cell.getStringCellValue();
			break;
		case Cell.CELL_TYPE_BOOLEAN: // Boolean
			value = cell.getBooleanCellValue() + "";
			break;
		case Cell.CELL_TYPE_FORMULA: // 公式
			value = cell.getCellFormula() + "";
			break;
		case Cell.CELL_TYPE_BLANK: // 空值
			value = "";
			break;
		case Cell.CELL_TYPE_ERROR: // 故障
			value = "非法字符";
			break;
		default:
			value = "未知类型";
			break;
		}
		return value;
	}

}
