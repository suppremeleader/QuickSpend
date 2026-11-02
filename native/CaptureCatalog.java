package com.quickspend.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/** Shared, local category catalogue. Custom names never become translated data keys. */
public final class CaptureCatalog {
    private static final String[] TYPES={"Expense","Income","Lent","Borrowed"};
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("qs_catalog",0);}
    private static JSONObject object(String value){try{return new JSONObject(value);}catch(JSONException e){return new JSONObject();}}
    private static String month(){Calendar c=Calendar.getInstance();return c.get(Calendar.YEAR)+"-"+c.get(Calendar.MONTH);}
    private static String[] defaults(String type){return "Expense".equals(type)?new String[]{"Uncategorised","Food","Travel","Shopping","Bills","Fun","Health","Other"}:"Income".equals(type)?new String[]{"Uncategorised","Salary","Gift","Refund","Other"}:new String[]{"Uncategorised","Friend","Family"};}
    public static boolean valid(String value){return value!=null&&!value.trim().isEmpty()&&value.trim().length()<=40&&!value.matches(".*[<>\"'\\p{Cntrl}].*")&&!Arrays.asList("__proto__","constructor","prototype","Savings").contains(value.trim());}
    public static synchronized JSONObject custom(Context c){return object(prefs(c).getString("custom","{}"));}
    private static boolean contains(List<String> names,String value){for(String n:names)if(n.equalsIgnoreCase(value))return true;return false;}
    public static synchronized List<String> labels(Context c,String type){List<String> result=new ArrayList<>(Arrays.asList(defaults(type)));JSONArray extra=custom(c).optJSONArray(type);if(extra!=null)for(int i=0;i<extra.length();i++){String name=extra.optString(i);if(valid(name)&&!contains(result,name))result.add(name);}JSONObject scores=month().equals(prefs(c).getString("month",""))?object(prefs(c).getString("scores","{}")):new JSONObject();JSONObject typed=scores.optJSONObject(type);if(typed!=null)result.sort((a,b)->{JSONArray x=typed.optJSONArray(a),y=typed.optJSONArray(b);int amount=Double.compare(y==null?0:y.optDouble(0),x==null?0:x.optDouble(0));return amount!=0?amount:Integer.compare(y==null?0:y.optInt(1),x==null?0:x.optInt(1));});return result;}
    public static synchronized boolean add(Context c,String type,String raw){String name=raw.trim();if(!Arrays.asList(TYPES).contains(type)||!valid(name)||contains(labels(c,type),name))return false;try{JSONObject data=custom(c);JSONArray names=data.optJSONArray(type);if(names==null)names=new JSONArray();names.put(name);data.put(type,names);if(!prefs(c).edit().putString("custom",data.toString()).commit())throw new IllegalStateException("Could not save category");return true;}catch(JSONException e){throw new IllegalStateException(e);}}
    public static synchronized JSONObject sync(Context c,JSONObject config){JSONObject incoming=config.optJSONObject("customCats");if(incoming!=null)for(String type:TYPES){JSONArray names=incoming.optJSONArray(type);if(names!=null)for(int i=0;i<names.length();i++){String name=names.optString(i);if(valid(name)&&!contains(labels(c,type),name))add(c,type,name);}}
        JSONObject scores=config.optJSONObject("scores");if(scores!=null&&!prefs(c).edit().putString("scores",scores.toString()).putString("month",month()).commit())throw new IllegalStateException("Could not save category order");return custom(c);}
    public static synchronized void record(Context c,JSONObject entry){Calendar when=Calendar.getInstance();when.setTimeInMillis(entry.optLong("ts",System.currentTimeMillis()));if(!(when.get(Calendar.YEAR)+"-"+when.get(Calendar.MONTH)).equals(month()))return;try{JSONObject scores=month().equals(prefs(c).getString("month",""))?object(prefs(c).getString("scores","{}")):new JSONObject();String type=entry.optString("t"),name=entry.optString("c");JSONObject typed=scores.optJSONObject(type);if(typed==null)typed=new JSONObject();JSONArray old=typed.optJSONArray(name);typed.put(name,new JSONArray().put((old==null?0:old.optDouble(0))+entry.optDouble("a")).put((old==null?0:old.optInt(1))+1));scores.put(type,typed);prefs(c).edit().putString("scores",scores.toString()).putString("month",month()).apply();}catch(JSONException ignored){}}
    public static synchronized void clear(Context c){if(!prefs(c).edit().clear().commit())throw new IllegalStateException("Could not reset categories");}
}
