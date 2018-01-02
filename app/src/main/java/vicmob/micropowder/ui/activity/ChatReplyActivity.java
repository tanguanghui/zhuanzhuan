package vicmob.micropowder.ui.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sevenheaven.iosswitch.ShSwitchView;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Response;
import vicmob.earn.R;
import vicmob.micropowder.base.BaseActivity;
import vicmob.micropowder.config.Callback;
import vicmob.micropowder.config.Constant;
import vicmob.micropowder.config.MyApplication;
import vicmob.micropowder.daoman.PxDBHelper;
import vicmob.micropowder.daoman.bean.AutoSendMessageBean;
import vicmob.micropowder.daoman.bean.MessageBean;
import vicmob.micropowder.daoman.dao.AutoSendMessageDb;
import vicmob.micropowder.http.ServiceFactory;
import vicmob.micropowder.service.BaseAccessibilityService;

import vicmob.micropowder.service.LoadUnReadMsgService;
import vicmob.micropowder.ui.views.ConfirmDialog;
import vicmob.micropowder.ui.views.ContentDialog;
import vicmob.micropowder.ui.views.DividerItemDecoration;
import vicmob.micropowder.utils.MyToast;
import vicmob.micropowder.utils.PrefUtils;

/**
 * Created by Eren on 2017/6/23.
 * <p>
 * 附近人
 */
public class ChatReplyActivity extends BaseActivity {
    /**
     * define 控件
     */
    public static final String TAG = "tgh";
    @BindView(R.id.iv_back)
    ImageView mIvBack;
    @BindView(R.id.button_begin)
    Button mButBegin;
    @BindView(R.id.rl_open)
    RelativeLayout mRlOpen;
    @BindView(R.id.iv_begin)
    ShSwitchView mIvBegin;
    /**
     * 全局变量
     */
    private MyApplication app;

    /**
     * 开启服务对话框
     */
    private ConfirmDialog mConfirmDialog;

    /**
     * 自定义Dialog
     */
    private ContentDialog mContentDialog;



    private BaseAccessibilityService mAccessibilityService;

    /**
     * 网络获取实例
     */
    private List<AutoSendMessageBean> mAutoSendBeanList = new ArrayList<>();
    public String mDeviceId;
//    public String mNickname;
//    public String mrMsgContent;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autoresponse);
        ButterKnife.bind(this);

        app = (MyApplication) getApplication();
//        startService(new Intent(ChatReplyActivity.this, LoadUnReadMsgService.class));
//        initDB();
    }

    /**
     * 初始化数据库数据
     */
    private void initDB1() {
        // 获取设备号；

        mDeviceId= new MyApplication().getDeviceID().trim();
        //查询回复消息的数据库
        AutoSendMessageDb mASMDb = new AutoSendMessageDb(ChatReplyActivity.this);
        mAutoSendBeanList = mASMDb.queryForAll();
        //初始化数据库倒叙显示！！！
        Collections.reverse(mAutoSendBeanList);
        //查询本地地址数据库
        if (mAutoSendBeanList.size() > 0) {
            mAutoSendBeanList.clear();
        }


        //从数据库中查出实例，并取出数据
        AutoSendMessageDb mqueryDb = new AutoSendMessageDb(ChatReplyActivity.this);
        mAutoSendBeanList = mqueryDb.queryForAll();

        //  取出 微信号以及对应的文本信息
//                        Log.i(TAG, Integer.toString(mAutoSendBeanList.size()));
//                        Log.i(TAG,  mAutoSendBeanList.get(0).getUserName());
        if(mAutoSendBeanList.size()>0){
            for(int i= 0;i<mAutoSendBeanList.size();i++) {
                String mName = mAutoSendBeanList.get(i).getNickName();
                String mMsg = mAutoSendBeanList.get(i).getResponseMessage();

            }

        }else{
            Log.i(TAG,"暂时数据库没有数据");
        }




        ////begin response   请求获取需要回复的消息内容；
        retrofit2.Call<ResponseBody> callBrand1 = ServiceFactory.getMainIns().get_msg_all(mDeviceId);

        callBrand1.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    JSONArray  MsgData= null;
                    try {

                        String data = response.body().string();
                        Log.i(TAG, data);
                        JSONObject jo = new JSONObject(data);
                        String code = jo.getString("code");
                        if (code.equals("200")) {
                            //使用JSONObject
                            MsgData= jo.getJSONArray("data");
                            Log.i(TAG,"消息内容："+ MsgData.toString());
                            for(int i=0; i< MsgData.length(); i++){
                                JSONObject mMsgData =  MsgData.getJSONObject(i);
                                String mUserName = mMsgData.getString("username");
                                String mNickName = mMsgData.getString("nickname");
                                String mrMsgContent = mMsgData.getString("content");
                                String mLastTime = mMsgData.getString("conversationTime");
//                                Log.i(TAG, "准备存入"+ mUserName+"***"+mNickName+"**&&*"+mrMsgContent+"&&&&&"+mLastTime);

                                AutoSendMessageBean mASMBean = new AutoSendMessageBean();
                                mASMBean.setResponseMessage(mrMsgContent);
                                mASMBean.setResponseTime(mLastTime);
                                mASMBean.setNickName(mNickName);
                                mASMBean.setUserName(mUserName);
                                //将数据库实例添加到数据库
                                AutoSendMessageDb mASMDb = new AutoSendMessageDb(ChatReplyActivity.this);
                                mASMDb.add(mASMBean);

                            }

                        }
//                                测试数据是否写入数据库；
//                                AutoSendMessageDb wqueryDb = new AutoSendMessageDb(ChatReplyActivity.this);
//                                mAutoSendBeanList = wqueryDb.queryForAll();
//                                Log.i(TAG, Integer.toString(mAutoSendBeanList.size()));
//                                Log.i(TAG,  mAutoSendBeanList.get(0).getUserName());
//                                Collections.reverse(mAutoSendBeanList);

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
            }
        });



    }


    /**
     * 布局可见时调用按钮的开启状态
     */
    @Override
    protected void onStart() {
        super.onStart();

        initOpenServiceState();
    }

    /**
     * 布局交互时调用按钮的监听
     */
    @Override
    public void onResume() {
        super.onResume();

        SwitchChanged();
    }

    @OnClick({R.id.iv_back, R.id.button_begin, R.id.rl_open })
    public void onViewClicked(View view) {

        switch (view.getId()) {
            case R.id.iv_back:
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                outAnimation();     //返回按钮
                break;

            case R.id.button_begin:    //


                ////////////////////////////////////////////
                if (isServiceOpening(ChatReplyActivity.this)) {

                    app.setSelectReply(true);  //开启选择回复模块
                    app.setAllowSelectReply(true);
                    MyToast.show(ChatReplyActivity.this, "请稍等一会儿");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //先执行杀掉微信后台操作
                                mAccessibilityService = new BaseAccessibilityService();
                                mAccessibilityService.execShellCmd("am force-stop com.tencent.mm");
//                                mAccessibilityService.execShellCmd("input keyevent 3");  //KEYCODE_HOME

//                                startMock(mMapSearchBeanList.get(0).getLatitudes(), mMapSearchBeanList.get(0).getLongtitudes(), mMapSearchBeanList.get(0).getLac(), mMapSearchBeanList.get(0).getCid(), mMapSearchBeanList.get(0).getMnc());
                                app.setSelectReply(true);
//                                app.setAllowGreeting(true);
                                try {
                                    Thread.sleep(2500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                intentWechat(); //跳转微信主界面
                            }
                        }).start();



                } else {

                    //跳转服务界面对话框
                    mConfirmDialog = new ConfirmDialog(ChatReplyActivity.this, new Callback() {
                        @Override
                        public void Positive() {
                            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));  //跳转服务界面
                        }

                        @Override
                        public void Negative() {
                            mConfirmDialog.dismiss();   //关闭
                        }
                    });
                    mConfirmDialog.setContent("提示：" + "\n服务没有开启不能进行下一步");
                    mConfirmDialog.setCancelable(true);
                    mConfirmDialog.show();

                }
                break;

            case R.id.rl_open: //服务开关切换
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));  //跳转服务界面
                break;

        }
    }


    /**
     * 开关状态发生改变
     */
    private void SwitchChanged() {
        mIvBegin.setOnSwitchStateChangeListener(new ShSwitchView.OnSwitchStateChangeListener() {
            @Override
            public void onSwitchStateChange(boolean isOn) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));  //跳转服务界面
            }
        });
    }


    /**
     * 初始化服务按钮开启的状态
     */
    private void initOpenServiceState() {
        if (isServiceOpening(ChatReplyActivity.this)) {

            mIvBegin.setOn(true);   //打开

        } else {

            mIvBegin.setOn(false);  //关闭
        }
    }

    /**
     * 调用开始模拟的方法
     *
     * @param latitude
     * @param longitude
     */
//    private void startMock(Double latitude, Double longitude, int lac, int cid, int mnc) {
//
//        mSQLiteDatabase = new PxDBHelper(this).getWritableDatabase();
//        //创建一个集合
//        ContentValues contentValues = new ContentValues();
//        contentValues.put("package_name", "com.tencent.mm");
//        contentValues.put("latitude", latitude);
//        contentValues.put("longitude", longitude);
//        contentValues.put("lac", lac);
//        contentValues.put("cid", cid);
//        contentValues.put("mnc", mnc);
//        //将集合里的参数插入到数据库
//        mSQLiteDatabase.insertWithOnConflict(PxDBHelper.APP_TABLE_NAME, null, contentValues, SQLiteDatabase.CONFLICT_REPLACE);
//    }
}
