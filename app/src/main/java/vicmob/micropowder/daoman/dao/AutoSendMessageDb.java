package vicmob.micropowder.daoman.dao;

import android.content.Context;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import vicmob.micropowder.daoman.DBHelper;
import vicmob.micropowder.daoman.bean.AutoSendMessageBean;


/**
 * author: Twisted
 * created on: 2017/7/20 15:26
 * description:
 */

public class AutoSendMessageDb {
    private DBHelper mDbHelper;
    private Dao<AutoSendMessageBean, Integer> mAutoSendMessageDb;

    public AutoSendMessageDb(Context context) {
            mDbHelper =  new DBHelper(context);
        try {
            mAutoSendMessageDb = mDbHelper.getDao(AutoSendMessageBean.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    /**
     * 添加一条数据
     */
    public void add(AutoSendMessageBean sendMessageBean) {
        try {
            mAutoSendMessageDb.create(sendMessageBean);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除一条记录
     *
     * @param
     */
    public void delete(AutoSendMessageBean autoSendMessageBean) {
        try {
            mAutoSendMessageDb.delete(autoSendMessageBean);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * 更新一条记录
     *
     * @param
     */
    public void update(AutoSendMessageBean autoSendMessageBean) {
        try {
            mAutoSendMessageDb.update(autoSendMessageBean);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询一条记录
     *
     * @param id
     * @return
     */
    public AutoSendMessageBean queryForId(int id) {
        AutoSendMessageBean sendMessage = null;
        try {
            sendMessage = mAutoSendMessageDb.queryForId(id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sendMessage;
    }


    /**
     * 查询所有记录
     *
     * @return
     */
    public List<AutoSendMessageBean> queryForAll() {
        List<AutoSendMessageBean> mSendMessageList = new ArrayList<AutoSendMessageBean>();
        try {
            mSendMessageList = mAutoSendMessageDb.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mSendMessageList;
    }

    /**
     * 删除所有记录
     */
    public List<AutoSendMessageBean> deleteAll() {
        List<AutoSendMessageBean> mSendMessage = new ArrayList<AutoSendMessageBean>();
        try {
            for (int i = 0; i < mSendMessage.size(); i++) {
                mAutoSendMessageDb.delete(mSendMessage.get(i));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mSendMessage;
    }

    /**
     * 简单查询特殊字段是否存在
     *
     * @param parameter
     * @return
     */
    public boolean queryOne(String parameter) {
        List<AutoSendMessageBean> mSendMessageBeanList = null;
        try {
            mSendMessageBeanList = mAutoSendMessageDb.queryBuilder().where().eq("responseMessage", parameter).query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mSendMessageBeanList.size() != 0 ? true : false;
    }
    /**
     * 查询字段的内容是否存在
     *
     * @param param1 and param2
     * @return
     */
    public boolean queryMsg(String param1,String param2) {
        List<AutoSendMessageBean> mSendMessageBeanList = null;
        try {
            mSendMessageBeanList = mAutoSendMessageDb.queryBuilder().where().eq("responseMessage", param1).and().eq("userName", param2).query();


        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mSendMessageBeanList.size() != 0 ? true : false;
    }


}
