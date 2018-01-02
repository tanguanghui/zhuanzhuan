
/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vicmob.micropowder.http;

import android.support.v4.util.ArrayMap;

import java.util.List;

import okhttp3.MultipartBody;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;
import retrofit2.http.Part;
import retrofit2.http.Query;
//MsgNickname
public interface MainService {
    @FormUrlEncoded
    @POST("lz/lz_userinfo.php")
    Call<ResponseBody> upload_msg_json1(
            @Field("deviceId") String deviceId,
            @Field("msgJson") String msgJson,
            @Field("FmsgJson") String FmsgJson,
            @Field("unReadJson") String unReadJson,
            @Field("uNameJson") String contactNameJson
    );

    @FormUrlEncoded
    @POST("lz/lz_user_logo.php")
    Call<ResponseBody> upload_msg_json(
            @Field("deviceId") String deviceId,
            @Field("msgJson") String msgJson,
            @Field("FmsgJson") String FmsgJson,
            @Field("unReadJson") String unReadJson,
            @Field("uNameJson") String contactNameJson,
            @Field("imgLogoJson") String imgLogoJson
    );

    @FormUrlEncoded
    @POST("lz/get_msg.php")
    Call<ResponseBody> get_msg(
//            @Field("nickname") String nickname,
            @Field("deviceId") String mDeviceId,
            @Field("content") String mMsgContent
    );
    @FormUrlEncoded
    @POST("lz/get_msg_all.php")
    Call<ResponseBody> get_msg_all(
            @Field("deviceId") String deviceId
    );
//    @POST("upload_msg.php")
//    Call<ResponseBody> upload_last_msg(
//            @Field("unReadCount") int unReadCount,
//            @Field("isSend") int isSend,
//            @Field("deviceId") String deviceId,
//            @Field("conversationTime") String conversationTime,
//            @Field("content") String content
//    );
// http://192.168.1.122/lz/lz_userinfo.php  lz_info  jsonInfo


}


