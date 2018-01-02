package vicmob.micropowder.http;

import vicmob.earn.BuildConfig;

public class ServiceFactory {

    private static MainService mService;

    public static MainService getMainIns() {
        if (mService == null) {
            mService = HttpClient.getIns(BuildConfig.BASE_URL).createService(MainService.class);
        }
        return mService;
    }

}