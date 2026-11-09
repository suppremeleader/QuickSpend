package com.quickspend.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.util.Base64;
import androidx.core.content.FileProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

/** Keeps the calling draft in place while the system camera or image browser is open. */
public final class CapturePhotoActivity extends Activity {
    public static final class Provider extends FileProvider {}
    private static final int PICK=51;
    private boolean delivered;
    private File photoFile;

    static Intent launch(Context context,String source,ResultReceiver callback){
        return new Intent(context,CapturePhotoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("qsCapturePhoto",callback).putExtra("qsPhotoSource",source);
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        if(state!=null){
            String path=state.getString("photoFile");
            if(path!=null)photoFile=new File(path);
            return;
        }
        try{
            Intent picker;
            if("camera".equals(getIntent().getStringExtra("qsPhotoSource"))){
                File folder=new File(getCacheDir(),"capture-photos");
                if(!folder.isDirectory()&&!folder.mkdirs())throw new IllegalStateException("Could not create photo cache");
                photoFile=File.createTempFile("receipt-",".jpg",folder);
                Uri output=photoUri();
                picker=new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT,output);
                picker.setClipData(ClipData.newRawUri("Receipt photo",output));
                picker.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }else picker=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*");
            startActivityForResult(picker,PICK);
            ResultReceiver receiver=getIntent().getParcelableExtra("qsCapturePhoto");
            if(receiver!=null)receiver.send(3,Bundle.EMPTY);
        }catch(Exception e){deliver(2,null);}
    }

    private Uri photoUri(){return FileProvider.getUriForFile(this,getPackageName()+".capturephotos",photoFile);}

    @Override protected void onSaveInstanceState(Bundle state){
        super.onSaveInstanceState(state);
        if(photoFile!=null)state.putString("photoFile",photoFile.getAbsolutePath());
    }

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=PICK)return;
        if(result!=RESULT_OK){deliver(0,null);return;}
        Uri uri=photoFile!=null?photoUri():data==null?null:data.getData();
        if(uri==null){deliver(2,null);return;}
        new Thread(()->{
            String photo;
            try{photo=encode(uri);}catch(Exception|OutOfMemoryError e){photo=null;}
            final String value=photo;
            runOnUiThread(()->deliver(value==null?2:1,value));
        },"qs-capture-photo").start();
    }

    private String encode(Uri uri) throws Exception{
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
        if(photoFile!=null){revokeUriPermission(photoUri(),Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);photoFile.delete();}
        ResultReceiver receiver=getIntent().getParcelableExtra("qsCapturePhoto");
        if(receiver!=null){Bundle payload=new Bundle();payload.putString("photo",value);receiver.send(result,payload);}
        finish();
    }
}
