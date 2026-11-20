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
        return "<Cell><Data ss:Type=\"String\">"+esc(value)+"</Data></Cell>";
    }
    private static String row(String... values) {
        StringBuilder b=new StringBuilder("<Row>");
        for(String v:values) b.append(cell(v));
        return b.append("</Row>").toString();
    }
    private static String sheet(String name,String content) {
        return "<Worksheet ss:Name=\""+esc(name)+"\"><Table>"+content+"</Table></Worksheet>";
    }
    public static File writeExcel(Context context,List<String[]> rows,String from,String to) throws Exception {
        StringBuilder summary=new StringBuilder();
        summary.append(row("事故编号","发生时间","班组","区域","事故类型","设备名称","整改负责人","整改期限","整改状态"));
        for(String[] r:rows) summary.append(row(r[0],r[1],r[2],r[3],r[4],r[5],r[12],r[13],r[14]));
        StringBuilder details=new StringBuilder();
        details.append(row("事故编号","发生时间","班组","区域","事故类型","设备名称","涉事人员","事故经过","事故分析","事故原因","当下采取措施","未来如何避免","整改负责人","整改期限","整改状态"));
        for(String[] r:rows) details.append(row(r[0],r[1],r[2],r[3],r[4],r[5],r[6],r[7],r[8],r[9],r[10],r[11],r[12],r[13],r[14]));
        Map<String,Integer> types=new TreeMap<>(), teams=new TreeMap<>(), areas=new TreeMap<>(), equipment=new TreeMap<>();
        for(String[] r:rows) {
            types.put(r[4],types.containsKey(r[4])?types.get(r[4])+1:1);
            teams.put(r[2],teams.containsKey(r[2])?teams.get(r[2])+1:1);
            areas.put(r[3],areas.containsKey(r[3])?areas.get(r[3])+1:1);
            equipment.put(r[5],equipment.containsKey(r[5])?equipment.get(r[5])+1:1);
        }
        StringBuilder stats=new StringBuilder();
        stats.append(row("统计项目","分类","事故数量"));
        stats.append(row("事故总数","全部",String.valueOf(rows.size())));
        appendStats(stats,"事故类型",types); appendStats(stats,"班组",teams);
        appendStats(stats,"区域",areas); appendStats(stats,"设备",equipment);
        String xml="<?xml version=\"1.0\" encoding=\"UTF-8\"?><?mso-application progid=\"Excel.Sheet\"?>"+
            "<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" "+
            "xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">"+
            sheet("事故汇总表",summary.toString())+sheet("事故详细记录",details.toString())+
            sheet("事故统计表",stats.toString())+"</Workbook>";
        File dir=new File(context.getCacheDir(),"exports"); if(!dir.exists()) dir.mkdirs();
        String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date());
        File file=new File(dir,"生产事故报表_"+stamp+".xls");
        try(OutputStream out=new FileOutputStream(file)) { out.write(xml.getBytes(StandardCharsets.UTF_8)); }
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
        void newPage() {
            if(page!=null) pdf.finishPage(page);
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
        void finish() { if(page!=null){pdf.finishPage(page);page=null;} }
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
