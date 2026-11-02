package com.quickspend.app;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.List;
/** Device preference only; never enabled by a backup or category ranking change. */
public final class QuickAccessConfig {
    private QuickAccessConfig() {}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("qs_float",0);}
    public static boolean enabled(Context c){return prefs(c).getBoolean("quick_access_enabled",false);}
    public static String[] slots(Context c,String type){
        String[] defaults="Income".equals(type)?new String[]{"Salary","Gift","Other","Refund"}:new String[]{"Food","Travel","Other","Shopping"};
        List<String> valid=CaptureCatalog.labels(c,type);
        try{JSONArray values=new JSONArray(prefs(c).getString("quick_access_"+type,"[]"));for(int i=0;i<4;i++){String value=values.optString(i);if(valid.contains(value))defaults[i]=value;}}catch(JSONException ignored){}
        return defaults;
    }
    public static JSONObject read(Context c){try{return new JSONObject().put("enabled",enabled(c)).put("Expense",new JSONArray(slots(c,"Expense"))).put("Income",new JSONArray(slots(c,"Income"))).put("expenseCategories",new JSONArray(CaptureCatalog.labels(c,"Expense"))).put("incomeCategories",new JSONArray(CaptureCatalog.labels(c,"Income")));}catch(JSONException e){throw new IllegalStateException(e);}}
    public static void save(Context c,JSONObject data){SharedPreferences.Editor edit=prefs(c).edit();for(String type:new String[]{"Expense","Income"}){JSONArray values=data.optJSONArray(type);if(values==null||values.length()!=4)throw new IllegalArgumentException("Choose four categories");List<String> valid=CaptureCatalog.labels(c,type);for(int i=0;i<4;i++)if(!valid.contains(values.optString(i)))throw new IllegalArgumentException("Choose valid categories for this payment type");edit.putString("quick_access_"+type,values.toString());}edit.putBoolean("quick_access_enabled",data.optBoolean("enabled",false));if(!edit.commit())throw new IllegalStateException("Could not save Quick Access preference");}
}
