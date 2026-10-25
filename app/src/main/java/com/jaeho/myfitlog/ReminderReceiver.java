package com.jaeho.myfitlog;
import android.app.*;
import android.content.*;
import org.json.*;
public class ReminderReceiver extends android.content.BroadcastReceiver {
 public static void schedule(Context c) {
  try {
   JSONObject db=new JSONObject(c.getSharedPreferences("fit",0).getString("db","{}"));
   JSONArray a=db.optJSONArray("appointments"); if(a==null)return;
   AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
   for(int i=0;i<a.length();i++) {JSONObject p=a.getJSONObject(i); PendingIntent pi=PendingIntent.getBroadcast(c,p.getInt("id"),new Intent(c,ReminderReceiver.class).putExtra("id",p.getInt("id")),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); am.cancel(pi); long time=p.getLong("time")-p.optInt("minutes",60)*60000L; if(time>System.currentTimeMillis())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,time,pi);}
  }catch(Exception ignored){}
 }
 @Override public void onReceive(Context c,Intent intent) {
  if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){schedule(c);return;}
  try {
   JSONArray a=new JSONObject(c.getSharedPreferences("fit",0).getString("db","{}")).getJSONArray("appointments");
   for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i); if(p.getInt("id")!=intent.getIntExtra("id",-1))continue;
    NotificationManager nm=c.getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("pt","PT 예약",NotificationManager.IMPORTANCE_HIGH));
    PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
    nm.notify(p.getInt("id"),new Notification.Builder(c,"pt").setSmallIcon(android.R.drawable.ic_menu_my_calendar).setContentTitle("PT 예약 알림").setContentText(new java.text.SimpleDateFormat("MM월 dd일 HH:mm",java.util.Locale.KOREA).format(new java.util.Date(p.getLong("time")))+" · "+p.optString("note")).setContentIntent(open).setAutoCancel(true).build());
   }
  }catch(Exception ignored){}
 }
}
