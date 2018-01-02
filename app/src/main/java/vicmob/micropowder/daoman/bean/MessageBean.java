package vicmob.micropowder.daoman.bean;
import java.util.List;
/**
 * Created by qq944 on 2017/9/29.
 */

public class MessageBean {
   private String rconversationId;
   private String isSend;
   private String deviceId;
   private String username;
   private String unReadCount;
   private String conversationTime;
   private String receiveTime;
   private String content;

    public String getRconversationId() {
        return rconversationId;
    }

    public String getIsSend() {
        return isSend;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getUsername() {
        return username;
    }

    public String getUnReadCount() {
        return unReadCount;
    }

    public String getConversationTime() {
        return conversationTime;
    }

    public String getReceiveTime() {
        return receiveTime;
    }

    public String getContent() {
        return content;
    }

    public void setRconversationId(String rconversationId) {
        this.rconversationId = rconversationId;
    }

    public void setIsSend(String isSend) {
        this.isSend = isSend;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUnReadCount(String unReadCount) {
        this.unReadCount = unReadCount;
    }

    public void setConversationTime(String conversationTime) {
        this.conversationTime = conversationTime;
    }

    public void setReceiveTime(String receiveTime) {
        this.receiveTime = receiveTime;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "MessageBean{" +
                "rconversationId='" + rconversationId + '\'' +
                ", isSend='" + isSend + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", username='" + username + '\'' +
                ", unReadCount='" + unReadCount + '\'' +
                ", conversationTime='" + conversationTime + '\'' +
                ", receiveTime='" + receiveTime + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
