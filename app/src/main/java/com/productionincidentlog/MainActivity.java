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
import java.util.Calendar;
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

    private android.graphics.drawable.GradientDrawable rounded(int color,int radius) {
        android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    private TextView sectionTitle(String value) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(17); t.setTextColor(ink); t.setTypeface(null,Typeface.BOLD); t.setPadding(4,16,4,8); return t;
    }
    private void page(String title) {
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(14,0,14,10); root.setBackgroundColor(bg);
        LinearLayout header=new LinearLayout(this); header.setGravity(android.view.Gravity.CENTER_VERTICAL); header.setPadding(10,14,10,14); header.setBackground(rounded(0xFF0866D8,24));
        TextView h=new TextView(this); h.setText(title); h.setTextSize(21); h.setTextColor(0xFFFFFFFF); h.setTypeface(null,Typeface.BOLD);
        header.addView(h,new LinearLayout.LayoutParams(0,-2,1)); root.addView(header,new LinearLayout.LayoutParams(-1,-2)); setContentView(root);
        getWindow().setStatusBarColor(0xFF0759C5); getWindow().setNavigationBarColor(0xFFF7FAFE);
    }
    private Button button(String text,Runnable run) {
        Button b=new Button(this); b.setText(text); b.setAllCaps(false); b.setTextColor(0xFFFFFFFF); b.setTextSize(15); b.setTypeface(null,Typeface.BOLD);
        b.setPadding(12,8,12,8); b.setBackground(rounded(0xFF1677EB,18)); b.setElevation(2);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,5,0,5); b.setLayoutParams(lp); b.setOnClickListener(v->run.run()); return b;
    }
    private TextView card(String title,String value) {
        TextView t=new TextView(this); t.setText(title+"\\n"+value); t.setTextSize(16); t.setTextColor(ink); t.setTypeface(null,Typeface.BOLD);
        t.setPadding(16,16,16,16); t.setBackground(rounded(0xFFFFFFFF,22)); t.setElevation(2); return t;
    }
    private void addBottomNav(String selected) {
        LinearLayout nav=new LinearLayout(this); nav.setGravity(android.view.Gravity.CENTER); nav.setBackground(rounded(0xFFFFFFFF,20)); nav.setPadding(3,5,3,5);
        String[] labels={"首页","记录","统计","我的"}; Runnable[] actions={this::dashboard,this::records,this::statistics,this::settingsCenter};
        for(int i=0;i<labels.length;i++){ final int index=i; TextView item=new TextView(this); item.setText(labels[i]); item.setGravity(android.view.Gravity.CENTER);
            item.setTextSize(12); item.setTypeface(null,Typeface.BOLD); item.setTextColor(labels[i].equals(selected)?0xFF126FE5:0xFF8392A5); item.setPadding(4,9,4,9);
            nav.addView(item,new LinearLayout.LayoutParams(0,-2,1)); item.setOnClickListener(v->actions[index].run()); }
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,8,0,0); root.addView(nav,lp);
    }
    private class IndustrialHero extends TextView {
        private final android.graphics.Paint paint=new android.graphics.Paint(3);
        IndustrialHero(android.content.Context context){super(context);setText("生产事故管理系统\\n记录事故 · 分析原因 · 闭环整改");setTextColor(0xFFFFFFFF);setTextSize(21);
            setTypeface(null,Typeface.BOLD);setPadding(20,28,20,28);setMinHeight(180);}
        @Override protected void onDraw(android.graphics.Canvas canvas){
            float w=getWidth(),h=getHeight();
            paint.setShader(new android.graphics.LinearGradient(0,0,w,h,0xFF0B58B8,0xFF258DEB,android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRoundRect(0,0,w,h,24,24,paint);paint.setShader(null);
            paint.setColor(0x553BBAFF);paint.setStyle(android.graphics.Paint.Style.FILL);
            float x=w*0.60f;
            canvas.drawRect(x+18,h*0.30f,x+48,h*0.92f,paint);
            canvas.drawRect(x+62,h*0.18f,x+91,h*0.92f,paint);
            canvas.drawRect(x+108,h*0.38f,x+139,h*0.92f,paint);
            canvas.drawRect(x+154,h*0.24f,x+181,h*0.92f,paint);
            paint.setColor(0x88D8F1FF);paint.setStrokeWidth(4);paint.setStyle(android.graphics.Paint.Style.STROKE);
            canvas.drawLine(x+32,h*0.30f,x+32,h*0.12f,paint);canvas.drawLine(x+76,h*0.18f,x+76,h*0.06f,paint);
            canvas.drawLine(x+122,h*0.38f,x+122,h*0.20f,paint);canvas.drawLine(x+167,h*0.24f,x+167,h*0.10f,paint);
            canvas.drawLine(x+5,h*0.57f,x+190,h*0.57f,paint);canvas.drawLine(x+5,h*0.72f,x+190,h*0.72f,paint);
            paint.setStyle(android.graphics.Paint.Style.FILL);paint.setColor(0x44FFFFFF);
            canvas.drawCircle(x+32,h*0.30f,7,paint);canvas.drawCircle(x+76,h*0.18f,7,paint);canvas.drawCircle(x+122,h*0.38f,7,paint);
            super.onDraw(canvas);
        }
    }
    private void dashboard() {
        page("生产事故管理系统");
        ScrollView scroll=new ScrollView(this); LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); scroll.addView(body);
        IndustrialHero hero=new IndustrialHero(this); hero.setElevation(3); body.addView(hero);
        body.addView(sectionTitle("快速登记"));
        LinearLayout quick=new LinearLayout(this); quick.setOrientation(LinearLayout.HORIZONTAL); String[] types={"设备损伤","物料混料","人员受伤"}; int[] colors={0xFFFFF0DF,0xFFE8F2FF,0xFFFFEDEF};
        for(int i=0;i<types.length;i++){ final String chosen=types[i]; TextView tile=new TextView(this); tile.setText(types[i]+"\\n记录"); tile.setGravity(android.view.Gravity.CENTER);
            tile.setTextColor(ink); tile.setTextSize(15); tile.setTypeface(null,Typeface.BOLD); tile.setPadding(6,20,6,20); tile.setBackground(rounded(colors[i],18));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); p.setMargins(3,3,3,3); quick.addView(tile,p); tile.setOnClickListener(v->incidentForm(chosen)); }
        body.addView(quick); body.addView(button("▥  数据统计 · 查询分析",this::statistics)); body.addView(sectionTitle("本月概况"));
        String month=new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date());
        List<String[]> monthly=db.incidentsFiltered(month+"-01",month+"-31","全部班组","全部区域","全部类型","全部设备");
        body.addView(card("本月事故总数",monthly.size()+" 起"));
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); String[] names={"设备损伤","物料混料","人员受伤"};
        for(String name:names){ int n=0; for(String[] r:monthly) if(name.equals(r[4])) n++; TextView t=card(name,n+" 起"); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); p.setMargins(3,3,3,3); row.addView(t,p); }
        body.addView(row); body.addView(sectionTitle("整改提醒"));
        String today=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()); List<String[]> overdue=db.incidentsDue(today,0,true); List<String[]> upcoming=db.incidentsDue(today,7,false);
        body.addView(card("逾期未完成整改",db.countOverdue(today)+" 起"));
        if(!overdue.isEmpty()) body.addView(button("查看逾期事故（"+overdue.size()+"）",()->showFilteredRecords("逾期未完成整改",overdue)));
        if(!upcoming.isEmpty()) body.addView(button("未来 7 天到期（"+upcoming.size()+"）",()->showFilteredRecords("未来 7 天到期整改",upcoming)));
        body.addView(sectionTitle("常用功能")); body.addView(button("＋ 登记新事故",this::incidentForm)); body.addView(button("事故档案",this::records)); body.addView(button("事故报表中心（Excel / PDF）",this::exportCenter));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); addBottomNav("首页");
    }
    private boolean validIncidentDate(String value) {
        if(value==null || value.trim().isEmpty()) return false;
        try {
            SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US);
            format.setLenient(false);
            return format.format(format.parse(value.trim())).equals(value.trim());
        } catch(Exception e) { return false; }
    }
    private boolean validDueDate(String value) {
        if(value==null || value.trim().isEmpty()) return true;
        try {
            SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
            format.setLenient(false);
            return format.format(format.parse(value.trim())).equals(value.trim());
        } catch(Exception e) { return false; }
    }
    private EditText field(LinearLayout box, String label, String hint, String initial, int lines) {
        TextView l=new TextView(this); l.setText(label); l.setTextColor(ink); l.setTypeface(null,Typeface.BOLD); l.setTextSize(14); l.setPadding(2,12,2,5); box.addView(l);
        EditText e=new EditText(this); e.setHint(hint); e.setText(initial); e.setMinLines(lines); e.setTextSize(15); e.setTextColor(ink);
        e.setPadding(14,10,14,10); e.setBackground(rounded(0xFFFFFFFF,15)); e.setElevation(1);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,5); box.addView(e,p); return e;
    }
    private Spinner spinner(LinearLayout box, String label, List<String> values) {
        if(label!=null && !label.isEmpty()){TextView l=new TextView(this);l.setText(label);l.setTextColor(ink);l.setTypeface(null,Typeface.BOLD);l.setTextSize(14);l.setPadding(2,12,2,5);box.addView(l);}
        Spinner s=new Spinner(this); s.setPadding(8,5,8,5); s.setBackground(rounded(0xFFFFFFFF,14));
        s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values)); box.addView(s); return s;
    }
    private static class IncidentDraft {
        String date,team,area,type,equipment,people,description,analysis,cause,action,prevention,owner,dueDate;
        List<android.net.Uri> attachments=new java.util.ArrayList<>();
    }
    private void incidentForm() { incidentForm(null); }
    private void incidentForm(String presetType) { incidentForm(presetType,null); }
    private void incidentForm(String presetType,IncidentDraft restore) {
        pendingAttachments=restore==null?new java.util.ArrayList<>():new java.util.ArrayList<>(restore.attachments);
        IncidentDraft draft=restore==null?new IncidentDraft():restore;
        page("新增事故记录 · 基本信息");
        ScrollView scroll=new ScrollView(this);
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); scroll.addView(form);
        TextView step=new TextView(this); step.setText("● 1 基本信息     ━━━━━     ○ 2 详细内容     ○ 3 完成");
        step.setTextColor(blue); step.setTextSize(14); step.setTypeface(null,Typeface.BOLD); step.setPadding(4,12,4,18); form.addView(step);
        TextView typeLabel=sectionTitle("事故类型");
        form.addView(typeLabel);
        LinearLayout typeRow=new LinearLayout(this); typeRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] types={"设备损伤","物料混料","人员受伤"}; int[] colors={0xFFFFF0DF,0xFFE8F2FF,0xFFFFEDEF};
        final int[] selectedType={presetType==null?0:Math.max(0,java.util.Arrays.asList(types).indexOf(presetType))};
        TextView[] typeTiles=new TextView[3];
        for(int i=0;i<types.length;i++){ final int index=i; typeTiles[i]=new TextView(this); typeTiles[i].setText(types[i]+"\\n记录"); typeTiles[i].setGravity(android.view.Gravity.CENTER);
            typeTiles[i].setTextColor(ink); typeTiles[i].setTextSize(14); typeTiles[i].setPadding(5,15,5,15); typeTiles[i].setBackground(rounded(colors[i],16));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); p.setMargins(3,3,3,3); typeRow.addView(typeTiles[i],p);
            typeTiles[i].setOnClickListener(v->{selectedType[0]=index; for(int j=0;j<3;j++) typeTiles[j].setAlpha(j==index?1f:0.55f);});
            typeTiles[i].setAlpha(i==selectedType[0]?1f:0.55f);
        }
        form.addView(typeRow);
        String now=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date());
        EditText date=field(form,"发生时间 *","yyyy-MM-dd HH:mm",draft.date==null?now:draft.date,1);
        Spinner team=spinner(form,"班组 *",java.util.Arrays.asList("A班","B班"));
        if(draft.team!=null) team.setSelection(Math.max(0,java.util.Arrays.asList("A班","B班").indexOf(draft.team)));
        Spinner area=spinner(form,"区域 *",java.util.Arrays.asList("造粒","仓库"));
        if(draft.area!=null) area.setSelection(Math.max(0,java.util.Arrays.asList("造粒","仓库").indexOf(draft.area)));
        List<String> equipmentNames=db.equipmentNames(); if(equipmentNames.isEmpty()) equipmentNames.add("未指定");
        Spinner equipment=spinner(form,"设备名称",equipmentNames);
        if(draft.equipment!=null && equipmentNames.contains(draft.equipment)) equipment.setSelection(equipmentNames.indexOf(draft.equipment));
        EditText people=field(form,"涉事人员","人员姓名或工号",draft.people==null?"":draft.people,1);
        attachmentHint=new TextView(this); attachmentHint.setText("现场照片 / 视频：已选择 "+pendingAttachments.size()+" 个"); attachmentHint.setTextColor(ink); form.addView(attachmentHint);
        form.addView(button("＋ 添加现场照片 / 视频",()->{
            android.content.Intent pick=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(android.content.Intent.CATEGORY_OPENABLE); pick.setType("*/*");
            pick.putExtra(android.content.Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"});
            pick.putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE,true);
            pick.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION|android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(pick,REQUEST_ATTACHMENTS);
        }));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(button("下一步：事故分析",()->{
            if(!validIncidentDate(date.getText().toString())){Toast.makeText(this,"发生时间格式错误，请使用 yyyy-MM-dd HH:mm",Toast.LENGTH_SHORT).show();return;}
            draft.date=date.getText().toString().trim(); draft.team=team.getSelectedItem().toString(); draft.area=area.getSelectedItem().toString();
            draft.type=types[selectedType[0]]; draft.equipment=equipment.getSelectedItem().toString(); draft.people=people.getText().toString().trim();
            draft.attachments=new java.util.ArrayList<>(pendingAttachments); incidentAnalysis(draft);
        }));
    }
    private void incidentAnalysis(IncidentDraft draft) {
        page("新增事故记录 · 详细内容");
        ScrollView scroll=new ScrollView(this); LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); scroll.addView(form);
        TextView step=new TextView(this); step.setText("✓ 1 基本信息     ━━━━━     ● 2 详细内容     ○ 3 完成");
        step.setTextColor(blue); step.setTextSize(14); step.setTypeface(null,Typeface.BOLD); step.setPadding(4,12,4,18); form.addView(step);
        TextView heading=sectionTitle("事故分析"); form.addView(heading);
        EditText description=field(form,"事故经过 *","请描述事故发生过程（不少于 50 字）",draft.description==null?"":draft.description,4);
        EditText analysis=field(form,"事故分析 *","分析事故过程、存在的问题及影响",draft.analysis==null?"":draft.analysis,4);
        EditText cause=field(form,"事故原因 *","包括直接原因、间接原因等",draft.cause==null?"":draft.cause,4);
        EditText action=field(form,"当下采取措施 *","事故发生后采取了哪些措施",draft.action==null?"":draft.action,3);
        EditText prevention=field(form,"未来如何避免 *","预防措施和改进方案",draft.prevention==null?"":draft.prevention,3);
        EditText owner=field(form,"整改负责人","负责推动整改的人",draft.owner==null?"":draft.owner,1);
        EditText dueDate=field(form,"整改期限（yyyy-MM-dd）","例如：2026-11-30",draft.dueDate==null?"":draft.dueDate,1);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(button("下一步：确认提交",()->{
            if(description.getText().toString().trim().isEmpty()||analysis.getText().toString().trim().isEmpty()
                ||cause.getText().toString().trim().isEmpty()||action.getText().toString().trim().isEmpty()
                ||prevention.getText().toString().trim().isEmpty()){
                Toast.makeText(this,"请填写五项事故分析内容",Toast.LENGTH_LONG).show();return;
            }
            if(!validDueDate(dueDate.getText().toString())){Toast.makeText(this,"整改期限格式错误，请使用真实日期 yyyy-MM-dd",Toast.LENGTH_LONG).show();return;}
            new AlertDialog.Builder(this).setTitle("确认提交事故")
                .setMessage("类型："+draft.type+"\\n时间："+draft.date+"\\n班组："+draft.team+" · "+draft.area+"\\n设备："+draft.equipment+"\\n附件："+draft.attachments.size()+" 个")
                .setNegativeButton("继续修改",null).setPositiveButton("确认提交",(dialog,which)->{
                    String code=db.addIncident(draft.date,draft.team,draft.area,draft.type,draft.equipment,draft.people,
                        description.getText().toString().trim(),analysis.getText().toString().trim(),cause.getText().toString().trim(),
                        action.getText().toString().trim(),prevention.getText().toString().trim(),owner.getText().toString().trim(),dueDate.getText().toString().trim());
                    int saved=0;
                    for(android.net.Uri uri:draft.attachments){String mime=getContentResolver().getType(uri);try{
                        String savedUri=copyAttachmentToPrivate(uri,code,mime); db.addAttachment(code,savedUri,mime==null?"application/octet-stream":mime); saved++;
                    }catch(Exception e){Toast.makeText(this,"附件保存失败："+e.getMessage(),Toast.LENGTH_LONG).show();}}
                    Toast.makeText(this,"事故档案保存成功："+code+"，附件 "+saved+" 个",Toast.LENGTH_LONG).show(); dashboard();
                }).show();
        }));
        root.addView(button("返回：修改基本信息",()->{
            draft.description=description.getText().toString(); draft.analysis=analysis.getText().toString();
            draft.cause=cause.getText().toString(); draft.action=action.getText().toString();
            draft.prevention=prevention.getText().toString(); draft.owner=owner.getText().toString(); draft.dueDate=dueDate.getText().toString();
            incidentFormWithDraft(draft);
        }));
    }
    private void incidentFormWithDraft(IncidentDraft draft) {
        pendingAttachments=new java.util.ArrayList<>(draft.attachments);
        incidentForm(draft.type,draft);
    }
    private void records() {
        page("事故记录");
        EditText search=new EditText(this); search.setSingleLine(true); search.setHint("⌕ 搜索编号、设备、类型或事故经过");
        search.setTextSize(15); search.setPadding(14,8,14,8); search.setBackground(rounded(0xFFFFFFFF,18)); root.addView(search);
        LinearLayout filters=new LinearLayout(this); filters.setOrientation(LinearLayout.HORIZONTAL);
        Spinner timeFilter=spinner(filters,"",java.util.Arrays.asList("全部时间","本月","本季度","本年度"));
        Spinner typeFilter=spinner(filters,"",java.util.Arrays.asList("全部类型","设备损伤","物料混料","人员受伤"));
        Spinner areaFilter=spinner(filters,"",java.util.Arrays.asList("全部区域","造粒","仓库"));
        root.addView(filters);
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this); scroll.addView(list);
        Runnable refresh=()->{
            list.removeAllViews(); List<String[]> rows=db.incidents(search.getText().toString());
            String period=timeFilter.getSelectedItem().toString(), typeChoice=typeFilter.getSelectedItem().toString(), areaChoice=areaFilter.getSelectedItem().toString();
            String currentMonth=new SimpleDateFormat("yyyy-MM",Locale.US).format(new Date());
            Calendar calendar=Calendar.getInstance(); int currentYear=calendar.get(Calendar.YEAR), quarterStart=(calendar.get(Calendar.MONTH)/3)*3+1;
            for(String[] r:rows){
                String occurred=r[1]==null?"":r[1]; String day=occurred.length()>=10?occurred.substring(0,10):"";
                if("本月".equals(period) && !occurred.startsWith(currentMonth)) continue;
                if("本季度".equals(period) && (day.length()<7 || !day.startsWith(String.format(Locale.US,"%04d-",currentYear)) || Integer.parseInt(day.substring(5,7))<quarterStart)) continue;
                if("本年度".equals(period) && !occurred.startsWith(String.valueOf(currentYear))) continue;
                if(!"全部类型".equals(typeChoice) && !typeChoice.equals(r[4])) continue;
                if(!"全部区域".equals(areaChoice) && !areaChoice.equals(r[3])) continue;
                TextView item=card(r[0]+"   ·   "+r[14],r[1]+"  ·  "+r[2]+"班  ·  "+r[3]+"\n"+r[4]+" / "+r[5]+"\n"+r[7]);
                item.setOnClickListener(v->showIncidentDetail(r)); list.addView(item);
                LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)item.getLayoutParams(); if(p!=null)p.setMargins(0,4,0,8);
            }
            if(rows.isEmpty()) list.addView(card("暂无事故记录","登记事故后会显示在这里"));
        };
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){refresh.run();}
            public void afterTextChanged(Editable e){}});
        android.widget.AdapterView.OnItemSelectedListener filterListener=new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> parent,android.view.View view,int position,long id){refresh.run();}
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
        };
        timeFilter.setOnItemSelectedListener(filterListener); typeFilter.setOnItemSelectedListener(filterListener); areaFilter.setOnItemSelectedListener(filterListener);
        refresh.run(); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); addBottomNav("记录");
    }
    private void showIncidentDetail(String[] r) {
        page("事故详情");
        ScrollView scroll=new ScrollView(this); LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); scroll.addView(body);
        TextView code=sectionTitle(r[0]); body.addView(code);
        body.addView(card("整改状态",r[14]));
        body.addView(card("发生时间 / 班组 / 区域",r[1]+"\n"+r[2]+" · "+r[3]));
        body.addView(card("事故类型 / 设备",r[4]+"\n"+r[5]));
        body.addView(card("涉事人员",r[6]));
        List<String[]> attachments=db.attachments(r[0]);
        body.addView(button("现场照片 / 视频（"+attachments.size()+"）",()->showAttachments(r[0])));
        body.addView(sectionTitle("事故内容"));
        body.addView(card("事故经过",r[7])); body.addView(card("事故分析",r[8])); body.addView(card("事故原因",r[9]));
        body.addView(card("当下采取措施",r[10])); body.addView(card("未来如何避免",r[11]));
        body.addView(sectionTitle("整改跟踪"));
        body.addView(card("整改负责人",r[12])); body.addView(card("整改期限",r[13]));
        StringBuilder history=new StringBuilder();
        List<String[]> events=db.statusHistory(r[0]);
        if(events.isEmpty()) history.append("暂无状态变更记录");
        else for(String[] e:events) history.append(e[2]).append("  ").append(e[0]).append(" → ").append(e[1]).append("\n");
        body.addView(card("整改状态变更记录",history.toString()));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(button("更新整改状态",()->{
            String[] states={"待整改","整改中","已完成"};
            new AlertDialog.Builder(this).setTitle("更新整改状态").setItems(states,(d,index)->{
                db.updateStatus(r[0],states[index]); showIncidentDetail(db.incidents(r[0]).get(0));
            }).show();
        }));
        root.addView(button("编辑事故档案",()->editIncident(r)));
        root.addView(button("删除事故档案",()->new AlertDialog.Builder(this).setTitle("确认删除事故")
            .setMessage("删除后将同时删除关联的照片和视频文件，且无法恢复。\n\n"+r[0])
            .setNegativeButton("取消",null).setPositiveButton("确认删除",(d,w)->{
                removeIncidentAttachmentFiles(r[0]); db.deleteIncident(r[0]); Toast.makeText(this,"事故档案已删除",Toast.LENGTH_SHORT).show(); records();
            }).show()));
        addBottomNav("记录");
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
            if(!validIncidentDate(d) || description.getText().toString().trim().isEmpty() || analysis.getText().toString().trim().isEmpty()
                || cause.getText().toString().trim().isEmpty() || action.getText().toString().trim().isEmpty()
                || prevention.getText().toString().trim().isEmpty()) {
                Toast.makeText(this,"请填写正确的发生时间（yyyy-MM-dd HH:mm）及五项事故内容",Toast.LENGTH_LONG).show(); return;
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

    private void showCatalog(String title,String[] values) {
        new AlertDialog.Builder(this).setTitle(title).setItems(values,(dialog,which)->{}).setPositiveButton("关闭",null).show();
    }
    private void settingsCenter() {
        page("设置中心"); ScrollView scroll=new ScrollView(this); LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); scroll.addView(body);
        body.addView(button("设备管理  ›",this::equipment));
        body.addView(button("班组管理  ›",()->showCatalog("班组管理",new String[]{"A班","B班"})));
        body.addView(button("区域管理  ›",()->showCatalog("区域管理",new String[]{"造粒","仓库"})));
        body.addView(button("数据备份（含照片 / 视频）  ›",this::startBackup));
        body.addView(button("从备份恢复数据",this::chooseRestore)); body.addView(button("立即检查整改提醒",this::checkRemindersNow));
        body.addView(button("检查照片 / 视频附件完整性",this::checkAttachmentIntegrity));
        body.addView(button("关于我们 / 版本信息  ›",()->new AlertDialog.Builder(this).setTitle("生产事故管理系统")
            .setMessage("版本：1.0.0\\n数据存储：本机 SQLite\\n支持：事故登记、整改跟踪、统计分析、Excel / PDF 报表、数据备份恢复")
            .setPositiveButton("关闭",null).show()));
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); addBottomNav("我的");
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
        addBottomNav("我的");
    }
    private void statistics() {
        page("多条件事故统计");
        ScrollView scroll=new ScrollView(this);
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); scroll.addView(body);
        String today=new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());
        EditText from=field(body,"开始日期（yyyy-MM-dd）","留空表示不限",today.substring(0,8)+"01",1);
        EditText to=field(body,"结束日期（yyyy-MM-dd）","留空表示不限",today,1);
        LinearLayout ranges=new LinearLayout(this); ranges.setOrientation(LinearLayout.HORIZONTAL);
        String[] rangeNames={"本月","本季度","本年度","自定义"};
        for(int ri=0;ri<rangeNames.length;ri++){final int mode=ri;TextView chip=new TextView(this);chip.setText(rangeNames[ri]);chip.setGravity(android.view.Gravity.CENTER);
            chip.setTextColor(blue);chip.setTextSize(13);chip.setTypeface(null,Typeface.BOLD);chip.setPadding(5,12,5,12);chip.setBackground(rounded(0xFFE7F0FF,16));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);cp.setMargins(3,4,3,8);ranges.addView(chip,cp);
            chip.setOnClickListener(v->{if(mode==3){from.requestFocus();return;}Calendar cal=Calendar.getInstance();cal.setTime(new Date());
                if(mode==0)cal.set(Calendar.DAY_OF_MONTH,1);else if(mode==1){int q=cal.get(Calendar.MONTH)/3;cal.set(Calendar.MONTH,q*3);cal.set(Calendar.DAY_OF_MONTH,1);}
                else{cal.set(Calendar.MONTH,Calendar.JANUARY);cal.set(Calendar.DAY_OF_MONTH,1);}
                from.setText(new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(cal.getTime()));to.setText(today);});
        }
        body.addView(ranges);
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
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); addBottomNav("统计");
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
        LinearLayout ranges=new LinearLayout(this); ranges.setOrientation(LinearLayout.HORIZONTAL);
        String[] rangeNames={"本月","本季度","本年度","自定义"};
        for(int ri=0;ri<rangeNames.length;ri++){ final int mode=ri; TextView chip=new TextView(this); chip.setText(rangeNames[ri]); chip.setGravity(android.view.Gravity.CENTER);
            chip.setTextColor(blue); chip.setTextSize(13); chip.setTypeface(null,Typeface.BOLD); chip.setPadding(5,12,5,12); chip.setBackground(rounded(0xFFE7F0FF,16));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1); cp.setMargins(3,4,3,8); ranges.addView(chip,cp);
            chip.setOnClickListener(v->{ if(mode==3){from.requestFocus();return;} Calendar cal=Calendar.getInstance(); cal.setTime(new Date());
                if(mode==0) cal.set(Calendar.DAY_OF_MONTH,1);
                else if(mode==1){int q=cal.get(Calendar.MONTH)/3;cal.set(Calendar.MONTH,q*3);cal.set(Calendar.DAY_OF_MONTH,1);}
                else {cal.set(Calendar.MONTH,Calendar.JANUARY);cal.set(Calendar.DAY_OF_MONTH,1);}
                from.setText(new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(cal.getTime())); to.setText(today);
            });
        }
        root.addView(ranges);
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
        addBottomNav("记录");
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
    private java.io.File ownedAttachmentFile(String rawUri) {
        try {
            android.net.Uri uri=android.net.Uri.parse(rawUri);
            if(!(getPackageName()+".fileprovider").equals(uri.getAuthority())) return null;
            List<String> parts=uri.getPathSegments();
            if(parts.size()<3 || !"incident_attachments".equals(parts.get(0))) return null;
            java.io.File root=attachmentDirectory();
            java.io.File file=new java.io.File(root,parts.get(1));
            for(int i=2;i<parts.size();i++) file=new java.io.File(file,parts.get(i));
            String base=root.getCanonicalPath()+java.io.File.separator;
            return file.getCanonicalPath().startsWith(base)?file:null;
        } catch(Exception e) { return null; }
    }
    private void removeIncidentAttachmentFiles(String code) {
        for(String[] item:db.attachments(code)) {
            java.io.File file=ownedAttachmentFile(item[0]);
            if(file!=null && file.isFile()) file.delete();
        }
        java.io.File dir=new java.io.File(attachmentDirectory(),code.replaceAll("[^A-Za-z0-9_-]","_"));
        java.io.File[] remaining=dir.listFiles();
        if(remaining!=null && remaining.length==0) dir.delete();
    }
    private void checkAttachmentIntegrity() {
        List<String[]> items=db.allAttachments();
        List<String> missing=new java.util.ArrayList<>();
        for(String[] item:items) {
            try {
                android.net.Uri uri=android.net.Uri.parse(item[1]);
                try(java.io.InputStream in=getContentResolver().openInputStream(uri)) {
                    if(in==null) missing.add(item[0]);
                }
            } catch(Exception e) { missing.add(item[0]); }
        }
        if(missing.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("附件完整性检查")
                .setMessage("检查完成：共 "+items.size()+" 个附件，均可读取。")
                .setPositiveButton("关闭",null).show();
        } else {
            StringBuilder message=new StringBuilder("发现 "+missing.size()+" 个无法读取的附件（共 "+items.size()+" 个）：");
            for(String code:missing) message.append("\n").append(code);
            new AlertDialog.Builder(this).setTitle("附件完整性异常")
                .setMessage(message.toString()).setPositiveButton("关闭",null).show();
        }
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
    private void checkpointDatabase() throws java.io.IOException {
        try(android.database.Cursor cursor=db.getWritableDatabase().rawQuery("PRAGMA wal_checkpoint(FULL)",null)) {
            if(cursor.moveToFirst() && cursor.getInt(0)!=0)
                throw new java.io.IOException("数据库 WAL 检查点忙碌，无法安全打包");
        }
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
            checkpointDatabase();
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
            checkpointDatabase();
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
