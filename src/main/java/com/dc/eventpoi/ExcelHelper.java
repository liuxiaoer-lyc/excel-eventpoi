/**
 * EventExcelHelper.java
 */
package com.dc.eventpoi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFDrawing;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.alibaba.fastjson.JSON;
import com.dc.eventpoi.core.PoiUtils;
import com.dc.eventpoi.core.entity.ExcelCell;
import com.dc.eventpoi.core.entity.ExcelRow;
import com.dc.eventpoi.core.entity.ExportExcelCell;
import com.dc.eventpoi.core.enums.FileType;
import com.dc.eventpoi.core.inter.CellStyleCallBack;
import com.dc.eventpoi.core.inter.ExcelEventStream;
import com.dc.eventpoi.core.inter.RowCallBack;
import com.dc.eventpoi.core.inter.SheetCallBack;

/**
 * excel操作
 *
 * @author beijing-penguin
 */
public class ExcelHelper {

    /**
     * 导出表格 以及 列表数据
     * 
     * @param tempExcelBtye        模板文件流
     * @param listAndTableDataList 包含列表数据集合 和 表格数据对象
     * @param sheetIndex           sheetIndex
     * @param sheetCallBack        sheetCallBack
     * @param callBackCellStyle    callBackCellStyle
     * @return byte[]
     * @throws Exception Exception
     */
    public static byte[] exportExcel(byte[] tempExcelBtye, List<?> listAndTableDataList, Integer sheetIndex, SheetCallBack sheetCallBack, CellStyleCallBack callBackCellStyle) throws Exception {
        boolean is_data_list = true;
        for (int i = 0,len = listAndTableDataList.size(); i < len; i++) {
			if(i == 5000) {
				break;
			}
			Object dataObj = listAndTableDataList.get(i);
			if(dataObj instanceof Collection) {
				is_data_list = false;
				break;
			}
		}
    	
    	Workbook workbook = null;
        FileType fileType = PoiUtils.judgeFileType(new ByteArrayInputStream(tempExcelBtye));
        if (fileType == FileType.XLSX) {
            workbook = new XSSFWorkbook(new ByteArrayInputStream(tempExcelBtye));
        } else {
            workbook = (HSSFWorkbook) WorkbookFactory.create(new ByteArrayInputStream(tempExcelBtye));
        }

        SXSSFWorkbook sxssfWorkbook = new SXSSFWorkbook();
        
        int sheetStart = 0;
        int sheetEnd = workbook.getNumberOfSheets();
        if (sheetIndex != null) {
            sheetStart = sheetIndex;
            sheetEnd = sheetIndex + 1;
        }
        for (int i = sheetStart; i < sheetEnd; i++) {
            SXSSFSheet sxssSheet = sxssfWorkbook.createSheet(workbook.getSheetName(i));
            if (sheetCallBack != null) {
                sheetCallBack.callBack(sxssSheet);
            }

            SXSSFDrawing patriarch = (SXSSFDrawing) sxssSheet.createDrawingPatriarch();
            Sheet xsssheet = workbook.getSheetAt(i);
            int sheetMergerCount = xsssheet.getNumMergedRegions();

            int rowNum = xsssheet.getPhysicalNumberOfRows();
            int offset = 0;
            int listCount = 0;
            for (int j = 0; j < rowNum; j++) {
                for (int ii = 0; ii < sheetMergerCount; ii++) {
                    CellRangeAddress mergedRegionAt = xsssheet.getMergedRegion(ii);
                    if (mergedRegionAt.getFirstRow() == j) {
                        mergedRegionAt.setFirstRow(mergedRegionAt.getFirstRow() + offset - listCount);
                        mergedRegionAt.setLastRow(mergedRegionAt.getLastRow() + offset - listCount);
                        sxssSheet.addMergedRegion(mergedRegionAt);
                    }
                }

                Row xssrow = xsssheet.getRow(j);
                int xssCellNum = xssrow.getPhysicalNumberOfCells();
                boolean breakFlag = false;

                SXSSFRow sxssrow = sxssSheet.createRow(j + offset - listCount);
                sxssrow.setHeight(xssrow.getHeight());

                for (int k = 0; k < xssCellNum; k++) {
                    final int temp_k = k;
                    if (breakFlag) {
                        break;
                    }
                    Cell xssCell = xssrow.getCell(k);
                    sxssSheet.setColumnWidth(k, xsssheet.getColumnWidth(k));
                    if (xssCell == null) {
                    } else {
                        boolean matchFlag = false;
                        String xssCellValue = xssCell.getStringCellValue();
                        if (xssCellValue != null && xssCellValue.contains("${")) {
                            String keyName = xssCellValue.substring(xssCellValue.indexOf("${") + 2, xssCellValue.lastIndexOf("}"));
                            String excelFieldSrcKeyword = xssCellValue.substring(xssCellValue.indexOf("${"), xssCellValue.lastIndexOf("}") + 1);

                            for (Object dataObj : listAndTableDataList) {
                                if (matchFlag) {
                                    break;
                                }
                                if ((dataObj instanceof Collection) || is_data_list == true) {
                                    List<?> dataList = null;
                                    if(is_data_list == true) {
                                    	dataList = listAndTableDataList;
                                    }else {
                                    	dataList = (List<?>) dataObj;
                                    }
                                    if (dataList.size() > 0) {
                                        Object tempData = dataList.get(0);
                                        if (FieldUtils.getField(tempData.getClass(), keyName, true) == null) {
                                            continue;
                                        }

                                        List<ExportExcelCell> keyCellList = new ArrayList<ExportExcelCell>();
                                        for (int kk = k; kk < xssCellNum; kk++) {
                                            Cell xssCell_kk = xssrow.getCell(kk);
                                            CellType type = xssCell_kk.getCellType();
                                            
                                            /*
                                             * Color color = xssCell_kk.getCellStyle().getFillBackgroundColorColor();
                                             * if(color != null) { System.err.println(((XSSFColor)color).getARGB());
                                             * System.err.println(((XSSFColor)color).getRGB());
                                             * System.err.println(((XSSFColor)color).getCTColor().xmlText());
                                             * System.err.println(((XSSFColor)color).getCTColor().getRgb());
                                             * System.err.println(((XSSFColor)color).getARGBHex()); }
                                             */
                                            //System.err.println(color);	
                                            
                                            CellStyle _sxssStyle = sxssfWorkbook.createCellStyle();
                                            _sxssStyle.cloneStyleFrom(xssCell_kk.getCellStyle());
                                            
                                            ExportExcelCell ee = new ExportExcelCell((short) xssCell_kk.getColumnIndex(), xssCell_kk.getStringCellValue(), _sxssStyle);
                                            ee.setCellType(type);
                                            keyCellList.add(ee);
                                        }
                                        breakFlag = true;
                                        matchFlag = true;
                                        listCount++;
                                        for (int y = 0,len=dataList.size(); y < len; y++) {
                                            final int create_row_num = j + offset;
                                            offset++;

                                            Object srcData = dataList.get(y);
                                            SXSSFRow sxssrow_y = sxssSheet.createRow(create_row_num);

                                            sxssrow_y.setHeight(xssrow.getHeight());
                                            for (int x = temp_k; x < xssCellNum; x++) {

                                                ExportExcelCell curCell = null;
                                                String vv = null;
                                                for (ExportExcelCell exportCell : keyCellList) {
                                                    if (exportCell.getIndex() == x) {
                                                        curCell = exportCell;
                                                        vv = exportCell.getValue();
                                                        break;
                                                    }
                                                }
                                                // curCell.getCellStyle().setFillForegroundColor(IndexedColors.AQUA.getIndex());
                                                // curCell.getCellStyle().setFillPattern(FillPatternType.SOLID_FOREGROUND);
                                                String _keyName = vv.substring(vv.indexOf("${") + 2, vv.lastIndexOf("}"));
                                                Field field = FieldUtils.getField(srcData.getClass(), _keyName, true);
                                                if (field != null && field.get(srcData) != null) {
                                                    SXSSFCell _sxssCell = sxssrow_y.createCell(x, curCell.getCellType());
                                                    if (callBackCellStyle != null) {
                                                        callBackCellStyle.callBack(sxssSheet, _sxssCell, curCell.getCellStyle());
                                                        _sxssCell.setCellStyle(curCell.getCellStyle());
                                                    } else {
                                                        _sxssCell.setCellStyle(curCell.getCellStyle());
                                                    }

                                                    Object value = field.get(srcData);
                                                    if (value instanceof byte[]) {
                                                        if (PoiUtils.getImageType((byte[]) value) != null) {
                                                            //XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, x, sxssrow_y.getRowNum(), x + 1, sxssrow_y.getRowNum() + 1);
//                                                            int picIndex = sxssSheet.getWorkbook().addPicture((byte[]) value, HSSFWorkbook.PICTURE_TYPE_PNG);
//                                                            Drawing drawing = sxssSheet.getDrawingPatriarch();
//                                                            if (drawing == null) {
//                                                                drawing = sxssSheet.createDrawingPatriarch();
//                                                            }
//                                                            
//                                                            CreationHelper helper = sxssSheet.getWorkbook().getCreationHelper();
//                                                            ClientAnchor anchor = helper.createClientAnchor();
//                                                            anchor.setDx1(0);
//                                                            anchor.setDx2(0);
//                                                            anchor.setDy1(0);
//                                                            anchor.setDy2(0);
//                                                            anchor.setCol1(_sxssCell.getColumnIndex());
//                                                            anchor.setCol2(_sxssCell.getColumnIndex() + 1);
//                                                            anchor.setRow1(_sxssCell.getRowIndex());
//                                                            anchor.setRow2(_sxssCell.getRowIndex() + 1);
//                                                            anchor.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);
//                                                            
//                                                            drawing.createPicture(anchor, picIndex);
                                                        	XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, x, sxssrow_y.getRowNum(), x + 1, sxssrow_y.getRowNum() + 1);
                                                            int picIndex = sxssfWorkbook.addPicture((byte[]) value, HSSFWorkbook.PICTURE_TYPE_JPEG);
                                                            patriarch.createPicture(anchor, picIndex);
                                                        } else {
                                                            _sxssCell.setCellValue(new String((byte[]) value));
                                                        }
                                                    } else {
                                                        _sxssCell.setCellValue(String.valueOf(value));
                                                    }
                                                } else {
                                                    SXSSFCell _sxssCell = sxssrow_y.createCell(x, curCell.getCellType());
                                                    if (callBackCellStyle != null) {
                                                        callBackCellStyle.callBack(sxssSheet, _sxssCell, curCell.getCellStyle());
                                                        _sxssCell.setCellStyle(curCell.getCellStyle());
                                                    } else {
                                                        _sxssCell.setCellStyle(curCell.getCellStyle());
                                                    }
                                                    _sxssCell.setCellValue("");
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Field field = FieldUtils.getField(dataObj.getClass(), keyName, true);
                                    if (field != null) {
                                        matchFlag = true;
                                        SXSSFCell sxssCell = sxssrow.createCell(k, xssCell.getCellType());
                                        CellStyle _sxssStyle = sxssfWorkbook.createCellStyle();
                                        if (callBackCellStyle != null) {
                                            _sxssStyle.cloneStyleFrom(xssCell.getCellStyle());
                                            sxssCell.setCellStyle(_sxssStyle);
                                            callBackCellStyle.callBack(sxssSheet, sxssCell, _sxssStyle);
                                        } else {
                                            _sxssStyle.cloneStyleFrom(xssCell.getCellStyle());
                                            sxssCell.setCellStyle(_sxssStyle);
                                        }

                                        Object value = field.get(dataObj);
                                        if (value instanceof byte[]) {
                                            if (PoiUtils.getImageType((byte[]) value) != null) {
                                                XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, k, sxssrow.getRowNum(), k + 1, sxssrow.getRowNum() + 1);
                                                int picIndex = sxssfWorkbook.addPicture((byte[]) value, HSSFWorkbook.PICTURE_TYPE_JPEG);
                                                patriarch.createPicture(anchor, picIndex);
                                            } else {
                                                sxssCell.setCellValue(new String((byte[]) value));
                                            }
                                        } else {
                                            String cellValue = xssCellValue.replace(excelFieldSrcKeyword, String.valueOf(field.get(dataObj)));
                                            sxssCell.setCellValue(cellValue);
                                        }
                                    }
                                }
                            }
                        }

                        if (matchFlag == false) {
                            SXSSFCell sxssCell = sxssrow.createCell(k, xssCell.getCellType());
                            String value = xssCell.getStringCellValue();
                            if (value != null && value.contains("${")) {
                                String excelFieldSrcKeyword = value.substring(value.indexOf("${"), value.lastIndexOf("}") + 1);
                                value = value.replace(excelFieldSrcKeyword, "");
                            }
                            CellStyle _sxssStyle = sxssfWorkbook.createCellStyle();
                            if (callBackCellStyle != null) {
                                _sxssStyle.cloneStyleFrom(xssCell.getCellStyle());
                                sxssCell.setCellStyle(_sxssStyle);
                                callBackCellStyle.callBack(sxssSheet, sxssCell, _sxssStyle);
                            } else {
                                _sxssStyle.cloneStyleFrom(xssCell.getCellStyle());
                                sxssCell.setCellStyle(_sxssStyle);
                            }
                            sxssCell.setCellValue(value);
                        }
                    }
                }
            }
        }

        workbook.close();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        sxssfWorkbook.write(byteStream);
        byteStream.flush();
        byteStream.close();
        sxssfWorkbook.close();
        sxssfWorkbook.dispose();
        return byteStream.toByteArray();
    }

    /**
     * 解析Excel为对象集合
     * 
     * @param excelTemplateStream   模版数据流
     * @param excelDataSourceStream Excel原数据流
     * @param clazz                 clazz
     * @param imageRead             是否支持图片格式读取（开启此功能，性能降低，内存消耗增加。）
     * @param <T>                   返回对象
     * @return 对象集合
     * @throws Exception IOException
     */
    public static <T> List<T> parseExcelToObject(InputStream excelTemplateStream,InputStream excelDataSourceStream,  Class<T> clazz, boolean imageRead) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while (-1 != (n = excelDataSourceStream.read(buffer))) {
            output.write(buffer, 0, n);
        }

        // 创建Workbook
        Workbook wb = null;
        // 创建sheet
        Sheet sheet = null;
        FileType fileType = PoiUtils.judgeFileType(new ByteArrayInputStream(output.toByteArray()));
        switch (fileType) {
        case XLS:
            wb = (HSSFWorkbook) WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()));
            break;
        case XLSX:
            wb = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()));
            break;
        default:
            throw new Exception("filetype is unsupport");
        }
        // 获取excel sheet总数
        int sheetNumbers = wb.getNumberOfSheets();

        Map<String, byte[]> map = new HashMap<String, byte[]>();
        // 循环sheet
        for (int i = 0; i < sheetNumbers; i++) {

            sheet = wb.getSheetAt(i);

            switch (fileType) {
            case XLS:
                map.putAll(PoiUtils.getXlsPictures(i, (HSSFSheet) sheet));
                break;
            case XLSX:
                map.putAll(PoiUtils.getXlsxPictures(i, (XSSFSheet) sheet));
                break;
            default:
                throw new Exception("filetype is unsupport");
            }
        }
        wb.close();

        List<ExcelRow> dataList = ExcelHelper.parseExcelRowList(new ByteArrayInputStream(output.toByteArray()));
        List<ExcelRow> templeteList = ExcelHelper.parseExcelRowList(excelTemplateStream);
        checkTemplete(templeteList,dataList);

        if (map.size() > 0) {
            for (ExcelRow excelRow : dataList) {
                int rowIndex = excelRow.getRowIndex();
                int sheetIndex = excelRow.getSheetIndex();
                List<ExcelCell> cellList = excelRow.getCellList();
                for (Entry<String, byte[]> entry : map.entrySet()) {
                    int img_sheetIndex = Integer.parseInt(entry.getKey().split("-")[0]);
                    int img_rowIndex = Integer.parseInt(entry.getKey().split("-")[1]);
                    int img_cellIndex = Integer.parseInt(entry.getKey().split("-")[2]);
                    if (rowIndex == img_rowIndex && img_sheetIndex == sheetIndex) {
                        ExcelCell imgCell = new ExcelCell((short) img_sheetIndex, entry.getValue());
                        cellList.add(img_cellIndex, imgCell);
                        break;
                    }
                }
            }
        }
        return ExcelHelper.parseExcelToObject(templeteList,dataList, clazz);
    }

    public static <T> List<T> parseExcelToObject(InputStream excelTemplateStream,InputStream excelDataSourceStream,  Class<T> clazz) throws Exception {
        List<ExcelRow> dataList = ExcelHelper.parseExcelRowList(excelDataSourceStream);
        List<ExcelRow> templeteList = ExcelHelper.parseExcelRowList(excelTemplateStream);
        checkTemplete(templeteList,dataList);
        return ExcelHelper.parseExcelToObject(templeteList,dataList, clazz);
    }

    /**
     * @param fileList     数据文件
     * @param templeteList 模板文件
     * @param clazz        类对象
     * @param <T>          T
     * @return 集合
     * @throws Exception IOException
     * @author beijing-penguin
     */
    public static <T> List<T> parseExcelToObject( List<ExcelRow> templeteList,List<ExcelRow> fileList, Class<T> clazz) throws Exception {
        List<T> rtn = new ArrayList<T>();
        List<ExcelCell> tempFieldList = new ArrayList<ExcelCell>();
        int size = fileList.size();
        int x = 0;
        int startRow = 0;
        for (int i = 0; i < templeteList.size(); i++) {
            if (templeteList.get(i).getCellList().get(0).getValue().startsWith("$")) {
                startRow = templeteList.get(i).getRowIndex();
                short sheetIndex = templeteList.get(i).getSheetIndex();
                tempFieldList = templeteList.get(i).getCellList();

                for (int j = (x + startRow); j < size; j++) {
                    ExcelRow row = fileList.get(j);
                    int rowIndex = row.getRowIndex();
                    if (rowIndex >= startRow && row.getSheetIndex() == sheetIndex) {
                        x++;
                        T obj = clazz.getDeclaredConstructor().newInstance();
                        List<ExcelCell> fieldList = row.getCellList();
                        for (ExcelCell fieldCell : fieldList) {
                            for (ExcelCell tempCell : tempFieldList) {
                                if (fieldCell.getIndex() == tempCell.getIndex()) {
                                    for (Field field : FieldUtils.getAllFields(clazz)) {
                                        if (!Modifier.isStatic(field.getModifiers())) {
                                            if (tempCell.getValue().contains(field.getName())) {
                                                field.setAccessible(true);
                                                if (fieldCell.getImgBytes() != null) {
                                                    // Object vall = getValueByFieldType(fieldCell.getImgBytes(),
                                                    // field.getType());
                                                    field.set(obj, fieldCell.getImgBytes());
                                                } else {
                                                    Object vall = PoiUtils.getValueByFieldType(fieldCell.getValue(), field.getType());
                                                    field.set(obj, vall);
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        rtn.add(obj);
                    }
                }
            }
        }

        return rtn;
    }

    /**
     * 读取所有sheet数据
     *
     * @param baytes 文件
     * @return List
     * @throws Exception IOException
     * @author dc
     */
    public static List<ExcelRow> parseExcelRowList(byte[] baytes) throws Exception {
        return parseExcelRowList(new ByteArrayInputStream(baytes));
    }

    /**
     * 读取excel指定sheet数据
     *
     * @param baytes     文件数据
     * @param sheetIndex sheet工作簿索引号
     * @return List
     * @throws Exception IOException
     * @author dc
     */
    public static List<ExcelRow> parseExcelRowList(byte[] baytes, Integer sheetIndex) throws Exception {
        return parseExcelRowList(new ByteArrayInputStream(baytes), sheetIndex);
    }

    /**
     * 读取excel指定sheet数据
     *
     * @param file       文件
     * @param sheetIndex sheet工作簿索引号
     * @return List
     * @throws Exception IOException
     * @author dc
     */
    public static List<ExcelRow> parseExcelRowList(File file, Integer sheetIndex) throws Exception {
        return parseExcelRowList(new FileInputStream(file), sheetIndex);
    }

    /**
     * 读取所有sheet数据
     *
     * @param file 文件
     * @return List
     * @throws Exception IOException
     * @author dc
     */
    public static List<ExcelRow> parseExcelRowList(File file) throws Exception {
        return parseExcelRowList(new FileInputStream(file), null);
    }

    /**
     * 读取指定sheet数据
     *
     * @param inputSrc   excel源文件input输入流
     * @param sheetIndex sheet工作簿索引号
     * @return List
     * @throws Exception IOException
     * @author dc
     */
    public static List<ExcelRow> parseExcelRowList(InputStream inputSrc, Integer sheetIndex) throws Exception {
        List<ExcelRow> fileList = new ArrayList<ExcelRow>();
        ExcelEventStream fileStream = null;
        try {
            fileStream = ExcelEventStream.readExcel(inputSrc);
            fileStream.sheetAt(sheetIndex).rowStream(new RowCallBack() {
                @Override
                public void getRow(ExcelRow row) {
                    fileList.add(row);
                }
            });
        } catch (Exception e) {
            throw e;
        } finally {
            if (fileStream != null) {
                fileStream.close();
            }
        }
        return fileList;
    }

    /**
     * @param inputSrc excel源文件input输入流
     * @return List
     * @throws Exception IOException
     * @author dc
     */
    public static List<ExcelRow> parseExcelRowList(InputStream inputSrc) throws Exception {
        return parseExcelRowList(inputSrc, null);
    }

    /**
     * 模板与数据文件检查
     *
     * @param templeteList 模板文件
     * @param fileList     原始上传文件
     * @throws Exception IOException
     * @author beijing-penguin
     */
    public static void checkTemplete(List<ExcelRow> templeteList, List<ExcelRow> fileList) throws Exception {
        for (int i = 0; i < templeteList.size(); i++) {
            ExcelRow row = templeteList.get(i);
            List<ExcelCell> excelCell = row.getCellList();
            if (!excelCell.get(0).getValue().startsWith("${")) {
                if (!JSON.toJSONString(templeteList.get(i)).equals(JSON.toJSONString(fileList.get(i)))) {
                    throw new Exception("fileList is not the same as templeteList[读取文件的excel头信息和模板头信息不匹配，文件格式不一致]");
                }
            } else {
                break;
            }
        }
    }

    /**
     * 导出列表或表格excel文件
     *
     * @param templete          模板数据
     * @param listData          对象数据集合
     * @param callBackCellStyle 单元格样式
     * @return byte[]
     * @throws Exception Exception
     */
    public static byte[] exportExcel(byte[] templete, List<?> listData, CellStyleCallBack callBackCellStyle) throws Exception {
        return exportExcel(templete, listData, 0, null, callBackCellStyle);
    }

    /**
     * 导出列表或表格excel文件
     *
     * @param templete   templete
     * @param listData   listData
     * @param sheetIndex sheetIndex
     * @return byte[]
     * @throws Exception Exception
     */
    public static byte[] exportExcel(byte[] templete, List<?> listData, Integer sheetIndex) throws Exception {
        return exportExcel(templete, Arrays.asList(listData), sheetIndex, null, null);
    }

    /**
     * 导出列表或表格excel文件
     *
     * @param templete             templete
     * @param listAndTableDataList listAndTableDataList
     * @return byte[]
     * @throws Exception Exception
     */
    public static byte[] exportExcel(byte[] templete, List<?> listAndTableDataList) throws Exception {
        return exportExcel(templete, Arrays.asList(listAndTableDataList), 0, null, null);
    }

    /**
     * 导出表格excel文件
     *
     * @param templeteStream 模板数据流
     * @param tableData      表格数据
     * @return byte[]
     * @throws Exception Exception
     */
    public static byte[] exportExcel(InputStream templeteStream, Object tableData) throws Exception {
        return exportExcel(PoiUtils.inputStreamToByte(templeteStream), Arrays.asList(tableData), 0, null, null);
    }

    /**
     * 导出表格excel文件
     *
     * @param templete  模板数据
     * @param tableData dataList
     * @return byte[]
     * @throws Exception IOException
     */
    public static byte[] exportExcel(byte[] templete, Object tableData) throws Exception {
        return exportExcel(templete, Arrays.asList(tableData), 0, null, null);
    }

    /**
     * 导出列表或表格excel文件
     *
     * @param templeteStream       模板数据流
     * @param listAndTableDataList dataList
     * @param sheetCallBack        sheet回调
     * @return byte[]
     * @throws Exception Exception
     */
    public static byte[] exportExcel(InputStream templeteStream, List<Object> listAndTableDataList, SheetCallBack sheetCallBack) throws Exception {
        return exportExcel(PoiUtils.inputStreamToByte(templeteStream), listAndTableDataList, null, sheetCallBack, null);
    }

    /**
     * 导出列表或表格excel文件
     *
     * @param templeteStream       模板数据流
     * @param listAndTableDataList dataList
     * @param sheetCallBack        sheet回调
     * @param callBackCellStyle    样式回调
     * @return byte[]
     * @throws Exception Exception
     */
    public static byte[] exportExcel(InputStream templeteStream, List<Object> listAndTableDataList, SheetCallBack sheetCallBack, CellStyleCallBack callBackCellStyle) throws Exception {
        return exportExcel(PoiUtils.inputStreamToByte(templeteStream), listAndTableDataList, null, sheetCallBack, callBackCellStyle);
    }

    /**
     * 导出列表或表格excel文件
     *
     * @param templeteStream       模板数据流
     * @param listAndTableDataList dataList
     * @param callBackCellStyle    样式回调
     * @return byte[]
     * @throws Exception Exception
     */
    public static byte[] exportExcel(InputStream templeteStream, List<Object> listAndTableDataList, CellStyleCallBack callBackCellStyle) throws Exception {
        return exportExcel(PoiUtils.inputStreamToByte(templeteStream), listAndTableDataList, null, null, callBackCellStyle);
    }

    /**
     * 导出列表或表格excel文件
     *
     * @param templeteStream       模板数据流
     * @param listAndTableDataList dataList
     * @param sheetIndex           sheetIndex
     * @return byte[]
     * @throws Exception Exception
     */
    public static byte[] exportExcel(InputStream templeteStream, List<?> listAndTableDataList, Integer sheetIndex) throws Exception {
    	return exportExcel(PoiUtils.inputStreamToByte(templeteStream), listAndTableDataList, sheetIndex, null, null);
    }
    /**
     * 导出列表或表格excel文件
     *
     * @param templeteStream       模板文件流
     * @param listAndTableDataList dataList
     * @return byte[]
     * @throws Exception Exception
     */
    public static byte[] exportExcel(InputStream templeteStream, List<Object> listAndTableDataList) throws Exception {
        return exportExcel(PoiUtils.inputStreamToByte(templeteStream), listAndTableDataList, 0, null, null);
    }

}
