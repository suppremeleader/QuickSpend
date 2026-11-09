package com.quickspend.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** Opens the system picker and returns one bounded JPEG to the active capture form. */
public final class CapturePhotoActivity extends Activity {
    private static final int PICK=51;
    private boolean delivered;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state!=null)return;
        Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
        try{startActivityForResult(picker,PICK);}catch(RuntimeException e){deliver(2,null);}
    }

    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=PICK)return;
        Uri uri=data==null?null:data.getData();
        if(result!=RESULT_OK||uri==null){deliver(0,null);return;}
        new Thread(()->{
            String photo;
            try{photo=encode(uri);}catch(Exception|OutOfMemoryError e){photo=null;}
            final String value=photo;
            runOnUiThread(()->deliver(value==null?2:1,value));
        },"qs-capture-photo").start();
    }

    private String encode(Uri uri) throws Exception {
        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        try(InputStream source=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(source,null,options);}
        if(options.outWidth<=0||options.outHeight<=0)return null;
        options.inJustDecodeBounds=false;
        options.inSampleSize=1;
        while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>960)options.inSampleSize*=2;
        Bitmap bitmap;
        try(InputStream source=getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(source,null,options);}
        if(bitmap==null)return null;
        try{
            ByteArrayOutputStream output=new ByteArrayOutputStream();
            for(int quality:new int[]{83,68,52,35,20}){
                output.reset();
                if(bitmap.compress(Bitmap.CompressFormat.JPEG,quality,output)&&output.size()<=134000)
                    return "data:image/jpeg;base64,"+Base64.encodeToString(output.toByteArray(),Base64.NO_WRAP);
            }
            return null;
        }finally{bitmap.recycle();}
    }

    @Override public void onBackPressed(){deliver(0,null);}

    private void deliver(int result,String value){
        if(delivered)return;
        delivered=true;
        ResultReceiver receiver=getIntent().getParcelableExtra("qsCapturePhoto");
        if(receiver!=null){Bundle payload=new Bundle();payload.putString("photo",value);receiver.send(result,payload);}
        finish();
    }
}
