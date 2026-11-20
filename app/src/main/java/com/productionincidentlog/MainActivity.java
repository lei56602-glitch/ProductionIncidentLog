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
    private static final int REQUEST_BACKUP = 4103;
    private static final int REQUEST_RESTORE = 4104;
    private TextView attachmentHint;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); db = new IncidentDb(this); dashboard();
        IncidentReminderScheduler.schedule(this);
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},7304);
        }
    }
    private void checkRemindersNow() {
        if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},7304);
            Toast.makeText(this,"请允许通知权限后再检查提醒",Toast.LENGTH_LONG).show();
            return;
        }
        sendBroadcast(new android.content.Intent(this,IncidentReminderReceiver.class));
        Toast.makeText(this,"已检查整改期限；如有逾期或临期任务，将显示系统通知",Toast.LENGTH_LONG).show();
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
    private boolean validDueDate(String value) {
        if(value==null || value.trim().isEmpty()) return true;
        try {
            SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
            format.setLenient(false);
            return format.format(format.parse(value.trim())).equals(value.trim());
        } catch(Exception e) { return false; }
    }

    private void dashboard() {
        page("生产事故管理");
        TextView sub = new TextView(this); sub.setText("现场事故登记 · 整改跟踪 · 数据统计");
        sub.setTextColor(0xFF60758A); root.addView(sub);
        LinearLayout counts = new LinearLayout(this); counts.setOrientation(LinearLayout.HORIZONTAL);
        counts.addView(card("事故总数", ""+db.countAll()), new LinearLayout.LayoutParams(0,-2,1));
        counts.addView(card("待整改", ""+db.countStatus("待整改")), new LinearLayout.LayoutParams(0,-2,1));
        root.addView(counts);
        String today = new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
        List<String[]> overdue = db.incidentsDue(today,0,true);
        List<String[]> upcoming = db.incidentsDue(today,7,false);
        root.addView(card("逾期未完成整改",db.countOverdue(today)+" 起"));
        if(!overdue.isEmpty()) root.addView(button("查看逾期事故（"+overdue.size()+"）",() -> showFilteredRecords("逾期未完成整改",overdue)));
        if(!upcoming.isEmpty()) root.addView(button("未来 7 天到期（"+upcoming.size()+"）",() -> showFilteredRecords("未来 7 天到期整改",upcoming)));
        root.addView(card("设备损伤", db.countType("设备损伤")+" 起"));
        root.addView(card("物料混料", db.countType("物料混料")+" 起"));
        root.addView(card("人员受伤", db.countType("人员受伤")+" 起"));
        root.addView(button("＋ 登记新事故", this::incidentForm));
        root.addView(button("事故档案", this::records));
        root.addView(button("设备档案管理", this::equipment));
        root.addView(button("统计概览", this::statistics));
        root.addView(button("事故报表中心（Excel / PDF）", this::exportCenter));
        root.addView(button("备份全部数据（含照片 / 视频）",this::startBackup));
        root.addView(button("从备份恢复数据",this::chooseRestore));
        root.addView(button("立即检查整改提醒",this::checkRemindersNow));
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
            if(!validDueDate(dueDate.getText().toString())) {
                Toast.makeText(this,"整改期限格式错误，请使用真实日期 yyyy-MM-dd",Toast.LENGTH_LONG).show(); return;
            }
            String code = db.addIncident(date.getText().toString(),team.getSelectedItem().toString(),
                area.getSelectedItem().toString(),type.getSelectedItem().toString(),equipment.getSelectedItem().toString(),
                people.getText().toString(),description.getText().toString(),analysis.getText().toString(),
                cause.getText().toString(),action.getText().toString(),prevention.getText().toString(),owner.getText().toString(),dueDate.getText().toString());
            for (android.net.Uri uri : pendingAttachments) {
                String mime = getContentResolver().getType(uri);
                try {
                    String savedUri=copyAttachmentToPrivate(uri,code,mime);
                    db.addAttachment(code,savedUri,mime==null?"application/octet-stream":mime);
                } catch(Exception e) {
                    Toast.makeText(this,"附件保存失败："+e.getMessage(),Toast.LENGTH_LONG).show();
                }
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
                    StringBuilder historyText = new StringBuilder("\n\n整改状态变更记录：");
                    List<String[]> history = db.statusHistory(r[0]);
                    if (history.isEmpty()) historyText.append("\n暂无状态变更记录");
                    else for (String[] event : history) historyText.append("\n").append(event[2]).append("  ").append(event[0]).append(" → ").append(event[1]);
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
            if(!validDueDate(dueDate.getText().toString())) {
                Toast.makeText(this,"整改期限格式错误，请使用真实日期 yyyy-MM-dd",Toast.LENGTH_LONG).show(); return;
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
        ScrollView scroll=new ScrollView(this);
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); scroll.addView(body);
        String today=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());
        EditText from=field(body,"开始日期（yyyy-MM-dd）","留空表示不限",today.substring(0,8)+"01",1);
        EditText to=field(body,"结束日期（yyyy-MM-dd）","留空表示不限",today,1);
        Spinner team=spinner(body,"班组",java.util.Arrays.asList("全部班组","A班","B班"));
        Spinner area=spinner(body,"区域",java.util.Arrays.asList("全部区域","造粒","仓库"));
        Spinner type=spinner(body,"事故类型",java.util.Arrays.asList("全部类型","设备损伤","物料混料","人员受伤"));
        List<String> equipmentOptions=new java.util.ArrayList<>(); equipmentOptions.add("全部设备"); equipmentOptions.addAll(db.equipmentNames());
        Spinner equipment=spinner(body,"设备",equipmentOptions);
        LinearLayout result=new LinearLayout(this); result.setOrientation(LinearLayout.VERTICAL);
        Runnable calculate=() -> {
            result.removeAllViews();
            String f=from.getText().toString().trim(),t=to.getText().toString().trim();
            if((!f.isEmpty()&&!f.matches("\\d{4}-\\d{2}-\\d{2}"))||(!t.isEmpty()&&!t.matches("\\d{4}-\\d{2}-\\d{2}"))||(!f.isEmpty()&&!t.isEmpty()&&f.compareTo(t)>0)) {
                Toast.makeText(this,"请检查日期格式及起止顺序（yyyy-MM-dd）",Toast.LENGTH_SHORT).show(); return;
            }
            List<String[]> rows=db.incidentsFiltered(f,t,team.getSelectedItem().toString(),area.getSelectedItem().toString(),type.getSelectedItem().toString(),equipment.getSelectedItem().toString());
            result.addView(button("事故总数："+rows.size()+" 起（点击查看明细）",() -> showFilteredRecords("全部筛选事故",rows)));
            addStatisticGroup(result,"事故类型",rows,4,new String[]{"设备损伤","物料混料","人员受伤"});
            addStatisticGroup(result,"班组分布",rows,2,new String[]{"A班","B班"});
            addStatisticGroup(result,"区域分布",rows,3,new String[]{"造粒","仓库"});
            java.util.Map<String,Integer> devices=new java.util.TreeMap<>();
            java.util.Map<String,Integer> months=new java.util.TreeMap<>();
            for(String[] r:rows) {
                devices.put(r[5],devices.containsKey(r[5])?devices.get(r[5])+1:1);
                String d=r[1]==null?"":r[1]; String m=d.length()>=7?d.substring(0,7):"日期未知";
                months.put(m,months.containsKey(m)?months.get(m)+1:1);
            }
            result.addView(sectionTitle("设备事故排行（点击查看明细）"));
            if(devices.isEmpty()) result.addView(card("设备排行","暂无记录"));
            else {
                java.util.List<java.util.Map.Entry<String,Integer>> ranked=new java.util.ArrayList<>(devices.entrySet());
                java.util.Collections.sort(ranked,(x,y)->y.getValue().compareTo(x.getValue()));
                int size=Math.min(8,ranked.size()); String[] labels=new String[size]; int[] values=new int[size];
                for(int i=0;i<size;i++){labels[i]=ranked.get(i).getKey();values[i]=ranked.get(i).getValue();}
                result.addView(new IncidentBarChart(this,labels,values));
                for(java.util.Map.Entry<String,Integer> e:ranked) {
                    String deviceName=e.getKey();
                    result.addView(button(deviceName+"："+e.getValue()+" 起",() -> showFilteredRecords("设备："+deviceName,filterRows(rows,5,deviceName))));
                }
            }
            result.addView(sectionTitle("事故发生趋势（月度）"));
            if(months.isEmpty()) result.addView(card("事故趋势","暂无记录"));
            else {
                int offset=Math.max(0,months.size()-12),index=0,k=0;
                String[] labels=new String[Math.min(12,months.size())]; int[] values=new int[labels.length];
                for(java.util.Map.Entry<String,Integer> e:months.entrySet()) {
                    if(index++<offset) continue; labels[k]=e.getKey(); values[k]=e.getValue(); k++;
                }
                result.addView(new IncidentBarChart(this,labels,values));
            }
        };
        body.addView(button("应用筛选并统计",calculate)); body.addView(result); calculate.run();
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); root.addView(button("返回首页",this::dashboard));
    }

    private TextView sectionTitle(String title) {
        TextView t=new TextView(this); t.setText(title); t.setTextColor(ink); t.setTextSize(18);
        t.setTypeface(null,Typeface.BOLD); t.setPadding(4,18,4,8); return t;
    }
    private void addStatisticGroup(LinearLayout target,String heading,List<String[]> rows,int column,String[] categories) {
        target.addView(sectionTitle(heading+"（点击查看明细）"));
        int[] values=new int[categories.length];
        for(int i=0;i<categories.length;i++) {
            for(String[] r:rows) if(categories[i].equals(r[column])) values[i]++;
        }
        if(column==2) target.addView(new IncidentBarChart(this,categories,values));
        for(int i=0;i<categories.length;i++) {
            final String category=categories[i]; final int count=values[i];
            target.addView(button(category+"："+count+" 起",() -> showFilteredRecords(heading+"："+category,filterRows(rows,column,category))));
        }
    }

    private List<String[]> filterRows(List<String[]> source,int column,String value) {
        List<String[]> out=new java.util.ArrayList<>();
        for(String[] r:source) if(value.equals(r[column])) out.add(r);
        return out;
    }
    private void showFilteredRecords(String title,List<String[]> rows) {
        page(title+" · "+rows.size()+" 起");
        ScrollView scroll=new ScrollView(this);
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        for(String[] r:rows) {
            list.addView(card(r[0]+" · "+r[4],r[1]+"\n"+r[2]+" / "+r[3]+" / "+r[5]+"\n"+r[7]));
            list.addView(button("查看事故详情",() -> {
                String detail=r[0]+"\n时间："+r[1]+"\n班组："+r[2]+"\n区域："+r[3]+"\n类型："+r[4]+"\n设备："+r[5]
                    +"\n涉事人员："+r[6]+"\n\n事故经过："+r[7]+"\n\n事故分析："+r[8]+"\n\n事故原因："+r[9]
                    +"\n\n当下措施："+r[10]+"\n\n未来避免："+r[11]+"\n\n整改负责人："+r[12]+"\n整改期限："+r[13]+"\n整改状态："+r[14];
                new AlertDialog.Builder(this).setTitle(r[0]).setMessage(detail).setPositiveButton("关闭",null)
                    .setNegativeButton("查看附件（"+db.attachments(r[0]).size()+"）",(d,w)->showAttachments(r[0])).show();
            }));
        }
        if(rows.isEmpty()) list.addView(card("暂无明细","当前分类没有事故记录"));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); root.addView(button("返回统计",this::statistics));
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


    private java.io.File attachmentDirectory() {
        java.io.File dir=new java.io.File(getFilesDir(),"incident_attachments");
        if(!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("无法创建附件目录");
        return dir;
    }
    private String copyAttachmentToPrivate(android.net.Uri source,String code,String mime) throws java.io.IOException {
        String safeCode=code.replaceAll("[^A-Za-z0-9_-]","_");
        java.io.File dir=new java.io.File(attachmentDirectory(),safeCode);
        if(!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("无法创建事故附件目录");
        String ext=android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        if(ext==null || ext.isEmpty()) ext="bin";
        java.io.File target=java.io.File.createTempFile("att_", "."+ext,dir);
        try(java.io.InputStream in=getContentResolver().openInputStream(source);
            java.io.OutputStream out=new java.io.FileOutputStream(target)) {
            if(in==null) throw new java.io.IOException("无法读取附件");
            byte[] buffer=new byte[8192]; int n;
            while((n=in.read(buffer))>0) out.write(buffer,0,n);
        } catch(Exception e) {
            target.delete();
            if(e instanceof java.io.IOException) throw (java.io.IOException)e;
            throw new java.io.IOException(e);
        }
        return androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".fileprovider",target).toString();
    }
    private void startBackup() {
        android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(android.content.Intent.EXTRA_TITLE,"ProductionIncidentLog-Backup-"+
            new SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(new Date())+".zip");
        startActivityForResult(intent,REQUEST_BACKUP);
    }
    private void chooseRestore() {
        new AlertDialog.Builder(this).setTitle("恢复事故数据")
            .setMessage("恢复将用备份中的数据库和照片/视频替换本机当前数据。请先备份当前数据。")
            .setNegativeButton("取消",null).setPositiveButton("选择备份",(d,w)->{
                android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);
                intent.setType("application/zip");
                startActivityForResult(intent,REQUEST_RESTORE);
            }).show();
    }
    private void zipFile(java.util.zip.ZipOutputStream zip,java.io.File file,String name) throws java.io.IOException {
        if(file.isDirectory()) {
            java.io.File[] children=file.listFiles();
            if(children!=null) for(java.io.File child:children)
                zipFile(zip,child,name+child.getName()+(child.isDirectory()?"/":""));
            return;
        }
        zip.putNextEntry(new java.util.zip.ZipEntry(name));
        try(java.io.InputStream in=new java.io.FileInputStream(file)) {
            byte[] buffer=new byte[8192]; int n;
            while((n=in.read(buffer))>0) zip.write(buffer,0,n);
        }
        zip.closeEntry();
    }
    private String sha256(java.io.File file) throws Exception {
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
        try(java.io.InputStream in=new java.io.FileInputStream(file)) {
            byte[] buffer=new byte[8192]; int n;
            while((n=in.read(buffer))>0) digest.update(buffer,0,n);
        }
        StringBuilder out=new StringBuilder();
        for(byte b:digest.digest()) out.append(String.format(Locale.US,"%02x",b&0xff));
        return out.toString();
    }
    private long[] attachmentStats(java.io.File dir) {
        long count=0,bytes=0;
        java.io.File[] children=dir.listFiles();
        if(children!=null) for(java.io.File child:children) {
            if(child.isDirectory()) {
                long[] sub=attachmentStats(child); count+=sub[0]; bytes+=sub[1];
            } else { count++; bytes+=child.length(); }
        }
        return new long[]{count,bytes};
    }
    private void writeZipText(java.util.zip.ZipOutputStream zip,String name,String value) throws java.io.IOException {
        zip.putNextEntry(new java.util.zip.ZipEntry(name));
        zip.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zip.closeEntry();
    }
    private void appendAttachmentHashes(java.io.File root,java.io.File current,org.json.JSONArray output) throws Exception {
        java.io.File[] children=current.listFiles();
        if(children==null) return;
        for(java.io.File child:children) {
            if(child.isDirectory()) appendAttachmentHashes(root,child,output);
            else {
                String relative=root.toURI().relativize(child.toURI()).getPath();
                org.json.JSONObject item=new org.json.JSONObject();
                item.put("path",relative);
                item.put("size",child.length());
                item.put("sha256",sha256(child));
                output.put(item);
            }
        }
    }
    private void validateAttachmentHashes(java.io.File root,org.json.JSONArray expected) throws Exception {
        java.util.HashSet<String> seen=new java.util.HashSet<>();
        for(int i=0;i<expected.length();i++) {
            org.json.JSONObject item=expected.getJSONObject(i);
            String relative=item.optString("path","");
            if(relative.isEmpty() || relative.startsWith("/") || relative.contains("..") || relative.contains("\\") || !seen.add(relative))
                throw new java.io.IOException("附件校验清单路径无效");
            java.io.File file=new java.io.File(root,relative);
            if(!file.getCanonicalPath().startsWith(root.getCanonicalPath()+java.io.File.separator) || !file.isFile())
                throw new java.io.IOException("备份缺少附件："+relative);
            if(file.length()!=item.optLong("size",-1) || !sha256(file).equals(item.optString("sha256","")))
                throw new java.io.IOException("附件 SHA-256 校验失败："+relative);
        }
        long[] actual=attachmentStats(root);
        if(actual[0]!=expected.length()) throw new java.io.IOException("备份包含未登记或缺失的附件文件");
    }
    private void exportBackup(android.net.Uri destination) {
        boolean closed=false;
        try {
            String ownAuthority=getPackageName()+".fileprovider";
            for(String[] attachment:db.allAttachments()) {
                android.net.Uri oldUri=android.net.Uri.parse(attachment[1]);
                if(ownAuthority.equals(oldUri.getAuthority())) continue;
                String newUri=copyAttachmentToPrivate(oldUri,attachment[0],attachment[2]);
                db.updateAttachmentUri(attachment[0],attachment[1],newUri);
            }
            db.getWritableDatabase().execSQL("PRAGMA wal_checkpoint(FULL)");
            db.close(); closed=true;
            java.io.File database=getDatabasePath("production_incidents.db");
            if(!database.isFile()) throw new java.io.IOException("数据库文件不存在，无法备份");
            long[] stats=attachmentStats(attachmentDirectory());
            org.json.JSONObject manifestObject=new org.json.JSONObject();
            manifestObject.put("format_version",1);
            manifestObject.put("database","production_incidents.db");
            manifestObject.put("database_sha256",sha256(database));
            manifestObject.put("attachment_file_count",stats[0]);
            manifestObject.put("attachment_total_bytes",stats[1]);
            org.json.JSONArray attachmentHashes=new org.json.JSONArray();
            appendAttachmentHashes(attachmentDirectory(),attachmentDirectory(),attachmentHashes);
            manifestObject.put("attachments",attachmentHashes);
            String manifest=manifestObject.toString();
            java.io.OutputStream raw=getContentResolver().openOutputStream(destination);
            if(raw==null) throw new java.io.IOException("无法创建目标备份文件");
            try(java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(raw)) {
                zipFile(zip,database,"production_incidents.db");
                java.io.File[] dirs=attachmentDirectory().listFiles();
                if(dirs!=null) for(java.io.File dir:dirs)
                    zipFile(zip,dir,"attachments/"+dir.getName()+(dir.isDirectory()?"/":""));
                writeZipText(zip,"backup_manifest.json",manifest);
            }
            Toast.makeText(this,"备份完成：已生成 SHA-256 校验清单，附件 "+stats[0]+" 个",Toast.LENGTH_LONG).show();
        } catch(Exception e) {
            Toast.makeText(this,"备份失败："+e.getMessage(),Toast.LENGTH_LONG).show();
        } finally {
            if(closed) { db=new IncidentDb(this); }
        }
    }
    private void deleteTree(java.io.File file) {
        if(file.isDirectory()) {
            java.io.File[] children=file.listFiles();
            if(children!=null) for(java.io.File child:children) deleteTree(child);
        }
        file.delete();
    }
    private void copyFile(java.io.File source,java.io.File target) throws java.io.IOException {
        java.io.File parent=target.getParentFile();
        if(parent!=null && !parent.exists() && !parent.mkdirs()) throw new java.io.IOException("无法创建恢复目录");
        try(java.io.InputStream in=new java.io.FileInputStream(source);
            java.io.OutputStream out=new java.io.FileOutputStream(target)) {
            byte[] buffer=new byte[8192]; int n;
            while((n=in.read(buffer))>0) out.write(buffer,0,n);
        }
    }
    private void copyTree(java.io.File source,java.io.File target) throws java.io.IOException {
        if(source.isDirectory()) {
            if(!target.exists() && !target.mkdirs()) throw new java.io.IOException("无法创建附件目录");
            java.io.File[] children=source.listFiles();
            if(children!=null) for(java.io.File child:children) copyTree(child,new java.io.File(target,child.getName()));
        } else copyFile(source,target);
    }
    private void restoreBackup(android.net.Uri source) {
        java.io.File temp=new java.io.File(getCacheDir(),"incident_restore");
        deleteTree(temp);
        if(!temp.mkdirs()) { Toast.makeText(this,"无法创建临时恢复目录",Toast.LENGTH_LONG).show(); return; }
        java.io.File safety=new java.io.File(temp,"safety");
        boolean databaseSnapshot=false,attachmentsSnapshot=false,dbClosed=false;
        try {
            try(java.io.InputStream raw=getContentResolver().openInputStream(source)) {
                if(raw==null) throw new java.io.IOException("无法读取备份文件");
                try(java.util.zip.ZipInputStream zip=new java.util.zip.ZipInputStream(raw)) {
                    java.util.zip.ZipEntry entry; int count=0; long total=0;
                    java.util.HashSet<String> names=new java.util.HashSet<>();
                    while((entry=zip.getNextEntry())!=null) {
                        if(++count>5005) throw new java.io.IOException("备份文件条目过多");
                        String name=entry.getName();
                        if(!names.add(name)) throw new java.io.IOException("备份文件包含重复条目");
                        boolean allowed=name.equals("production_incidents.db") || name.equals("backup_manifest.json") || name.startsWith("attachments/");
                        if(name.startsWith("/") || name.contains("..") || name.contains("\\") || !allowed)
                            throw new java.io.IOException("备份文件路径不合法");
                        java.io.File out=new java.io.File(temp,name);
                        if(!out.getCanonicalPath().startsWith(temp.getCanonicalPath()+java.io.File.separator))
                            throw new java.io.IOException("备份文件路径越界");
                        if(entry.isDirectory()) {
                            if(!out.exists() && !out.mkdirs()) throw new java.io.IOException("无法创建备份目录");
                        } else {
                            java.io.File parent=out.getParentFile();
                            if(parent!=null && !parent.exists() && !parent.mkdirs()) throw new java.io.IOException("无法创建备份目录");
                            try(java.io.OutputStream stream=new java.io.FileOutputStream(out)) {
                                byte[] buffer=new byte[8192]; int n;
                                while((n=zip.read(buffer))>0) {
                                    total+=n;
                                    if(total>536870912L) throw new java.io.IOException("备份解压内容超过 512 MB");
                                    stream.write(buffer,0,n);
                                }
                            }
                        }
                        zip.closeEntry();
                    }
                }
            }
            java.io.File restoredDb=new java.io.File(temp,"production_incidents.db");
            java.io.File manifestFile=new java.io.File(temp,"backup_manifest.json");
            if(!restoredDb.isFile() || !manifestFile.isFile()) throw new java.io.IOException("备份缺少数据库或校验清单；旧格式备份暂不支持直接恢复");
            StringBuilder manifestText=new StringBuilder();
            try(java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(
                    new java.io.FileInputStream(manifestFile),java.nio.charset.StandardCharsets.UTF_8))) {
                String line; while((line=reader.readLine())!=null) manifestText.append(line);
            }
            org.json.JSONObject manifest=new org.json.JSONObject(manifestText.toString());
            if(manifest.optInt("format_version",-1)!=1 || !"production_incidents.db".equals(manifest.optString("database")))
                throw new java.io.IOException("备份格式版本不受支持");
            String expectedHash=manifest.optString("database_sha256","");
            if(!expectedHash.matches("[0-9a-f]{64}") || !expectedHash.equals(sha256(restoredDb)))
                throw new java.io.IOException("数据库 SHA-256 校验失败，备份可能已损坏");
            java.io.File restoredAttachments=new java.io.File(temp,"attachments");
            if(!restoredAttachments.exists() && !restoredAttachments.mkdirs()) throw new java.io.IOException("附件目录缺失");
            long[] stats=attachmentStats(restoredAttachments);
            if(stats[0]!=manifest.optLong("attachment_file_count",-1) || stats[1]!=manifest.optLong("attachment_total_bytes",-1))
                throw new java.io.IOException("附件数量或总大小校验失败，拒绝恢复");
            org.json.JSONArray expectedAttachments=manifest.optJSONArray("attachments");
            if(expectedAttachments==null || expectedAttachments.length()!=stats[0])
                throw new java.io.IOException("附件 SHA-256 清单缺失或不完整");
            validateAttachmentHashes(restoredAttachments,expectedAttachments);
            android.database.sqlite.SQLiteDatabase check=android.database.sqlite.SQLiteDatabase.openDatabase(
                restoredDb.getAbsolutePath(),null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY);
            try {
                try(android.database.Cursor cursor=check.rawQuery("PRAGMA integrity_check",null)) {
                    if(!cursor.moveToFirst() || !"ok".equalsIgnoreCase(cursor.getString(0)))
                        throw new java.io.IOException("数据库完整性检查未通过");
                }
                try(android.database.Cursor cursor=check.rawQuery("SELECT COUNT(*) FROM incidents",null)) {
                    if(!cursor.moveToFirst()) throw new java.io.IOException("数据库缺少事故数据表");
                }
                try(android.database.Cursor cursor=check.rawQuery("SELECT COUNT(*) FROM incident_attachments",null)) {
                    if(!cursor.moveToFirst()) throw new java.io.IOException("数据库附件表结构不完整");
                }
            } finally { check.close(); }

            if(!safety.mkdirs()) throw new java.io.IOException("无法创建恢复回滚目录");
            db.getWritableDatabase().execSQL("PRAGMA wal_checkpoint(FULL)");
            db.close(); dbClosed=true;
            java.io.File current=getDatabasePath("production_incidents.db");
            if(!current.isFile()) throw new java.io.IOException("本机数据库不存在，无法创建恢复回滚点");
            copyFile(current,new java.io.File(safety,"current.db")); databaseSnapshot=true;
            copyTree(attachmentDirectory(),new java.io.File(safety,"attachments")); attachmentsSnapshot=true;

            java.io.File stagedDb=new java.io.File(getFilesDir(),"production_incidents.restore.db");
            java.io.File stagedAttachments=new java.io.File(getFilesDir(),"incident_attachments.restore");
            java.io.File attachmentRoot=attachmentDirectory();
            deleteTree(stagedDb); deleteTree(stagedAttachments);
            copyFile(restoredDb,stagedDb);
            copyTree(restoredAttachments,stagedAttachments);
            new java.io.File(current.getPath()+"-wal").delete();
            new java.io.File(current.getPath()+"-shm").delete();
            if(current.exists() && !current.delete()) throw new java.io.IOException("无法替换本机数据库");
            if(!stagedDb.renameTo(current)) throw new java.io.IOException("新数据库安装失败");
            java.io.File oldAttachments=new java.io.File(getFilesDir(),"incident_attachments.previous");
            deleteTree(oldAttachments);
            if(attachmentRoot.exists() && !attachmentRoot.renameTo(oldAttachments))
                throw new java.io.IOException("无法切换附件目录");
            if(!stagedAttachments.renameTo(attachmentRoot))
                throw new java.io.IOException("新附件目录安装失败");
            deleteTree(oldAttachments);
            db=new IncidentDb(this); db.getReadableDatabase(); dbClosed=false;
            deleteTree(temp);
            Toast.makeText(this,"恢复完成：数据库哈希、附件清单及完整性均校验通过",Toast.LENGTH_LONG).show();
            dashboard();
        } catch(Exception e) {
            if(dbClosed) {
                try {
                    if(db!=null) db.close();
                    java.io.File current=getDatabasePath("production_incidents.db");
                    if(databaseSnapshot && new java.io.File(safety,"current.db").isFile()) {
                        new java.io.File(current.getPath()+"-wal").delete();
                        new java.io.File(current.getPath()+"-shm").delete();
                        copyFile(new java.io.File(safety,"current.db"),current);
                    }
                    java.io.File activeAttachments=new java.io.File(getFilesDir(),"incident_attachments");
                    if(attachmentsSnapshot && new java.io.File(safety,"attachments").exists()) {
                        deleteTree(activeAttachments);
                        copyTree(new java.io.File(safety,"attachments"),activeAttachments);
                    }
                } catch(Exception rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                db=new IncidentDb(this);
                try { db.getReadableDatabase(); } catch(Exception ignored) {}
            }
            deleteTree(temp);
            Toast.makeText(this,"恢复失败，已尝试保留并回滚原有数据："+e.getMessage(),Toast.LENGTH_LONG).show();
        }
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
        if(resultCode!=RESULT_OK || data==null) return;
        if(requestCode==REQUEST_BACKUP) { exportBackup(data.getData()); return; }
        if(requestCode==REQUEST_RESTORE) {
            new AlertDialog.Builder(this).setTitle("确认覆盖本机数据")
                .setMessage("将覆盖本机现有事故档案和附件，恢复前请确认已备份当前数据。")
                .setNegativeButton("取消",null)
                .setPositiveButton("确认恢复",(d,w)->restoreBackup(data.getData())).show();
            return;
        }
        if(requestCode!=REQUEST_ATTACHMENTS) return;
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
