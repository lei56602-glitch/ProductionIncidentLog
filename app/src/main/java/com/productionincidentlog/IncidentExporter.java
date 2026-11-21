package com.productionincidentlog;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class IncidentExporter {
    private static String esc(String s) {
        if (s==null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
            .replace("\"","&quot;").replace("'","&apos;");
    }
    private static String cell(String value) {
        return "<Cell ss:StyleID=\"Body\"><Data ss:Type=\"String\">"+esc(value)+"</Data></Cell>";
    }
    private static String headerCell(String value) {
        return "<Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">"+esc(value)+"</Data></Cell>";
    }
    private static String row(String... values) {
        StringBuilder b=new StringBuilder("<Row ss:AutoFitHeight=\"1\">");
        for(String v:values) b.append(cell(v));
        return b.append("</Row>").toString();
    }
    private static String headerRow(String... values) {
        StringBuilder b=new StringBuilder("<Row ss:AutoFitHeight=\"1\">");
        for(String v:values) b.append(headerCell(v));
        return b.append("</Row>").toString();
    }
    private static String styles() {
        return "<Styles>"+
            "<Style ss:ID=\"Default\" ss:Name=\"Normal\"><Alignment ss:Vertical=\"Center\"/><Borders/><Font ss:FontName=\"等线\" ss:Size=\"10\"/><Interior/><NumberFormat/><Protection/></Style>"+
            "<Style ss:ID=\"Header\"><Alignment ss:Vertical=\"Center\" ss:WrapText=\"1\"/><Font ss:FontName=\"等线\" ss:Size=\"10\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/><Interior ss:Color=\"#1556A8\" ss:Pattern=\"Solid\"/><Borders><Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#FFFFFF\"/></Borders></Style>"+
            "<Style ss:ID=\"Body\"><Alignment ss:Vertical=\"Top\" ss:WrapText=\"1\"/><Font ss:FontName=\"等线\" ss:Size=\"10\"/><Borders><Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\" ss:Color=\"#E2E8F0\"/></Borders></Style>"+
            "</Styles>";
    }
    private static String sheet(String name,String content,int columnCount,int dataRows) {
        StringBuilder columns=new StringBuilder();
        for(int i=0;i<columnCount;i++) columns.append("<Column ss:AutoFitWidth=\"1\"/>");
        String range="R1C1:R"+(dataRows+1)+"C"+columnCount;
        return "<Worksheet ss:Name=\""+esc(name)+"\"><Table>"+columns+content+"</Table>"+
            "<AutoFilter x:Range=\""+range+"\" xmlns=\"urn:schemas-microsoft-com:office:excel\"/>"+
            "<WorksheetOptions xmlns=\"urn:schemas-microsoft-com:office:excel\">"+
            "<FreezePanes/><FrozenNoSplit/><SplitHorizontal>1</SplitHorizontal><TopRowBottomPane>1</TopRowBottomPane><ActivePane>2</ActivePane>"+
            "</WorksheetOptions></Worksheet>";
    }
    private static void zipText(java.util.zip.ZipOutputStream zip,String name,String value) throws IOException {
        zip.putNextEntry(new java.util.zip.ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
    private static String xml(String value) {
        if(value==null)return "";
        String clean=value.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]","");
        return clean.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
            .replace("\"","&quot;").replace("'","&apos;");
    }
    private static String columnName(int index) {
        StringBuilder out=new StringBuilder();int n=index+1;
        while(n>0){int r=(n-1)%26;out.insert(0,(char)('A'+r));n=(n-1)/26;}
        return out.toString();
    }
    private static String worksheetXml(List<String[]> rows) {
        StringBuilder out=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><sheetFormatPr defaultRowHeight=\"20\"/><sheetData>");
        for(int r=0;r<rows.size();r++){
            out.append("<row r=\"").append(r+1).append("\">");
            String[] values=rows.get(r);
            for(int c=0;c<values.length;c++){
                String ref=columnName(c)+(r+1);
                out.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"");
                if(r==0)out.append(" s=\"1\"");
                out.append("><is><t xml:space=\"preserve\">").append(xml(values[c])).append("</t></is></c>");
            }
            out.append("</row>");
        }
        return out.append("</sheetData><autoFilter ref=\"A1:").append(columnName(rows.isEmpty()?0:rows.get(0).length-1))
            .append(Math.max(1,rows.size())).append("\"/><pageMargins left=\"0.25\" right=\"0.25\" top=\"0.5\" bottom=\"0.5\" header=\"0.2\" footer=\"0.2\"/></worksheet>").toString();
    }
    private static Map<String,Integer> countColumn(List<String[]> rows,int column) {
        Map<String,Integer> result=new TreeMap<>();
        for(String[] row:rows){String key=row[column]==null||row[column].trim().isEmpty()?"未填写":row[column];result.put(key,result.containsKey(key)?result.get(key)+1:1);}
        return result;
    }
    private static List<String[]> statsRows(String title,Map<String,Integer> values) {
        List<String[]> out=new ArrayList<>();out.add(new String[]{"统计类别","分类项目","事故数量"});
        for(Map.Entry<String,Integer> entry:values.entrySet())out.add(new String[]{title,entry.getKey(),String.valueOf(entry.getValue())});
        return out;
    }
    public static File writeExcel(Context context,List<String[]> rows,String from,String to) throws Exception {
        List<List<String[]>> sheets=new ArrayList<>();
        List<String[]> summary=new ArrayList<>();
        summary.add(new String[]{"事故编号","发生时间","班组","区域","事故类型","设备名称","整改负责人","整改期限","整改状态"});
        List<String[]> details=new ArrayList<>();
        details.add(new String[]{"事故编号","发生时间","班组","区域","事故类型","设备名称","涉事人员","事故经过","事故分析","事故原因","当下采取措施","未来如何避免","整改负责人","整改期限","整改状态"});
        for(String[] r:rows){
            summary.add(new String[]{r[0],r[1],r[2],r[3],r[4],r[5],r[12],r[13],r[14]});
            details.add(new String[]{r[0],r[1],r[2],r[3],r[4],r[5],r[6],r[7],r[8],r[9],r[10],r[11],r[12],r[13],r[14]});
        }
        sheets.add(summary);sheets.add(details);
        sheets.add(statsRows("事故类型",countColumn(rows,4)));
        sheets.add(statsRows("班组",countColumn(rows,2)));
        sheets.add(statsRows("区域",countColumn(rows,3)));
        sheets.add(statsRows("设备",countColumn(rows,5)));
        String[] names={"事故汇总表","事故详细记录","事故分类统计","班组统计","区域统计","设备统计"};
        StringBuilder workbook=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        StringBuilder rels=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for(int i=0;i<names.length;i++){workbook.append("<sheet name=\"").append(xml(names[i])).append("\" sheetId=\"").append(i+1).append("\" r:id=\"rId").append(i+1).append("\"/>");rels.append("<Relationship Id=\"rId").append(i+1).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i+1).append(".xml\"/>");}
        workbook.append("</sheets></workbook>");rels.append("<Relationship Id=\"rId7\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
        String types="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>";
        StringBuilder contentTypes=new StringBuilder(types);
        for(int i=0;i<names.length;i++)contentTypes.append("<Override PartName=\"/xl/worksheets/sheet").append(i+1).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        contentTypes.append("</Types>");
        String styles="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><fonts count=\"2\"><font><sz val=\"10\"/><name val=\"等线\"/></font><font><b/><color rgb=\"FFFFFFFF\"/><sz val=\"10\"/><name val=\"等线\"/></font></fonts><fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF1556A8\"/><bgColor indexed=\"64\"/></patternFill></fill></fills><borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"2\"><xf xfId=\"0\"/><xf xfId=\"0\" fontId=\"1\" fillId=\"1\" applyFont=\"1\" applyFill=\"1\"/></cellXfs></styleSheet>";
        File dir=new File(context.getCacheDir(),"exports");if(!dir.exists()&&!dir.mkdirs())throw new IOException("无法创建报表目录");
        String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
        File file=new File(dir,"生产事故报表_"+stamp+".xlsx");
        try(java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(new FileOutputStream(file))){
            zipText(zip,"[Content_Types].xml",contentTypes.toString());
            zipText(zip,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            zipText(zip,"xl/workbook.xml",workbook.toString());zipText(zip,"xl/_rels/workbook.xml.rels",rels.toString());zipText(zip,"xl/styles.xml",styles);
            for(int i=0;i<sheets.size();i++)zipText(zip,"xl/worksheets/sheet"+(i+1)+".xml",worksheetXml(sheets.get(i)));
        }
        return file;
    }
    private static String wordParagraph(String value,String style) {
        return "<w:p><w:pPr>"+(style==null?"":"<w:pStyle w:val=\""+style+"\"/>")+"</w:pPr><w:r><w:t xml:space=\"preserve\">"+xml(value)+"</w:t></w:r></w:p>";
    }
    private static String wordCell(String value,boolean header) {
        return "<w:tc><w:tcPr><w:tcW w:w=\"4500\" w:type=\"dxa\"/>"+(header?"<w:shd w:fill=\"DCEBFA\"/>":"")+"</w:tcPr>"+wordParagraph(value,header?"Heading2":null)+"</w:tc>";
    }
    private static String wordTable(List<String[]> values) {
        StringBuilder out=new StringBuilder("<w:tbl><w:tblPr><w:tblBorders><w:top w:val=\"single\" w:sz=\"4\" w:color=\"B8C7D8\"/><w:left w:val=\"single\" w:sz=\"4\" w:color=\"B8C7D8\"/><w:bottom w:val=\"single\" w:sz=\"4\" w:color=\"B8C7D8\"/><w:right w:val=\"single\" w:sz=\"4\" w:color=\"B8C7D8\"/><w:insideH w:val=\"single\" w:sz=\"4\" w:color=\"B8C7D8\"/><w:insideV w:val=\"single\" w:sz=\"4\" w:color=\"B8C7D8\"/></w:tblBorders></w:tblPr>");
        for(int i=0;i<values.size();i++){out.append("<w:tr>");String[] row=values.get(i);for(String value:row)out.append(wordCell(value,i==0));out.append("</w:tr>");}
        return out.append("</w:tbl>").toString();
    }
    public static File writeWord(Context context,List<String[]> rows,String from,String to) throws Exception {
        StringBuilder body=new StringBuilder();
        body.append(wordParagraph("生产事故统计与分析报告","Title"));
        body.append(wordParagraph("统计范围："+(from==null||from.isEmpty()?"不限":from)+" 至 "+(to==null||to.isEmpty()?"不限":to),"Subtitle"));
        body.append(wordParagraph("事故总数："+rows.size()+" 起","Heading1"));
        Map<String,Integer> typeCounts=countColumn(rows,4),teamCounts=countColumn(rows,2),areaCounts=countColumn(rows,3);
        List<String[]> summary=new ArrayList<>();summary.add(new String[]{"统计维度","分类","事故数量"});
        for(Map.Entry<String,Integer> e:typeCounts.entrySet())summary.add(new String[]{"事故类型",e.getKey(),String.valueOf(e.getValue())});
        for(Map.Entry<String,Integer> e:teamCounts.entrySet())summary.add(new String[]{"班组",e.getKey(),String.valueOf(e.getValue())});
        for(Map.Entry<String,Integer> e:areaCounts.entrySet())summary.add(new String[]{"区域",e.getKey(),String.valueOf(e.getValue())});
        body.append(wordParagraph("一、事故分类统计","Heading1")).append(wordTable(summary));
        body.append(wordParagraph("二、事故档案明细","Heading1"));
        if(rows.isEmpty())body.append(wordParagraph("当前筛选范围内暂无事故记录。",null));
        for(int i=0;i<rows.size();i++){
            String[] r=rows.get(i);body.append(wordParagraph((i+1)+"、"+r[0]+"  "+r[4],"Heading2"));
            List<String[]> fields=new ArrayList<>();fields.add(new String[]{"项目","内容"});
            String[] labels={"发生时间","班组","区域","事故类型","设备名称","涉事人员","事故经过","事故分析","事故原因","当下采取措施","未来预防措施","整改负责人","整改期限","整改状态"};
            int[] indexes={1,2,3,4,5,6,7,8,9,10,11,12,13,14};
            for(int k=0;k<labels.length;k++)fields.add(new String[]{labels[k],r[indexes[k]]});
            body.append(wordTable(fields));
        }
        String document="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>"+body+"<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1134\" w:right=\"850\" w:bottom=\"1134\" w:left=\"850\"/></w:sectPr></w:body></w:document>";
        String types="<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/><Override PartName=\"/word/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml\"/></Types>";
        String styles="<?xml version=\"1.0\" encoding=\"UTF-8\"?><w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\"><w:name w:val=\"Normal\"/><w:rPr><w:rFonts w:eastAsia=\"等线\"/><w:sz w:val=\"21\"/></w:rPr></w:style><w:style w:type=\"paragraph\" w:styleId=\"Title\"><w:name w:val=\"Title\"/><w:rPr><w:b/><w:sz w:val=\"36\"/><w:rFonts w:eastAsia=\"等线\"/></w:rPr></w:style><w:style w:type=\"paragraph\" w:styleId=\"Subtitle\"><w:name w:val=\"Subtitle\"/><w:rPr><w:color w:val=\"64748B\"/><w:sz w:val=\"20\"/></w:rPr></w:style><w:style w:type=\"paragraph\" w:styleId=\"Heading1\"><w:name w:val=\"heading 1\"/><w:rPr><w:b/><w:sz w:val=\"28\"/></w:rPr></w:style><w:style w:type=\"paragraph\" w:styleId=\"Heading2\"><w:name w:val=\"heading 2\"/><w:rPr><w:b/><w:sz w:val=\"23\"/></w:rPr></w:style></w:styles>";
        File dir=new File(context.getCacheDir(),"exports");if(!dir.exists()&&!dir.mkdirs())throw new IOException("无法创建报表目录");
        String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
        File file=new File(dir,"生产事故报告_"+stamp+".docx");
        try(java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(new FileOutputStream(file))){
            zipText(zip,"[Content_Types].xml",types);
            zipText(zip,"_rels/.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/></Relationships>");
            zipText(zip,"word/document.xml",document);
            zipText(zip,"word/styles.xml",styles);
            zipText(zip,"word/_rels/document.xml.rels","<?xml version=\"1.0\" encoding=\"UTF-8\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
        }
        return file;
    }
    private static void appendStats(StringBuilder b,String group,Map<String,Integer> map) {
        for(Map.Entry<String,Integer> e:map.entrySet()) b.append(row(group,e.getKey(),String.valueOf(e.getValue())));
    }
    private static class PdfPageWriter {
        final PdfDocument pdf;
        final Paint paint;
        final String from,to;
        final int total;
        PdfDocument.Page page;
        Canvas canvas;
        int pageNo=0,y=0;
        PdfPageWriter(PdfDocument pdf,Paint paint,String from,String to,int total) {
            this.pdf=pdf;this.paint=paint;this.from=from;this.to=to;this.total=total;
            newPage();
        }
        void finishCurrentPage() {
            if(page==null) return;
            paint.setColor(0xFF94A3B8);paint.setStrokeWidth(0.6f);
            canvas.drawLine(40,814,555,814,paint);
            paint.setColor(0xFF64748B);paint.setTextSize(8);paint.setTypeface(Typeface.DEFAULT);
            String footer="生产事故管理系统  ·  第 "+pageNo+" 页";
            canvas.drawText(footer,555-paint.measureText(footer),830,paint);
            pdf.finishPage(page);page=null;
        }
        void newPage() {
            if(page!=null) finishCurrentPage();
            PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(595,842,++pageNo).create();
            page=pdf.startPage(info);canvas=page.getCanvas();y=42;
            paint.setColor(Color.BLACK);paint.setTextSize(17);
            paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            canvas.drawText("生产事故报告",40,y,paint);y+=25;
            paint.setTextSize(9);paint.setTypeface(Typeface.DEFAULT);
            canvas.drawText("统计范围："+(from.isEmpty()?"不限":from)+" 至 "+(to.isEmpty()?"不限":to)+"    事故总数："+total,40,y,paint);y+=22;
            canvas.drawLine(40,y,555,y,paint);y+=20;
        }
        void ensure(int height) { if(y+height>800) newPage(); }
        void line(String text,boolean bold,float left) {
            ensure(15);
            paint.setTextSize(9);
            paint.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.DEFAULT);
            canvas.drawText(text,left,y,paint);y+=14;
        }
        void wrapped(String label,String value) {
            String remaining=(label==null?"":label)+(value==null?"":value);
            if(remaining.isEmpty()) remaining="—";
            paint.setTextSize(9);paint.setTypeface(Typeface.DEFAULT);
            float maxWidth=515;
            boolean first=true;
            while(!remaining.isEmpty()) {
                String prefix=first?"":"  ";
                float width=paint.measureText(prefix);
                int end=0;
                while(end<remaining.length()) {
                    float next=paint.measureText(remaining.substring(end,end+1));
                    if(width+next>maxWidth && end>0) break;
                    width+=next;end++;
                }
                if(end==0) end=1;
                line(prefix+remaining.substring(0,end),false,first?40:52);
                remaining=remaining.substring(end);first=false;
            }
        }
        void finish() { finishCurrentPage(); }
    }
    public static File writePdf(Context context,List<String[]> rows,String from,String to) throws Exception {
        PdfDocument pdf=new PdfDocument();
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        PdfPageWriter writer=new PdfPageWriter(pdf,paint,from,to,rows.size());
        writer.line("事故档案明细",true,40);
        if(rows.isEmpty()) writer.wrapped("","该时间范围内暂无事故记录。");
        for(String[] r:rows) {
            writer.wrapped("编号：",r[0]);
            writer.wrapped("时间：",r[1]);
            writer.wrapped("班组 / 区域：",r[2]+" / "+r[3]);
            writer.wrapped("类型 / 设备：",r[4]+" / "+r[5]);
            writer.wrapped("涉事人员：",r[6]);
            writer.wrapped("事故经过：",r[7]);
            writer.wrapped("事故分析：",r[8]);
            writer.wrapped("事故原因：",r[9]);
            writer.wrapped("当下措施：",r[10]);
            writer.wrapped("未来预防：",r[11]);
            writer.wrapped("整改负责人 / 期限 / 状态：",r[12]+" / "+r[13]+" / "+r[14]);
            writer.ensure(14);
            writer.line("────────────────────────────────────────",false,40);
        }
        writer.finish();
        File dir=new File(context.getCacheDir(),"exports");if(!dir.exists()&&!dir.mkdirs())throw new IOException("无法创建报表目录");
        String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
        File file=new File(dir,"生产事故报告_"+stamp+".pdf");
        try(FileOutputStream out=new FileOutputStream(file)){pdf.writeTo(out);}
        pdf.close();return file;
    }
}
