package com.quickspend.app;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.JSObject;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

@CapacitorPlugin(name="UpdateBridge")
public class UpdateBridgePlugin extends Plugin {
    private AppUpdateManager manager;
    @Override public void load(){manager=AppUpdateManagerFactory.create(getContext());}
    @PluginMethod public void status(PluginCall call){JSObject result=new JSObject();result.put("enabled",UpdatePush.enabled(getContext()));result.put("allowed",UpdatePush.allowed(getContext()));result.put("configured",UpdatePush.configured(getContext()));call.resolve(result);}
    @PluginMethod public void setAlerts(PluginCall call){try{UpdatePush.setEnabled(getContext(),Boolean.TRUE.equals(call.getBoolean("enabled",true)));status(call);}catch(Exception e){call.reject("Could not save update preferences",e);}}
    @PluginMethod public void check(PluginCall call){
        final boolean manual=Boolean.TRUE.equals(call.getBoolean("manual",false));
        manager.getAppUpdateInfo().addOnSuccessListener(info->{
            boolean downloaded=info.installStatus()==InstallStatus.DOWNLOADED;
            long version=info.availableVersionCode(),required=UpdatePush.recordAvailability(getContext(),info);
            JSObject result=new JSObject();result.put("available",info.updateAvailability()==UpdateAvailability.UPDATE_AVAILABLE&&version>UpdatePush.installed(getContext()));
            result.put("required",required>0);result.put("versionCode",required>0?required:version);result.put("downloaded",downloaded);result.put("flexible",info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE));result.put("installStatus",info.installStatus());
            result.put("promptAllowed",UpdatePolicy.prompt(version,UpdatePush.prefs(getContext()).getLong("dismissed",0),System.currentTimeMillis(),UpdatePush.prefs(getContext()).getLong("dismissed_at",0),manual));call.resolve(result);
        }).addOnFailureListener(error->{
            long required=UpdatePush.required(getContext());
            if(required==0){call.reject("Could not check updates. Open Google Play to check.",error);return;}
            JSObject result=new JSObject();result.put("available",true);result.put("downloaded",false);
            result.put("required",true);result.put("versionCode",required);result.put("promptAllowed",true);call.resolve(result);
        });
    }
    @PluginMethod public void dismiss(PluginCall call){long version=call.getInt("versionCode",0).longValue();UpdatePush.prefs(getContext()).edit().putLong("dismissed",version).putLong("dismissed_at",System.currentTimeMillis()).apply();call.resolve();}
    @PluginMethod public void start(PluginCall call){getActivity().runOnUiThread(()->manager.getAppUpdateInfo().addOnSuccessListener(info->{
        if(info.installStatus()==InstallStatus.DOWNLOADED){call.reject("The update is ready. Tap Restart to install.");return;}
        if(info.updateAvailability()!=UpdateAvailability.UPDATE_AVAILABLE||!info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)){JSObject response=new JSObject();response.put("openedStore",true);call.resolve(response);getActivity().startActivity(UpdatePush.storeIntent(getContext()));return;}
        manager.startUpdateFlow(info,getActivity(),AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build())
            .addOnSuccessListener(result->{
                JSObject response=new JSObject();
                if(result==android.app.Activity.RESULT_OK){UpdatePush.prefs(getContext()).edit().remove("dismissed").remove("dismissed_at").apply();response.put("accepted",true);call.resolve(response);}
                else if(result==android.app.Activity.RESULT_CANCELED){UpdatePush.prefs(getContext()).edit().putLong("dismissed",info.availableVersionCode()).putLong("dismissed_at",System.currentTimeMillis()).apply();response.put("cancelled",true);call.resolve(response);}
                else call.reject("Could not start the update. Try Google Play.");
            }).addOnFailureListener(error->call.reject("Could not start the update. Try Google Play.",error));
    }).addOnFailureListener(error->call.reject("Could not start the update. Try Google Play.",error)));}
    @PluginMethod public void complete(PluginCall call){getActivity().runOnUiThread(()->manager.completeUpdate().addOnSuccessListener(result->call.resolve()).addOnFailureListener(error->call.reject("Could not install the update. Try Google Play.",error)));}
    @PluginMethod public void openStore(PluginCall call){try{getActivity().startActivity(UpdatePush.storeIntent(getContext()));call.resolve();}catch(Exception e){call.reject("Could not open Google Play",e);}}
}
