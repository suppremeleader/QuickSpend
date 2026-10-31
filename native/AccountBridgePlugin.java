package com.quickspend.app;

import android.os.CancellationSignal;
import androidx.core.content.ContextCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CustomCredential;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.ClearCredentialException;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.FieldValue;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@CapacitorPlugin(name="AccountBridge")
public class AccountBridgePlugin extends Plugin {
    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private String webClient;
    private boolean signingIn;

    @Override public void load() {
        try (InputStream input=getContext().getAssets().open("cloud/google-services.json"); ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096]; int n;
            while((n=input.read(buffer))!=-1)bytes.write(buffer,0,n);
            JSONObject config=new JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
            JSONArray clients=config.getJSONArray("client"); JSONObject client=null;
            for(int i=0;i<clients.length();i++) {
                JSONObject candidate=clients.getJSONObject(i);
                if(getContext().getPackageName().equals(candidate.getJSONObject("client_info").getJSONObject("android_client_info").getString("package_name")))client=candidate;
            }
            if(client==null)return;
            JSONArray oauth=client.optJSONArray("oauth_client");
            if(oauth!=null)for(int i=0;i<oauth.length();i++)if(oauth.getJSONObject(i).optInt("client_type")==3)webClient=oauth.getJSONObject(i).getString("client_id");
            if(webClient==null||webClient.isEmpty())return;
            FirebaseOptions options=new FirebaseOptions.Builder()
                .setApplicationId(client.getJSONObject("client_info").getString("mobilesdk_app_id"))
                .setApiKey(client.getJSONArray("api_key").getJSONObject(0).getString("current_key"))
                .setProjectId(config.getJSONObject("project_info").getString("project_id")).build();
            FirebaseApp app;
            try { app=FirebaseApp.getInstance("QuickSpendCloud"); }
            catch(IllegalStateException missing){app=FirebaseApp.initializeApp(getContext(),options,"QuickSpendCloud");}
            auth=FirebaseAuth.getInstance(app); database=FirebaseFirestore.getInstance(app);
        } catch(Exception absent) { auth=null; database=null; }
    }
    private JSObject account() {
        JSObject result=new JSObject();result.put("configured",auth!=null&&database!=null);
        FirebaseUser user=auth==null?null:auth.getCurrentUser();
        if(user!=null){result.put("uid",user.getUid());result.put("email",user.getEmail());result.put("name",user.getDisplayName());}
        return result;
    }
    @PluginMethod public void status(PluginCall call){call.resolve(account());}
    @PluginMethod public void signIn(PluginCall call) {
        if(auth==null){call.reject("Google sign-in is not available in this build yet.");return;}
        getActivity().runOnUiThread(()->{
            if(signingIn){call.reject("Sign-in is already in progress.");return;} signingIn=true;
            GetSignInWithGoogleOption option=new GetSignInWithGoogleOption.Builder(webClient).build();
            GetCredentialRequest request=new GetCredentialRequest.Builder().addCredentialOption(option).build();
            CredentialManager.create(getActivity()).getCredentialAsync(getActivity(),request,new CancellationSignal(),ContextCompat.getMainExecutor(getActivity()),new CredentialManagerCallback<GetCredentialResponse,GetCredentialException>(){
                @Override public void onResult(GetCredentialResponse response){
                    try {
                        if(!(response.getCredential() instanceof CustomCredential))throw new IllegalStateException();
                        CustomCredential credential=(CustomCredential)response.getCredential();
                        if(!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType()))throw new IllegalStateException();
                        GoogleIdTokenCredential token=GoogleIdTokenCredential.createFrom(credential.getData());
                        auth.signInWithCredential(GoogleAuthProvider.getCredential(token.getIdToken(),null)).addOnCompleteListener(task->{
                            signingIn=false;
                            if(task.isSuccessful())call.resolve(account());else call.reject("Google sign-in failed. Please try again.");
                        });
                    }catch(Exception invalid){signingIn=false;call.reject("Google sign-in failed. Please try again.");}
                }
                @Override public void onError(GetCredentialException error){signingIn=false;call.reject("Sign-in was cancelled or could not finish. You can try again.");}
            });
        });
    }
    @PluginMethod public void signOut(PluginCall call) {
        if(auth!=null)auth.signOut();
        CredentialManager.create(getContext()).clearCredentialStateAsync(new ClearCredentialStateRequest(),new CancellationSignal(),ContextCompat.getMainExecutor(getContext()),new CredentialManagerCallback<Void,ClearCredentialException>(){
            @Override public void onResult(Void result){call.resolve(account());}
            @Override public void onError(ClearCredentialException error){call.resolve(account());}
        });
    }
    private String user(PluginCall call) {
        FirebaseUser user=auth==null?null:auth.getCurrentUser();
        String expected=call.getString("uid","");
        if(user==null||!user.getUid().equals(expected)){call.reject("Sign in again before using cloud saving.");return null;}
        return user.getUid();
    }
    private DocumentReference backup(String uid){return database.collection("users").document(uid).collection("backups").document("latest");}
    @PluginMethod public void download(PluginCall call) {
        String uid=user(call);if(uid==null)return;
        backup(uid).get(Source.SERVER).addOnSuccessListener(document->{
            if(auth.getCurrentUser()==null||!uid.equals(auth.getCurrentUser().getUid())){call.reject("Your account changed. Please try again.");return;}
            JSObject result=new JSObject();result.put("exists",document.exists());result.put("revision",document.exists()?document.getLong("revision"):0);
            if(document.exists())result.put("payload",document.getString("payload"));call.resolve(result);
        }).addOnFailureListener(error->call.reject("Could not read your cloud backup. Check your connection and try again."));
    }
    @PluginMethod public void upload(PluginCall call) {
        String uid=user(call);if(uid==null)return;
        String payload=call.getString("payload","");int expected=call.getInt("revision",-1);
        try {
            if(expected<0||payload.getBytes(StandardCharsets.UTF_8).length>800000)throw new IllegalArgumentException();
            JSONObject data=new JSONObject(payload);if(!"quickspend".equals(data.optString("app"))||!data.has("tx"))throw new IllegalArgumentException();
        }catch(Exception invalid){call.reject("This backup is too large or invalid. Use a file backup instead.");return;}
        DocumentReference reference=backup(uid);
        database.runTransaction(transaction->{
            DocumentSnapshot current=transaction.get(reference);Long revision=current.exists()?current.getLong("revision"):0L;
            if(revision==null||revision!=expected)throw new FirebaseFirestoreException("Cloud backup changed.",FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            Map<String,Object> data=new HashMap<>();data.put("payload",payload);data.put("revision",expected+1);data.put("updatedAt",FieldValue.serverTimestamp());transaction.set(reference,data);return expected+1;
        }).addOnSuccessListener(revision->{JSObject result=new JSObject();result.put("revision",revision);call.resolve(result);})
          .addOnFailureListener(error->{if(error instanceof FirebaseFirestoreException&&((FirebaseFirestoreException)error).getCode()==FirebaseFirestoreException.Code.FAILED_PRECONDITION)call.reject("Cloud backup changed on another device. Restore it before saving.");else call.reject("Cloud save failed. Check your connection and try again.");});
    }
}
