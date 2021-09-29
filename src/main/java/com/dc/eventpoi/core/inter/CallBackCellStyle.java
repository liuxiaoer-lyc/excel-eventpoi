package com.dc.eventpoi.core.inter;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFSheet;

public interface CallBackCellStyle {
    /**
     * 单元格样式回调
     * @param curCell 当前单元格对象
     * @param curCellStyle 当前单元格样式
     */
    void callBack(SXSSFSheet sxssSheet,SXSSFCell curCell,CellStyle curCellStyle);
}
