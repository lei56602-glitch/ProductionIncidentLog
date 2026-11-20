package com.productionincidentlog;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.*;
import android.graphics.Typeface;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.List;

public class MainActivity extends Activity {
    private IncidentDb db;
    private LinearLayout root;
    private final int ink = 0xFF17324D, blue = 0xFF1769AA, bg = 0xFFF2F7FC;
    private List<android.net.Uri> pendingAttachments = new java.util.ArrayList<>();
    private static final int REQUEST_ATTACHMENTS = 4102;
    private TextView attachmentHint;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); db = new IncidentDb(this); dashboard();
    }
    private void page(String title) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18,18,18,18); root.setBackgroundColor(bg);
        TextView h = new TextView(this); h.setText(title); h.setTextSize(23);
        h.setTextColor(ink); h.setTypeface(null, Typeface.BOLD); h.setPadding(4,12,4,18);
        root.addView(h); setContentView(root);
    }
    private Button button(String text, Runnable run) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setTextColor(blue);
        b.setOnClickListener(v -> run.run()); return b;
    }
    private TextView card(String title, String value) {
        TextView t = new TextView(this); t.setText(title + "\n" + value); t.setTextSize(17);
        t.setTextColor(ink); t.setPadding(18,18,18,18); t.setBackgroundColor(-1);
        return t;
    }
    private void dashboard() {
        page("生产事故管理");
        TextView sub = new TextView(this); sub.setText("现场事故登记 · 整改跟踪 · 数据统计");
        sub.setTextColor(0xFF60758A); root.addView(sub);
        LinearLayout counts = new LinearLayout(this); counts.setOrientation(LinearLayout.HORIZONTAL);
        counts.addView(card("事故总数", ""+db.countAll()), new LinearLayout.LayoutParams(0,-2,1));
        counts.addView(card("待整改", ""+db.countStatus("待整改")), new LinearLayout.LayoutParams(0,-2,1));
        root.addView(counts);
        root.addView(card("设备损伤", db.countType("设备损伤")+" 起"));
        root.addView(card("物料混料", db.countType("物料混料")+" 起"));
        root.addView(card("人员受伤", db.countType("人员受伤")+" 起"));
        root.addView(button("＋ 登记新事故", this::incidentForm));
        root.addView(button("事故档案", this::records));
        root.addView(button("设备档案管理", this::equipment));
        root.addView(button("统计概览", this::statistics));
        root.addView(button("事故报表中心（Excel / PDF）", this::exportCenter));
    }
    private EditText field(LinearLayout box, String label, String hint, String initial, int lines) {
        TextView l = new TextView(this); l.setText(label); l.setTextColor(ink); l.setPadding(2,10,2,3); box.addView(l);
        EditText e = new EditText(this); e.setHint(hint); e.setText(initial); e.setMinLines(lines);
        e.setTextSize(16); e.setPadding(12,8,12,8); e.setBackgroundColor(-1); box.addView(e); return e;
    }
    private Spinner spinner(LinearLayout box, String label, List<String> values) {
        TextView l = new TextView(this); l.setText(label); l.setTextColor(ink); l.setPadding(2,10,2,3); box.addView(l);
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values)); box.addView(s); return s;
    }
    private void incidentForm() {
        pendingAttachments = new java.util.ArrayList<>();
        page("登记生产事故");
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); scroll.addView(form);
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        EditText date = field(form,"发生日期与时间","例如：2026-11-20 14:30",now,1);
        Spinner team = spinner(form,"班组",java.util.Arrays.asList("A班","B班"));
        Spinner area = spinner(form,"发生区域",java.util.Arrays.asList("造粒","仓库"));
        Spinner type = spinner(form,"事故类型",java.util.Arrays.asList("设备损伤","物料混料","人员受伤"));
        List<String> equipmentNames = db.equipmentNames();
        if (equipmentNames.isEmpty()) equipmentNames.add("未指定");
        Spinner equipment = spinner(form,"设备名称（从设备档案选择）",equipmentNames);
        EditText people = field(form,"涉事人员","人员姓名或工号；注意保护伤者隐私","",1);
        EditText description = field(form,"一、事故经过","客观描述事故发生过程","",3);
        EditText analysis = field(form,"二、事故分析","分析事故过程及影响","",3);
        EditText cause = field(form,"三、事故原因","填写直接原因和根本原因","",3);
        EditText action = field(form,"四、当下采取措施","现场处置和临时控制措施","",3);
        EditText prevention = field(form,"五、未来如何避免","长期整改和预防措施","",3);
        EditText owner = field(form,"整改负责人","负责推动整改的人","",1);
        EditText dueDate = field(form,"整改期限（yyyy-MM-dd）","例如：2026-11-30","",1);
        attachmentHint = new TextView(this); attachmentHint.setText("现场照片 / 视频：尚未选择"); attachmentHint.setTextColor(ink); form.addView(attachmentHint);
        form.addView(button("选择现场照片或视频（可多选）", () -> {
            android.content.Intent pick = new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(android.content.Intent.CATEGORY_OPENABLE); pick.setType("*/*");
            pick.putExtra(android.content.Intent.EXTRA_MIME_TYPES, new String[]{"image/*","video/*"});
            pick.putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true);
            pick.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(pick, REQUEST_ATTACHMENTS);
        }));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(button("保存事故档案", () -> {
            if (date.getText().toString().trim().isEmpty() || description.getText().toString().trim().isEmpty()
                || analysis.getText().toString().trim().isEmpty() || cause.getText().toString().trim().isEmpty()
                || action.getText().toString().trim().isEmpty() || prevention.getText().toString().trim().isEmpty()) {
                Toast.makeText(this,"请填写发生时间及五项事故内容",Toast.LENGTH_LONG).show(); return;
            }
            String code = db.addIncident(date.getText().toString(),team.getSelectedItem().toString(),
                area.getSelectedItem().toString(),type.getSelectedItem().toString(),equipment.getSelectedItem().toString(),
                people.getText().toString(),description.getText().toString(),analysis.getText().toString(),
                cause.getText().toString(),action.getText().toString(),prevention.getText().toString(),owner.getText().toString(),dueDate.getText().toString());
            for (android.net.Uri uri : pendingAttachments) {
                try { getContentResolver().takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (SecurityException ignored) { }
                String mime = getContentResolver().getType(uri);
                db.addAttachment(code, uri.toString(), mime == null ? "application/octet-stream" : mime);
            }
            Toast.makeText(this,"保存成功："+code+"，附件 "+pendingAttachments.size()+" 个",Toast.LENGTH_LONG).show(); dashboard();
        }));
        root.addView(button("返回首页",this::dashboard));
    }
    private void records() {
        page("事故档案");
        EditText search = new EditText(this); search.setHint("搜索编号、设备、类型或事故经过"); root.addView(search);
        LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this); scroll.addView(list);
        Runnable refresh = () -> {
            list.removeAllViews();
            List<String[]> rows = db.incidents(search.getText().toString());
            for (String[] r : rows) {
                list.addView(card(r[0]+" · "+r[4],r[1]+"\n"+r[2]+" / "+r[3]+" / "+r[5]+"\n"+r[7]));
                list.addView(button("查看事故详情", () -> {
                    String detail = r[0]+"\n时间："+r[1]+"\n班组："+r[2]+"\n区域："+r[3]+"\n类型："+r[4]+"\n设备："+r[5]+"\n涉事人员："+r[6]
                        +"\n\n事故经过："+r[7]+"\n\n事故分析："+r[8]+"\n\n事故原因："+r[9]
                        +"\n\n当下措施："+r[10]+"\n\n未来避免："+r[11]+"\n\n整改负责人："+r[12]+"\n整改期限："+r[13]+"\n整改状态："+r[14];
                    StringBuilder historyText = new StringBuilder("\\n\\n整改状态变更记录：");
                    List<String[]> history = db.statusHistory(r[0]);
                    if (history.isEmpty()) historyText.append("\\n暂无状态变更记录");
                    else for (String[] event : history) historyText.append("\\n").append(event[2]).append("  ").append(event[0]).append(" → ").append(event[1]);
                    detail += historyText.toString();
                    TextView detailView = new TextView(this); detailView.setText(detail); detailView.setTextSize(16); detailView.setPadding(24,20,24,20);
                    ScrollView sv = new ScrollView(this); sv.addView(detailView);
                    List<String[]> attachments = db.attachments(r[0]);
                    new AlertDialog.Builder(this).setTitle(r[0]).setView(sv)
                        .setPositiveButton("关闭",null)
                        .setNegativeButton("查看附件（"+attachments.size()+"）",(dialog,which) -> showAttachments(r[0]))
                        .setNeutralButton("更新整改状态",(dialog,which) -> {
                            String[] states={"待整改","整改中","已完成"};
                            new AlertDialog.Builder(this).setTitle("选择整改状态")
                                .setItems(states,(d2,index) -> {
                                    db.updateStatus(r[0],states[index]);
                                    Toast.makeText(this,"整改状态已更新",Toast.LENGTH_SHORT).show();
                                    records();
                                }).show();
                        }).show();
                }));
                list.addView(button("编辑事故档案", () -> editIncident(r)));
                list.addView(button("删除事故档案", () -> new AlertDialog.Builder(this)
                    .setTitle("确认删除事故")
                    .setMessage("删除后将同时删除该事故关联的照片和视频附件，且无法恢复。\n\n事故编号："+r[0])
                    .setNegativeButton("取消",null)
                    .setPositiveButton("确认删除",(dialog,which) -> {
                        db.deleteIncident(r[0]);
                        Toast.makeText(this,"事故档案已删除",Toast.LENGTH_SHORT).show();
                        records();
                    }).show()));
            }
            if (rows.isEmpty()) list.addView(card("暂无事故记录","登记事故后会显示在这里"));
        };
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) { refresh.run(); }
            public void afterTextChanged(Editable e) {}
        });
        refresh.run(); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(button("返回首页",this::dashboard));
    }

    private void editIncident(String[] r) {
        page("编辑事故档案");
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); scroll.addView(form);
        EditText date = field(form,"发生日期与时间","yyyy-MM-dd HH:mm",r[1],1);
        Spinner team = spinner(form,"班组",java.util.Arrays.asList("A班","B班"));
        team.setSelection(Math.max(0,java.util.Arrays.asList("A班","B班").indexOf(r[2])));
        Spinner area = spinner(form,"发生区域",java.util.Arrays.asList("造粒","仓库"));
        area.setSelection(Math.max(0,java.util.Arrays.asList("造粒","仓库").indexOf(r[3])));
        Spinner type = spinner(form,"事故类型",java.util.Arrays.asList("设备损伤","物料混料","人员受伤"));
        type.setSelection(Math.max(0,java.util.Arrays.asList("设备损伤","物料混料","人员受伤").indexOf(r[4])));
        List<String> equipmentNames = db.equipmentNames();
        if (!equipmentNames.contains(r[5])) equipmentNames.add(0,r[5]);
        if (equipmentNames.isEmpty()) equipmentNames.add("未指定");
        Spinner equipment = spinner(form,"设备名称",equipmentNames);
        equipment.setSelection(Math.max(0,equipmentNames.indexOf(r[5])));
        EditText people = field(form,"涉事人员","人员姓名或工号",r[6],1);
        EditText description = field(form,"一、事故经过","客观描述事故发生过程",r[7],3);
        EditText analysis = field(form,"二、事故分析","分析事故过程及影响",r[8],3);
        EditText cause = field(form,"三、事故原因","直接原因和根本原因",r[9],3);
        EditText action = field(form,"四、当下采取措施","现场处置和临时控制措施",r[10],3);
        EditText prevention = field(form,"五、未来如何避免","长期整改和预防措施",r[11],3);
        EditText owner = field(form,"整改负责人","负责推动整改的人",r[12],1);
        EditText dueDate = field(form,"整改期限（yyyy-MM-dd）","例如：2026-11-30",r[13],1);
        TextView status = new TextView(this); status.setText("当前整改状态："+r[14]+"（状态可在详情中单独更新）"); status.setTextColor(ink); status.setPadding(4,12,4,12); form.addView(status);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(button("保存修改",() -> {
            String d=date.getText().toString().trim();
            if(d.isEmpty() || description.getText().toString().trim().isEmpty() || analysis.getText().toString().trim().isEmpty()
                || cause.getText().toString().trim().isEmpty() || action.getText().toString().trim().isEmpty()
                || prevention.getText().toString().trim().isEmpty()) {
                Toast.makeText(this,"请填写发生时间及五项事故内容",Toast.LENGTH_LONG).show(); return;
            }
            db.updateIncident(r[0],d,team.getSelectedItem().toString(),area.getSelectedItem().toString(),
                type.getSelectedItem().toString(),equipment.getSelectedItem().toString(),people.getText().toString(),
                description.getText().toString(),analysis.getText().toString(),cause.getText().toString(),
                action.getText().toString(),prevention.getText().toString(),owner.getText().toString(),dueDate.getText().toString());
            Toast.makeText(this,"事故档案修改成功："+r[0],Toast.LENGTH_LONG).show();
            records();
        }));
        root.addView(button("取消返回档案",this::records));
    }

    private void equipment() {
        page("设备档案管理");
        TextView note = new TextView(this); note.setText("事故登记时从统一设备档案选择，避免同一设备出现多个名称。"); note.setTextColor(ink); root.addView(note);
        EditText name = field(root,"新增设备名称","例如：输送机","",1);
        root.addView(button("添加设备",() -> {
            if (db.addEquipment(name.getText().toString())) { Toast.makeText(this,"设备已添加",Toast.LENGTH_SHORT).show(); equipment(); }
            else Toast.makeText(this,"名称为空或设备已存在",Toast.LENGTH_SHORT).show();
        }));
        TextView heading = new TextView(this); heading.setText("现有设备"); heading.setTextColor(ink); heading.setTextSize(18); root.addView(heading);
        for (String n : db.equipmentNames()) root.addView(card("设备",n));
        root.addView(button("返回首页",this::dashboard));
    }
    private void statistics() {
        page("多条件事故统计");
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String monthStart = today.substring(0,8) + "01";
        EditText from = field(body,"开始日期（yyyy-MM-dd）","2026-11-01",monthStart,1);
        EditText to = field(body,"结束日期（yyyy-MM-dd）","2026-11-30",today,1);
        Spinner team = spinner(body,"班组",java.util.Arrays.asList("全部班组","A班","B班"));
        Spinner area = spinner(body,"区域",java.util.Arrays.asList("全部区域","造粒","仓库"));
        Spinner type = spinner(body,"事故类型",java.util.Arrays.asList("全部类型","设备损伤","物料混料","人员受伤"));
        List<String> equipmentOptions = new java.util.ArrayList<>();
        equipmentOptions.add("全部设备"); equipmentOptions.addAll(db.equipmentNames());
        Spinner equipment = spinner(body,"设备",equipmentOptions);
        LinearLayout result = new LinearLayout(this); result.setOrientation(LinearLayout.VERTICAL);
        Runnable calculate = () -> {
            result.removeAllViews();
            String startDate=from.getText().toString().trim(), endDate=to.getText().toString().trim();
            if ((!startDate.isEmpty() && !startDate.matches("\\d{4}-\\d{2}-\\d{2}")) ||
                (!endDate.isEmpty() && !endDate.matches("\\d{4}-\\d{2}-\\d{2}"))) {
                Toast.makeText(this,"日期格式请使用 yyyy-MM-dd",Toast.LENGTH_SHORT).show(); return;
            }
            List<String[]> rows=db.incidentsFiltered(startDate,endDate,team.getSelectedItem().toString(),
                area.getSelectedItem().toString(),type.getSelectedItem().toString(),equipment.getSelectedItem().toString());
            result.addView(card("筛选结果",rows.size()+" 起事故"));
            for(String t:new String[]{"设备损伤","物料混料","人员受伤"}) {
                int n=0; for(String[] r:rows) if(t.equals(r[4])) n++;
                result.addView(card(t,n+" 起"));
            }
            for(String t:new String[]{"A班","B班"}) {
                int n=0; for(String[] r:rows) if(t.equals(r[2])) n++;
                result.addView(card(t,n+" 起"));
            }
            for(String t:new String[]{"造粒","仓库"}) {
                int n=0; for(String[] r:rows) if(t.equals(r[3])) n++;
                result.addView(card(t,n+" 起"));
            }
            java.util.Map<String,Integer> ranking=new java.util.TreeMap<>();
            for(String[] r:rows) ranking.put(r[5],ranking.containsKey(r[5])?ranking.get(r[5])+1:1);
            result.addView(card("设备事故分布",ranking.isEmpty()?"暂无记录":ranking.toString()));
        };
        body.addView(button("应用筛选并统计",calculate));
        body.addView(result);
        calculate.run();
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(button("返回首页",this::dashboard));
    }

    private void exportCenter() {
        page("事故报表中心");
        TextView tip = new TextView(this);
        tip.setText("按发生日期筛选事故。Excel 文件包含事故汇总、详细记录、统计分析三个工作表；PDF 用于查看和打印。");
        tip.setTextColor(ink); tip.setTextSize(15); root.addView(tip);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String monthStart = today.substring(0,8) + "01";
        EditText from = field(root,"开始日期（yyyy-MM-dd）","留空表示不限",monthStart,1);
        EditText to = field(root,"结束日期（yyyy-MM-dd）","留空表示不限",today,1);
        Spinner team = spinner(root,"班组",java.util.Arrays.asList("全部班组","A班","B班"));
        Spinner area = spinner(root,"区域",java.util.Arrays.asList("全部区域","造粒","仓库"));
        Spinner type = spinner(root,"事故类型",java.util.Arrays.asList("全部类型","设备损伤","物料混料","人员受伤"));
        List<String> equipmentOptions = new java.util.ArrayList<>();
        equipmentOptions.add("全部设备"); equipmentOptions.addAll(db.equipmentNames());
        Spinner equipment = spinner(root,"设备名称",equipmentOptions);
        TextView count = new TextView(this); count.setText("当前符合条件：待查询"); count.setTextColor(ink); root.addView(count);
        final List<String[]>[] selected = new List[]{new java.util.ArrayList<String[]>()};
        Runnable load = () -> {
            String f=from.getText().toString().trim(), t=to.getText().toString().trim();
            if ((!f.isEmpty() && !f.matches("\\d{4}-\\d{2}-\\d{2}")) ||
                (!t.isEmpty() && !t.matches("\\d{4}-\\d{2}-\\d{2}"))) {
                Toast.makeText(this,"日期格式请使用 yyyy-MM-dd",Toast.LENGTH_SHORT).show(); return;
            }
            if (!f.isEmpty() && !t.isEmpty() && f.compareTo(t)>0) {
                Toast.makeText(this,"开始日期不能晚于结束日期",Toast.LENGTH_SHORT).show(); return;
            }
            selected[0]=db.incidentsFiltered(f,t,team.getSelectedItem().toString(),
                area.getSelectedItem().toString(),type.getSelectedItem().toString(),equipment.getSelectedItem().toString());
            count.setText("当前符合条件："+selected[0].size()+" 起事故");
        };
        root.addView(button("查询报表范围",load));
        root.addView(button("导出 Excel（三个工作表）",() -> {
            load.run();
            try {
                java.io.File file=IncidentExporter.writeExcel(this,selected[0],from.getText().toString(),to.getText().toString());
                shareExport(file,"application/vnd.ms-excel");
            } catch(Exception e) { Toast.makeText(this,"Excel 导出失败："+e.getMessage(),Toast.LENGTH_LONG).show(); }
        }));
        root.addView(button("生成 PDF 事故报告",() -> {
            load.run();
            try {
                java.io.File file=IncidentExporter.writePdf(this,selected[0],from.getText().toString(),to.getText().toString());
                shareExport(file,"application/pdf");
            } catch(Exception e) { Toast.makeText(this,"PDF 导出失败："+e.getMessage(),Toast.LENGTH_LONG).show(); }
        }));
        root.addView(button("返回首页",this::dashboard));
    }

    private void showAttachments(String code) {
        List<String[]> items = db.attachments(code);
        if (items.isEmpty()) { new AlertDialog.Builder(this).setMessage("该事故没有现场照片或视频附件。").setPositiveButton("关闭",null).show(); return; }
        String[] labels = new String[items.size()];
        for (int i=0;i<items.size();i++) labels[i] = (items.get(i)[1].startsWith("video/") ? "视频 " : "照片 / 文件 ") + (i+1);
        new AlertDialog.Builder(this).setTitle("现场附件").setItems(labels,(dialog,index) -> {
            try { android.net.Uri uri=android.net.Uri.parse(items.get(index)[0]); android.content.Intent view=new android.content.Intent(android.content.Intent.ACTION_VIEW); view.setDataAndType(uri,items.get(index)[1]); view.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(view); }
            catch (Exception e) { Toast.makeText(this,"无法打开附件："+e.getMessage(),Toast.LENGTH_LONG).show(); }
        }).setPositiveButton("关闭",null).show();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,android.content.Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=REQUEST_ATTACHMENTS || resultCode!=RESULT_OK || data==null) return;
        if(data.getClipData()!=null) { android.content.ClipData clip=data.getClipData(); for(int i=0;i<clip.getItemCount();i++) addPendingAttachment(clip.getItemAt(i).getUri()); }
        else if(data.getData()!=null) addPendingAttachment(data.getData());
        if (attachmentHint != null) attachmentHint.setText("现场照片 / 视频：已选择 "+pendingAttachments.size()+" 个附件");
        Toast.makeText(this,"已选择 "+pendingAttachments.size()+" 个附件",Toast.LENGTH_SHORT).show();
    }
    private void addPendingAttachment(android.net.Uri uri) { if(uri!=null && !pendingAttachments.contains(uri)) pendingAttachments.add(uri); }
    private void shareExport(java.io.File file, String mime) {
        android.net.Uri uri=androidx.core.content.FileProvider.getUriForFile(this,
            getPackageName()+".fileprovider",file);
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_SEND);
        intent.setType(mime); intent.putExtra(android.content.Intent.EXTRA_STREAM,uri);
        intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(android.content.Intent.createChooser(intent,"分享或保存报表"));
    }

    @Override protected void onDestroy() { if (db!=null) db.close(); super.onDestroy(); }
}
