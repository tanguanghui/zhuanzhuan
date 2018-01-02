package vicmob.micropowder.daoman.bean;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * author: Twisted
 * created on: 2017/7/20 15:28
 * description:
 */
@DatabaseTable(tableName = "t_auto_send_message")
public class AutoSendMessageBean {
//    id
    @DatabaseField(columnName = "_id", generatedId = true)
    private int _id;
//    微信号
    @DatabaseField(canBeNull = false)
    private String userName;
    //    昵称：
    @DatabaseField(canBeNull = false)
    private String nickName;
//    回复的消息内容
    @DatabaseField(canBeNull = false)
    private String responseMessage;
//    回复时间
    @DatabaseField(canBeNull = false)
    private String responseTime;

    public AutoSendMessageBean() {
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(String responseTime) {
        this.responseTime = responseTime;
    }
}
