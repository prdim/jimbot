/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.jimbot.protocol;

import java.util.Timer;
import java.util.TimerTask;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import ru.jimbot.modules.AbstractProps;
import ru.jimbot.modules.MsgOutQueue;
import ru.jimbot.util.Log;
import ru.jimbot.util.MainProps;

/**
 *
 * @author black-kot
 */
public class XmmpProtocol
extends Protocol
implements MessageListener,ChatManagerListener,ConnectionListener {

private XMPPConnection con;
public AbstractProps  psp;
private boolean connected = false;
public XmmpProtocol(AbstractProps props, int num) {
psp = props;
mq = new MsgOutQueue(this, props.getIntProperty("bot.pauseOut"),
                props.getIntProperty("bot.pauseRestart"),
                props.getIntProperty("bot.msgOutLimit"),num);
}
//@Override
//public ChatServer getChatServer(){
//    return srv;
//}

public void connect() {
    mq.start();
    doConnect();
  }

public void reConnect() {
doDisconnect();
doConnect();
}

public void disconnect() {
    mq.stop();
    doDisconnect();
}

public boolean isOnLine() {
if(con == null) return false;
return connected;
}

public void sendMsg(String to, String message) {
        try {
        Chat chat = con.getChatManager().getThreadChat(to);
	if(chat==null)chat = con.getChatManager().createChat(to,to, this);
        chat.sendMessage(message);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
    }

    private void doConnect() {
        try {
            ConnectionConfiguration config = new ConnectionConfiguration(server, port, server);
            SASLAuthentication.supportSASLMechanism("PLAIN");
            con = new XMPPConnection(config);
            con.connect();
            con.login(screenName, password);
            con.getChatManager().addChatListener(this);
            setStatus(true, psp.getStringProperty("icq.STATUS_MESSAGE2"));
            Log.info(baseUin + " Online!");
            connected = true;
        } catch (XMPPException ex) {
            ex.printStackTrace();
        }
    }

    private void doDisconnect() {
        try {
//            System.out.println(screenName + " Disconnect...");
      con.disconnect();
      con.getChatManager().removeChatListener(this);
      con = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        connected = false;
    }
    
    @Override
    public void processMessage(Chat chat, Message msg) {
             if(msg.getType() == Message.Type.chat){
       String SenderID = chat.getParticipant().split("/")[0];
       String msg0 = msg.getBody();

              if(MainProps.isIgnor(SenderID)){
			Log.flood2("IGNORE LIST: " + SenderID + "->" + screenName + ": " + msg0);
			return;
		}
//        getMsg(SenderID,screenName,msg0);
//              Log.info(SenderID.split("/")[0]+" : "+msg0);
        protList.getMsg(SenderID, screenName, msg0,false);
     }
    }

    @Override
    public void chatCreated(Chat chat, boolean bln) {
                    if (!bln){
                chat.addMessageListener(this);
            }
    }

    public void setStatus(boolean available, String status) {
        Presence.Type type = Type.available;
        Presence.Mode mode = Mode.chat;
        Presence presence = new Presence(type, status, 30, mode);
        con.sendPacket(presence);
     }

    @Override
    public void RemoveContactList(String uin) {
    }

    @Override
    public void addContactList(String uin) {
    }

    @Override
    public void connectionClosed() {
        reconnectingIn(15);
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        e.printStackTrace();
        reconnectingIn(20);
    }

    @Override
    public void reconnectingIn(int seconds) {
        Timer tm = new Timer();
        TimerTask task = new TimerTask() {
    public void run()
    {
    reConnect();
    cancel();
    }
  };
        tm.schedule(task, 1000 * seconds);
    }

    @Override
    public void reconnectionSuccessful() {
        connected=true;
    }

    @Override
    public void reconnectionFailed(Exception e) {
        e.printStackTrace();
        reconnectingIn(20);
    }

    public boolean isNoAuthUin(String uin){
                return psp.getBooleanProperty("chat.isAuthRequest");
    }

    @Override
    public AbstractProps getProps() {
        return psp;
    }

}
