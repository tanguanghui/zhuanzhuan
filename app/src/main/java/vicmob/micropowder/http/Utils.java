package vicmob.micropowder.http;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vicmob.earn.R;

/**
 * Created by tgh on 1/22/16.
 */

public class Utils {
    private static Toast mToast;
    private static final float DENSITY = Resources.getSystem().getDisplayMetrics().density;
    private static long lastClickTime;
    private static long lastSubmitClickTime;

    public static int dp2px(int dp) {
        return Math.round(dp * DENSITY);
    }

    public static int px2dp(int px) {
        return Math.round(px / DENSITY);
    }

    public static String subZeroAndDot(String s) {
        if (s.indexOf(".") > 0) {
            s = s.replaceAll("0+?$", "");//去掉多余的0
            s = s.replaceAll("[.]$", "");//如最后一位是.则去掉
        }
        return s;
    }

    public synchronized static boolean isFastClick() {
        long time = System.currentTimeMillis();
        if (time - lastClickTime < 2000) {
            return true;
        }
        lastClickTime = time;
        return false;
    }

    public synchronized static boolean isFastClickForSubmit() {
        long time = System.currentTimeMillis();
        if (time - lastSubmitClickTime < 5000) {
            return true;
        }
        lastSubmitClickTime = time;
        return false;
    }

    public static Uri getOutputMediaFileUri() {
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp
                + ".jpg");

        return Uri.fromFile(mediaFile);
    }

    public static boolean isPhoneNumber(String phoneNumber) {
        Pattern pattern = Pattern.compile("^1[0-9]{10}$");
        Matcher matcher = pattern.matcher(phoneNumber);
        return matcher.matches();
    }

    public static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnectedOrConnecting();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Boolean translucentSystemBar(Activity activity, boolean translucent_status_bar, boolean translucent_navigation_bar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = null;
            if (activity != null)
                window = activity.getWindow();
            // Translucent status bar
            if (translucent_status_bar) {
                if (window != null)
                    window.setFlags(
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }
            // Translucent navigation bar
            if (translucent_navigation_bar) {
                if (window != null)
                    window.setFlags(
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }
            return true;
        }
        return false;
    }

    public static boolean VerifyPhoneNumber(String phoneStr) {
        Pattern p = Pattern
                .compile("(^(((13[0-9])|(14[0-9])|(15([0-3]|[5-9]))|(17[0-9])|(18[0-9]))\\d{8})$)");
        Matcher m = p.matcher(phoneStr);
        return m.matches();
    }

    /**
     * 身份证验证
     *
     * @param idCardNum
     * @return
     */
    public static boolean isIdCardNum(String idCardNum) {

        Pattern p = Pattern.compile("^[1-9]\\d{5}[1-9]\\d{3}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}([0-9]|X)$");

        Matcher m = p.matcher(idCardNum);
        return m.matches();
    }

    /**
     * 保留2位小数
     *
     * @param number
     * @return
     */
    public static String saveTwoPointNumber(Float number) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");// 构造方法的字符格式这里如果小数不足2位,会以0补足.
        return decimalFormat.format(number);
    }


//    public static boolean VerifyPhoneNumber(String phoneStr) {
//        Pattern p = Pattern
//                .compile("(^(((13[0-9])|(14[0-9])|(15([0-3]|[5-9]))|(17[0-9])|(18[0-9]))\\d{8})$)");
//        //.compile("^(((13[0-9])|(14[0-9])|(15([0-9]))|(18[0-9])|(17[0-9])\\d{8}))");
//        Matcher m = p.matcher(phoneStr);
//        return m.matches();
//    }

    /**
     * 身份证验证
     *
     * @param cardNum
     * @return
     */
    public static boolean CardNum(String cardNum) {

        Pattern p = Pattern.compile("^[1-9]\\d{5}[1-9]\\d{3}((0\\d)|(1[0-2]))(([0|1|2]\\d)|3[0-1])\\d{3}([0-9]|X)$");

        Matcher m = p.matcher(cardNum);
        return m.matches();
    }

    /**
     * 邮箱验证
     *
     * @param email
     * @return
     */
    public static boolean isEmail(String email) {

        String str = "^([a-zA-Z0-9]*[-_]?[a-zA-Z0-9]+)*@([a-zA-Z0-9]*[-_]?[a-zA-Z0-9]+)+[\\.][A-Za-z]{2,3}([\\.][A-Za-z]{2})?$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(email);

        return m.matches();
    }

    //验证邮政编码
    public static boolean checkPost(String post) {
        if (post.matches("[1-9]\\d{5}(?!\\d)")) {
            return true;
        } else {
            return false;
        }
    }

    public static int getToolbarHeight(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{R.attr.actionBarSize});
        int toolbarHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        return toolbarHeight;
    }

    /**
     * 去掉\t ,\r ,\n
     */
    public static String replaceBlank(String str) {
        String dest = "";
        if (str != null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }

}
