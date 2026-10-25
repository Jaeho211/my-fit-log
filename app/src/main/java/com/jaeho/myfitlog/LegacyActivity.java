package com.jaeho.myfitlog;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.text.*;

public class LegacyActivity extends Activity {
 JSONObject db; LinearLayout body; String tab="오늘"; final int ink=Color.rgb(24,37,41),accent=Color.rgb(8,126,120);
 JSONArray arr(String k){return db.optJSONArray(k);}
 JSONObject obj(Object... kv){JSONObject o=new JSONObject();try{for(int i=0;i<kv.length;i+=2)o.put((String)kv[i],kv[i+1]);}catch(Exception e){throw new RuntimeException(e);}return o;}
 void put(String k,Object v){try{db.put(k,v);}catch(Exception e){throw new RuntimeException(e);}}
 public void onCreate(Bundle state){
  super.onCreate(state);
  tab=state==null?getIntent().getStringExtra("tab"):state.getString("tab","운동");
  if(tab==null)tab="운동";
  try{db=new FitRepository(this).read();}catch(Exception e){throw new IllegalStateException("저장 데이터를 읽을 수 없습니다",e);}
  show();
 }

 void save(){new FitRepository(this).write(db);}
 int dp(int n){return (int)(getResources().getDisplayMetrics().density*n);}
 TextView text(LinearLayout parent,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(ink);t.setPadding(0,dp(8),0,dp(8));parent.addView(t);return t;}
 android.graphics.drawable.GradientDrawable surface(int color,int radius){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
 void button(LinearLayout p,String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);boolean secondary=s.contains("수정")||s.contains("삭제")||s.contains("제외");b.setTextColor(secondary?accent:Color.WHITE);b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22339988),surface(secondary?0xffe4f3ef:accent,16),null));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(52));bp.setMargins(0,dp(6),0,dp(6));p.addView(b,bp);b.setOnClickListener(v->r.run());}
 LinearLayout box(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(dp(16),dp(10),dp(16),dp(12));b.setBackground(surface(Color.WHITE,22));b.setElevation(dp(1));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,dp(8));body.addView(b,lp);return b;}
 EditText field(LinearLayout p,String hint,String val,boolean number){text(p,hint,13);EditText e=new EditText(this);e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setBackground(surface(0xffeef3ef,10));e.setMinHeight(dp(48));e.setHint(hint);e.setText(val);if(number)e.setInputType(8194);p.addView(e);return e;}
 LinearLayout form(){LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(20),dp(12),dp(20),dp(12));return p;}
 interface Accept{void run()throws Exception;}
 void dialog(String title,LinearLayout f,Accept a){ScrollView s=new ScrollView(this);s.addView(f);AlertDialog d=new AlertDialog.Builder(this).setTitle(title).setView(s).setNegativeButton("취소",null).setPositiveButton("저장",null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{try{a.run();save();d.dismiss();show();}catch(Exception e){toast("입력값을 확인해 주세요: "+e.getMessage());}}));d.show();}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 String date(long time){return new SimpleDateFormat("M월 d일 (E) HH:mm",Locale.KOREA).format(new Date(time));}
 String day(long time){return new SimpleDateFormat("yyyy-MM-dd",Locale.KOREA).format(new Date(time));}
 String today(){return day(System.currentTimeMillis());}

 void heading(LinearLayout p,String label,int size){text(p,label,size).setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));}
 void muted(LinearLayout p,String label){text(p,label,13).setTextColor(0xff647378);}
 void link(LinearLayout p,String label,Runnable action){TextView t=text(p,label,14);t.setTextColor(accent);t.setMinHeight(dp(48));t.setGravity(Gravity.CENTER_VERTICAL);t.setOnClickListener(v->action.run());}
 protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("tab",tab);}
 @Override public void onBackPressed(){finish();}
 void show(){
 if("오늘".equals(tab)||"운동".equals(tab)||"기록".equals(tab)||"예약".equals(tab)){
  startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("section",tab));
  finish();return;
 }

 LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xfff5f6f8);setContentView(root);
 root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(dp(20),i.getSystemWindowInsetTop()+dp(8),dp(20),i.getSystemWindowInsetBottom());return i;});
 LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);root.addView(top);
 TextView brand=new TextView(this);brand.setText("MY FIT LOG");brand.setTextColor(ink);brand.setTextSize(12);brand.setLetterSpacing(.15f);top.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));
 TextView gear=new TextView(this);gear.setText("설정");gear.setTextColor(accent);gear.setGravity(Gravity.CENTER);gear.setContentDescription("설정 열기");top.addView(gear,new LinearLayout.LayoutParams(dp(56),dp(48)));gear.setOnClickListener(v->{tab="설정";show();});
 heading(root,"나에게 맞게",28);
 ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(0,dp(8),0,dp(20));scroll.addView(body);
 settings();
 LinearLayout nav=new LinearLayout(this);root.addView(nav);String[] tabs={"오늘","기록","운동","예약"};String[] labels={"오늘","기록","내 운동","PT"};String[] glyphs={"home","history","dumbbell","calendar"};
 for(int n=0;n<tabs.length;n++){String dest=tabs[n];boolean selected=tab.equals(dest);LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setBackground(surface(selected?0xffc0e9df:0xfff5f6f8,16));item.addView(new NavIcon(glyphs[n],selected?ink:0xff77796f),new LinearLayout.LayoutParams(dp(24),dp(24)));TextView t=new TextView(this);t.setText(labels[n]);t.setTextSize(12);t.setTextColor(selected?ink:0xff647378);item.addView(t);item.setContentDescription(labels[n]+" 탭");item.setSelected(selected);item.setOnClickListener(v->{tab=dest;show();});nav.addView(item,new LinearLayout.LayoutParams(0,dp(64),1));}
 }
 class NavIcon extends View{
 String kind;int color;NavIcon(String k,int c){super(LegacyActivity.this);kind=k;color=c;}
 protected void onDraw(android.graphics.Canvas canvas){super.onDraw(canvas);canvas.save();canvas.scale(getWidth()/24f,getHeight()/24f);android.graphics.Paint p=new android.graphics.Paint(3);p.setColor(color);p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(android.graphics.Paint.Cap.ROUND);p.setStrokeJoin(android.graphics.Paint.Join.ROUND);
 if(kind.equals("home")){android.graphics.Path path=new android.graphics.Path();path.moveTo(3,11);path.lineTo(12,3);path.lineTo(21,11);path.moveTo(6,9);path.lineTo(6,21);path.lineTo(18,21);path.lineTo(18,9);canvas.drawPath(path,p);}
 else if(kind.equals("history")){canvas.drawCircle(12,12,9,p);canvas.drawLine(12,6,12,12,p);canvas.drawLine(12,12,17,15,p);}
 else if(kind.equals("calendar")){canvas.drawRoundRect(4,5,20,21,2,2,p);canvas.drawLine(4,10,20,10,p);canvas.drawLine(8,3,8,7,p);canvas.drawLine(16,3,16,7,p);}
 else {canvas.drawLine(8,12,16,12,p);canvas.drawRoundRect(4,6,8,18,1,1,p);canvas.drawRoundRect(16,6,20,18,1,1,p);canvas.drawLine(2,9,2,15,p);canvas.drawLine(22,9,22,15,p);}canvas.restore();}
 }
 double positive(EditText e,boolean decimal)throws Exception{double n=Double.parseDouble(e.getText().toString());if(!Double.isFinite(n)||n<0||(!decimal&&(n<1||n!=Math.floor(n)))||n>1000)throw new Exception("무게·횟수·세트 범위");return n;}

 void settings(){LinearLayout b=box();text(b,"재호 · 1986년생",22);text(b,"상체 근육 키우기 · 자세 개선\n장비: 3kg 덤벨 두 개 · 철봉\n로그인 없이 기기에 저장됩니다.",16);button(body,"주간 운동 목표 변경",()->{LinearLayout f=form();EditText n=field(f,"주당 운동 일수",String.valueOf(db.optInt("weekly",3)),true);dialog("주간 목표",f,()->{double value=positive(n,false);if(value>7)throw new Exception("1~7일");put("weekly",(int)value);});});button(body,"JSON 백업 저장",()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"my-fit-log-"+today()+".json"),10));button(body,"백업 복원",()->new AlertDialog.Builder(this).setMessage("복원하면 현재 기록이 백업 내용으로 교체됩니다.").setNegativeButton("취소",null).setPositiveButton("파일 선택",(d,w)->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE),11)).show());}
 protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;try{if(request==10){try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(db.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}toast("백업 완료");}else if(request==11){JSONObject restored;try(java.io.InputStream in=getContentResolver().openInputStream(data.getData());java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>10000000)throw new Exception("파일이 너무 큽니다");}restored=new JSONObject(out.toString("UTF-8"));}if(restored.optInt("version")!=1)throw new Exception("지원하지 않는 백업");for(String key:new String[]{"exercises","logs","plan","appointments"}){JSONArray a=restored.getJSONArray(key);for(int i=0;i<a.length();i++)a.getJSONObject(i);}db=restored;save();show();toast("복원 완료");}}catch(Exception e){toast("파일 처리 실패: "+e.getMessage());}}
}
