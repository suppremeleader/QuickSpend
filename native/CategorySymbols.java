package com.quickspend.app;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
/** Matching vector paths and colours from the in-app category badge system. */
public final class CategorySymbols {
 private CategorySymbols(){}
 static int resource(String name){if(name==null)return R.drawable.qs_category_tag;switch(name){case "Food":return R.drawable.qs_category_food;case "Travel":return R.drawable.qs_category_car;case "Shopping":return R.drawable.qs_category_bag;case "Bills":return R.drawable.qs_category_receipt;case "Fun":return R.drawable.qs_category_film;case "Health":return R.drawable.qs_category_health;case "Other":return R.drawable.qs_category_grid;case "Salary":return R.drawable.qs_category_briefcase;case "Gift":return R.drawable.qs_category_gift;case "Refund":return R.drawable.qs_category_refund;case "Friend":return R.drawable.qs_category_people;case "Family":return R.drawable.qs_category_family;case "Uncategorised":return R.drawable.qs_category_circle;case "Savings":return R.drawable.qs_category_savings;default:return R.drawable.qs_category_tag;}}
 static int color(String name){if(name==null)return 0xff389e99;switch(name){case "Food":return 0xffdb8b35;case "Travel":return 0xff4d87d5;case "Shopping":return 0xff9361de;case "Bills":return 0xffc39b31;case "Fun":return 0xff60a269;case "Health":return 0xffc36583;case "Other":return 0xff667784;case "Salary":return 0xff389e99;case "Gift":return 0xffc36583;case "Refund":return 0xff4d87d5;case "Friend":return 0xff9361de;case "Family":return 0xff60a269;case "Uncategorised":return 0xff667784;case "Savings":return 0xff60a269;default:return 0xff389e99;}}
 static Drawable drawable(Context c,String name){Drawable d=ContextCompat.getDrawable(c,resource(name));if(d!=null){d=d.mutate();int size=Math.round(24*c.getResources().getDisplayMetrics().density);d.setBounds(0,0,size,size);}return d;}
 static void draw(Context c,Canvas canvas,String name,float x,float y,float size){Drawable d=drawable(c,name);if(d!=null){d.setBounds(Math.round(x),Math.round(y),Math.round(x+size),Math.round(y+size));d.draw(canvas);}}
}
