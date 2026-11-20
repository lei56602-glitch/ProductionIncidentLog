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
                cause.getText().toString(),action.getText().toString(),prevention.getText().toString());
            Toast.makeText(this,"保存成功："+code,Toast.LENGTH_LONG).show(); dashboard();
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
                        +"\n\n当下措施："+r[10]+"\n\n未来避免："+r[11]+"\n\n状态："+r[12];
                    TextView detailView = new TextView(this); detailView.setText(detail); detailView.setTextSize(16); detailView.setPadding(24,20,24,20);
                    ScrollView sv = new ScrollView(this); sv.addView(detailView);
                    new AlertDialog.Builder(this).setTitle(r[0]).setView(sv).setPositiveButton("关闭",null).show();
                }));
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
        page("统计概览");
        root.addView(card("事故总数",db.countAll()+" 起"));
        for (String t : new String[]{"设备损伤","物料混料","人员受伤"}) root.addView(card(t,db.countType(t)+" 起"));
        List<String[]> rows = db.incidents("");
        for (String team : new String[]{"A班","B班"}) {
            int n=0; for(String[] r:rows) if(team.equals(r[2])) n++;
            root.addView(card(team,n+" 起"));
        }
        for (String area : new String[]{"造粒","仓库"}) {
            int n=0; for(String[] r:rows) if(area.equals(r[3])) n++;
            root.addView(card(area,n+" 起"));
        }
        root.addView(button("返回首页",this::dashboard));
    }
    @Override protected void onDestroy() { if (db!=null) db.close(); super.onDestroy(); }
}
