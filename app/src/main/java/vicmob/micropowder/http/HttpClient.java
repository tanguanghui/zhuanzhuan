package vicmob.micropowder.http;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
//import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import vicmob.micropowder.config.MyApplication;

class HttpClient {

    private Retrofit retrofit;
    private static HttpClient mInstance;

    private static int READ_TIMEOUT = 300;
    private static int WRITE_TIMEOUT = 300;
    private static int CONNECT_TIMEOUT = 300;

    private HttpClient(String BASE_URL) {

        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(new ItemTypeAdapterFactory())
                .create();

//        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
//        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        File httpCacheDirectory = new File(MyApplication.sContext.getExternalCacheDir(), "responses");
        Cache cache = new Cache(httpCacheDirectory, 10 * 1024 * 1024);

        OkHttpClient client = new OkHttpClient.Builder()
                .cache(cache)
                .readTimeout(READ_TIMEOUT,TimeUnit.SECONDS)//设置读取超时时间
                .writeTimeout(WRITE_TIMEOUT,TimeUnit.SECONDS)//设置写的超时时间
                .connectTimeout(CONNECT_TIMEOUT,TimeUnit.SECONDS)//设置连接超时时间
//                .addInterceptor(interceptor)
                .addInterceptor(new CacheControlInterceptor())
                .addNetworkInterceptor(new CacheControlInterceptor())
                .addInterceptor(
                        new Interceptor() {
                            @Override
                            public Response intercept(Chain chain) throws IOException {
                                Request.Builder build = chain.request().newBuilder();
                                SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(MyApplication.sContext);
                                build.addHeader("PERSONTEL", preference.getString("userData_mobileNumber", ""));
                                build.addHeader("UNIID", preference.getString("userData_uuid", ""));
                                build.addHeader("PERSONID", preference.getString("userData_id", ""));
                                Request request = build.build();
                                return chain.proceed(request);
                            }
                        })
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }
    public static HttpClient getIns(String base_url) {
        if (mInstance == null) {
            synchronized (HttpClient.class) {
                if (mInstance == null) {
                    mInstance = new HttpClient(base_url);
                }
            }
        }
        return mInstance;
    }
    public <T> T createService(Class<T> clz) {
        return retrofit.create(clz);
    }

}