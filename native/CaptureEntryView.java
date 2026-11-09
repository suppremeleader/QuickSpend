package com.quickspend.app;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Local renderer for the shared entry sheet, with no access to the app's WebView storage. */
public final class CaptureEntryView extends WebView {
    private static final String ORIGIN="https://appassets.androidplatform.net";
    private static final String PATH="/app/public/index.html";
    private static final Set<String> ASSETS=new HashSet<>(Arrays.asList(
        PATH,"/app/public/assets/quickspend-icon.png","/app/public/assets/ftue.css","/app/public/locales/hi.json"
    ));
    interface Action {void run(JSONObject message,Reply reply);}
    static final class Reply {
        private final JavaScriptReplyProxy proxy;
        private final int id;
        Reply(JavaScriptReplyProxy proxy,int id){this.proxy=proxy;this.id=id;}
        void send(String key,String value){
            try{proxy.postMessage(new JSONObject().put("id",id).put(key,value).toString());}catch(Exception ignored){}
        }
        void success(){send("status","ok");}
        void error(String message){send("error",message);}
    }

    @Override public boolean onKeyUp(int code,android.view.KeyEvent event){
        if(code==android.view.KeyEvent.KEYCODE_BACK){
            evaluateJavascript("closeSheet()",null);return true;
        }
        return super.onKeyUp(code,event);
    }

    public static boolean supported(){return WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER);}

    CaptureEntryView(Context context,JSONObject state,Action action){
        super(context);
        setBackgroundColor(Color.TRANSPARENT);
        getSettings().setJavaScriptEnabled(true);
        getSettings().setAllowFileAccess(false);
        getSettings().setAllowContentAccess(false);
        getSettings().setDomStorageEnabled(false);
        setVerticalScrollBarEnabled(false);
        setWebChromeClient(new WebChromeClient());
        WebViewAssetLoader assets=new WebViewAssetLoader.Builder()
            .addPathHandler("/app/",new WebViewAssetLoader.AssetsPathHandler(context)).build();
        setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return true;}
            @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return true;}
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
                Uri uri=request.getUrl();
                if("data".equals(uri.getScheme())&&uri.toString().startsWith("data:image/"))return null;
                if("https".equals(uri.getScheme())&&"appassets.androidplatform.net".equals(uri.getHost())&&ASSETS.contains(uri.getPath()))
                    return assets.shouldInterceptRequest(uri);
                return new WebResourceResponse("text/plain","UTF-8",403,"Forbidden",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
            }
            @Override public void onPageFinished(WebView view,String url){
                if((ORIGIN+PATH).equals(url))evaluateJavascript("initialiseCapture(JSON.parse("+JSONObject.quote(state.toString())+"))",null);
            }
        });
        WebViewCompat.addWebMessageListener(this,"QuickCapture",Collections.singleton(ORIGIN),(view,message,origin,mainFrame,proxy)->{
            if(!mainFrame||!ORIGIN.equals(origin.toString()))return;
            String raw=message.getData();
            if(raw==null||raw.length()>210000)return;
            try{
                JSONObject request=new JSONObject(raw);
                action.run(request,new Reply(proxy,request.getInt("id")));
            }catch(Exception ignored){}
        });
        loadUrl(ORIGIN+PATH);
    }
}
