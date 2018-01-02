package vicmob.micropowder.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.ResponseBody;
import retrofit2.Response;
import vicmob.micropowder.config.MyApplication;
import vicmob.micropowder.daoman.bean.AutoSendMessageBean;
import vicmob.micropowder.daoman.dao.AutoSendMessageDb;
import vicmob.micropowder.http.ServiceFactory;
import vicmob.micropowder.ui.activity.ChatReplyActivity;

public class LoadUnReadMsgService extends Service {
    public static final String TAG = "tgh";
    public String mDeviceId;
    Timer timer;
    TimerTask mTimerTask;
    private List<AutoSendMessageBean> mAutoSendBeanList = new ArrayList<>();
    public LoadUnReadMsgService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        //执行函数入口；

            LoadUnReadMsg();
        return super.onStartCommand(intent, flags, startId);
    }

    public void LoadUnReadMsg(){
        timer = new Timer();
        //查询回复消息的数据库

        mTimerTask =new TimerTask() {
            @Override
        public void run() {
                mDeviceId= new MyApplication().getDeviceID().trim();
                AutoSendMessageDb mASMDb = new AutoSendMessageDb(LoadUnReadMsgService.this);
                mAutoSendBeanList = mASMDb.queryForAll();
                //初始化数据库倒叙显示！！！
                Collections.reverse(mAutoSendBeanList);
                //查询本地地址数据库
                if (mAutoSendBeanList.size() > 0) {
                    mAutoSendBeanList.clear();
                }
        ////begin response   请求获取需要回复的消息内容；
        retrofit2.Call<ResponseBody> callBrand1 = ServiceFactory.getMainIns().get_msg_all(mDeviceId);

        callBrand1.enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    JSONArray MsgData= null;
                    try {

                        String Jsondata = response.body().string();
//                        Log.i(TAG,"消息内容："+ Jsondata);
                        JSONObject jo = new JSONObject(Jsondata);
                        String code = jo.getString("code");
                        if (code.equals("200")) {
                            //使用JSONObject
                            MsgData= jo.getJSONArray("data");
                            Log.i(TAG,"Msg消息内容："+ MsgData.toString());
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
                                AutoSendMessageDb mASMDb = new AutoSendMessageDb(LoadUnReadMsgService.this);
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
        };
        timer.schedule(mTimerTask, 0, 15000);//20秒一获取

    }


}
