package com.example.user.testserver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;


import android.app.MediaRouteButton;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    // SharedPreferences에 저장할 때 key 값으로 사용됨
    public static final String PROPERTY_REG_ID = "registration_id";

    // SharedPreferences에 저장할 때 key 값으로 사용됨
    public static final String PROPERTY_APP_VERSION = "appVersion";

    static String SENDER_ID = "565254922586"; // 프로젝트 아이디
    static String SERVER_URL = "http://116.120.119.222:8080/TestServer/sendGCMReg.jsp"; // 서버 주소
    GoogleCloudMessaging gcm;
    Context context;
    String regid;
    private Button reg_btn, push_list_btn, count_push_list_btn, setting_btn, chart_btn, barchart_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        context = this;

        reg_btn = (Button) findViewById(R.id.button);
        reg_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpUtil hu = new HttpUtil(context);
                String[] params = {SERVER_URL, "KEY:1234", "REG:" + regid};
                hu.execute(params);
            }
        });

        push_list_btn = (Button) findViewById(R.id.pushlistbtn);
        push_list_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,PushListActivity.class);	//MainActivity ��(this) sub�� .
                startActivity(intent);
            }
        });

        count_push_list_btn = (Button) findViewById(R.id.countpushlistbtn);
        count_push_list_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ExpandableListForItems.class);	//MainActivity ��(this) sub�� .
                startActivity(intent);
            }
        });

        setting_btn = (Button) findViewById(R.id.settingbtn);
        setting_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SettingActivity.class);	//MainActivity ��(this) sub�� .
                startActivity(intent);
            }
        });

        chart_btn = (Button) findViewById(R.id.chartbtn);
        chart_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,ChartActivity.class);	//MainActivity ��(this) sub�� .
                startActivity(intent);
            }
        });

        barchart_btn = (Button) findViewById(R.id.barchartbtn);
        barchart_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,BarChartActivity.class);	//MainActivity ��(this) sub�� .
                startActivity(intent);
            }
        });

        // gcm 등록
        gcm = GoogleCloudMessaging.getInstance(this); // GoogleCloudMessaging 클래스의 인스턴스를 생성한다
        regid = getRegistrationId(context); // 기존에 발급받은 등록 아이디를 가져온다

        if (regid.isEmpty()) { // 기존에 발급된 등록 아이디가 없으면 registerInBackground 메서드를 호출해 GCM 서버에 발급을 요청한다.
            System.out.println("************************************************* gcm 발급");
            registerInBackground();
        }

        System.out.println("************************************************* gcm regid : " + regid);

    }
    // 저장된 reg id 조회
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context); // 이전에 저장해둔 등록 아이디를 SharedPreferences에서 가져온다.
        String registrationId = prefs.getString(PROPERTY_REG_ID, ""); // 저장해둔 등록 아이디가 없으면 빈 문자열을 반환한다.
        if (registrationId.isEmpty()) {
            System.out.println("************************************************* Registration not found.");
            return "";
        }

        // 앱이 업데이트 되었는지 확인하고, 업데이트 되었다면 기존 등록 아이디를 제거한다.
        // 새로운 버전에서도 기존 등록 아이디가 정상적으로 동작하는지를 보장할 수 없기 때문이다.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) { // 이전에 등록 아이디를 저장한 앱의 버전과 현재 버전을 비교해 버전이 변경되었으면 빈 문자열을 반환한다.
            System.out.println("************************************************* App version changed.");
            return "";
        }
        return registrationId;
    }

    private SharedPreferences getGCMPreferences(Context context) {
        return getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    // reg id 발급
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    // 서버에 발급받은 등록 아이디를 전송한다.
                    // 등록 아이디는 서버에서 앱에 푸쉬 메시지를 전송할 때 사용된다.
                    sendRegistrationIdToBackend();

                    // 등록 아이디를 저장해 등록 아이디를 매번 받지 않도록 한다.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                System.out.println("****************************************************************************** msg : " + msg);
            }

        }.execute(null, null, null);
    }

    // SharedPreferences에 발급받은 등록 아이디를 저장해 등록 아이디를 여러 번 받지 않도록 하는 데 사용
    private void storeRegistrationId(Context context, String regid) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        System.out.println("************************************************* Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regid);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    // 등록 아이디를 서버(앱이랑 통신하는 서버)에 전달
    // 서버는 이 등록 아이디를 사용자마다 따로 저장해두었다가 특정 사용자에게 푸쉬 메시지를 전송할 때 사용할 수 도 있음
    private void sendRegistrationIdToBackend() {
        System.out.println("************************************************* 서버에 regid 전달 : " + regid);

        HttpUtil hu = new HttpUtil(context);
        String[] params = {SERVER_URL, "KEY:1234", "REG:" + regid};
        hu.execute(params);
    }

    // 토스트 생성 함수
    public void printToast(String txt) {
        Toast.makeText(this, txt, Toast.LENGTH_SHORT).show();
    }
}
