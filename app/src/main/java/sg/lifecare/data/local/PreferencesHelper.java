package sg.lifecare.data.local;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import sg.lifecare.data.remote.model.response.AssistsedEntityResponse;
import sg.lifecare.data.remote.model.response.EntityDetailResponse;
import sg.lifecare.framework.di.ApplicationContext;
import sg.lifecare.utils.CookieUtils;
import timber.log.Timber;


@Singleton
public class PreferencesHelper {

    private static final String PREF_FILE_NAME = "smartears_app_pref_file";

    private static final String PREF_KEY_DEVICE_ID = "DEVICE_ID";
    private static final String PREF_KEY_FCM_TOKEN = "FCM_TOKEN";
    private static final String PREF_KEY_ENTITY_ID = "ENTITY_ID";
    private static final String PREF_KEY_USER_ENTITY = "USER_ENTITY";
    private static final String PREF_KEY_MEMBERS_ENTITY = "MEMBERS_ENTITY";

    private static final String GCM_SENDER_ID = "1076112719492";

    private DeviceData mDeviceData;

    private final SharedPreferences mPref;

    @Inject
    public PreferencesHelper(@ApplicationContext final Context context) {
        mPref = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        mDeviceData = DeviceData.getInstance(context);

        if (TextUtils.isEmpty(getDeviceId())) {
            setDeviceId(context);
        }
        checkGooglePlayServices(context);
        if (TextUtils.isEmpty(getFcmToken()) && checkGooglePlayServices(context)) {
            Observable.create((ObservableEmitter<String> aSubscriber) -> {
                InstanceID instanceID = InstanceID.getInstance(context);
                try {
                    String token = instanceID.getToken(GCM_SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                    aSubscriber.onNext(token);
                } catch (IOException e) {
                    Timber.e(e, e.getMessage());
                    //aSubscriber.onError(e);
                    aSubscriber.onNext("");
                }
                aSubscriber.onComplete();
            })
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(token -> putFcmToken(token),
                    throwable -> Timber.e(throwable, throwable.getMessage()));
        }

    }

    private boolean checkGooglePlayServices(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);

        if (resultCode == ConnectionResult.SUCCESS) {
            Timber.d("checkGooglePlayServices: true");
            return true;
        }

        Timber.d("checkGooglePlayServices: false");
        return false;

    }

    public void clear(Context context) {
        CookieUtils.getCookieJar(context).clear();
        setEntityId("");
        setUserEntity(null);
        setMembersEntity(null);
    }

    private void putFcmToken(String token) {
        mPref.edit().putString(PREF_KEY_FCM_TOKEN, token).apply();
    }

    public String getFcmToken() {
        return mPref.getString(PREF_KEY_FCM_TOKEN, null);
    }

    private void setDeviceId(Context context) {
        String id = InstanceID.getInstance(context).getId();
        mPref.edit().putString(PREF_KEY_DEVICE_ID, id).apply();
    }

    public String getDeviceId() {
        return mPref.getString(PREF_KEY_DEVICE_ID, null);
    }

    public String getEntityId() {
        return mPref.getString(PREF_KEY_ENTITY_ID, null);
    }

    public EntityDetailResponse.Data getUserEntity() {
        String result = mPref.getString(PREF_KEY_USER_ENTITY, null);

        Timber.d("getUserEntity: result=%s", result);

        if (TextUtils.isEmpty(result)) {
            return null;
        }

        Gson gson = new Gson();
        Type type = new TypeToken<EntityDetailResponse.Data>(){}.getType();
        return gson.fromJson(result, type);
    }

    public List<AssistsedEntityResponse.Data> getMembersEntity() {
        String result = mPref.getString(PREF_KEY_MEMBERS_ENTITY, "");

        if (TextUtils.isEmpty(result)) {
            return null;
        }

        Gson gson = new Gson();
        Type type = new TypeToken<List<AssistsedEntityResponse.Data>>(){}.getType();
        return gson.fromJson(result, type);
    }

    public void setEntityId(String entityId) {
        mPref.edit().putString(PREF_KEY_ENTITY_ID, entityId).apply();
    }

    public void setUserEntity(EntityDetailResponse.Data userEntity) {

        Gson gson = new Gson();
        String result = "";

        if (userEntity != null) {
            result = gson.toJson(userEntity);
        }

        //Timber.d("setUserEntity: result\n%s", result);
        mPref.edit().putString(PREF_KEY_USER_ENTITY, result).apply();
    }

    public void setMembersEntity(List<AssistsedEntityResponse.Data> membersEntity) {
        Gson gson = new Gson();
        String result = "";

        if (membersEntity != null && membersEntity.size() > 0) {
            result = gson.toJson(membersEntity);
        }

        mPref.edit().putString(PREF_KEY_MEMBERS_ENTITY, result).apply();
    }

    public DeviceData getDeviceData() {
        return mDeviceData;
    }

}