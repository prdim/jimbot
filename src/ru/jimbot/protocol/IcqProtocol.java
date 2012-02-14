/**
 * JimBot - Java IM Bot
 * Copyright (C) 2006-2009 JimBot project
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ru.jimbot.protocol;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import ru.caffeineim.protocols.icq.core.OscarConnection;
import ru.caffeineim.protocols.icq.exceptions.ContactListOperationException;
import ru.caffeineim.protocols.icq.exceptions.ConvertStringException;
import ru.caffeineim.protocols.icq.integration.OscarInterface;
import ru.caffeineim.protocols.icq.integration.events.ContactListEvent;
import ru.caffeineim.protocols.icq.integration.events.IncomingMessageEvent;
import ru.caffeineim.protocols.icq.integration.events.IncomingUrlEvent;
import ru.caffeineim.protocols.icq.integration.events.LoginErrorEvent;
import ru.caffeineim.protocols.icq.integration.events.MessageErrorEvent;
import ru.caffeineim.protocols.icq.integration.events.MessageMissedEvent;
import ru.caffeineim.protocols.icq.integration.events.OfflineMessageEvent;
import ru.caffeineim.protocols.icq.integration.events.SsiAuthReplyEvent;
import ru.caffeineim.protocols.icq.integration.events.SsiAuthRequestEvent;
import ru.caffeineim.protocols.icq.integration.events.SsiFutureAuthGrantEvent;
import ru.caffeineim.protocols.icq.integration.events.SsiModifyingAckEvent;
import ru.caffeineim.protocols.icq.integration.events.StatusEvent;
import ru.caffeineim.protocols.icq.integration.events.XStatusRequestEvent;
import ru.caffeineim.protocols.icq.integration.events.XStatusResponseEvent;
import ru.caffeineim.protocols.icq.integration.listeners.MessagingListener;
import ru.caffeineim.protocols.icq.integration.listeners.OurStatusListener;
import ru.caffeineim.protocols.icq.integration.listeners.XStatusListener;
import ru.caffeineim.protocols.icq.integration.listeners.ContactListListener;
import ru.jimbot.modules.AbstractProps;
import ru.jimbot.modules.MsgOutQueue;
import ru.jimbot.util.Log;
import ru.jimbot.util.MainProps;

/**
 * @author Prolubnikov Dmitry, Black_Kot
 */
public class IcqProtocol
        extends Protocol
        implements OurStatusListener
, MessagingListener
, XStatusListener
//          ,UserStatusListener
        , ContactListListener
//          ,MetaInfoListener
//          ,MetaAckListener
{

    public AbstractProps props = null;
    public String server = "login.icq.com";
    private OscarConnection con = null;
    private boolean connected = false;

    public IcqProtocol(AbstractProps props, int num) {
		this.props = props;
		server = MainProps.getServer();
		port = MainProps.getPort();
		mq = new MsgOutQueue(this, props.getIntProperty("bot.pauseOut"),
                props.getIntProperty("bot.pauseRestart"),
                props.getIntProperty("bot.msgOutLimit"),num);
//        mq.start();
	}
//
//    public void addListener(MsgReceiver p) {
//        protList = p;
//    }

    public void connect() {
        mq.start();
        doConnect();
    }

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

    public void reConnect() {
        doDisconnect();
        port = MainProps.getPort();
        doConnect();
    }

    public void disconnect() {
        mq.stop();
        doDisconnect();
    }

    public void doConnect() {
        connected = true;
        try {
            con = new OscarConnection(server, port, screenName, password);
            con.addMessagingListener(this);
//      con.addUserStatusListener(this);
            con.addXStatusListener(this);
//      con.addMetaInfoListener(this);
            con.addOurStatusListener(this);
            con.addContactListListener(this);
            con.connect();
        } catch (Exception ex) {
            ex.printStackTrace();
            connected = false;
        }
    }

    public void doDisconnect() {
        connected = false;
        try {
            try {
                con.close();
            } catch (Exception ex) {
            }
            con.removeMessagingListener(this);
//      con.removeUserStatusListener(this);
            con.removeXStatusListener(this);
//      con.removeMetaInfoListener(this);
            con.removeOurStatusListener(this);
            con.removeContactListListener(this);
            con = null;
        } catch (Exception e) {
        }
    }

    public boolean isOnLine() {
        if (con == null) {
            return false;
        }
        return connected;
    }

    public void sendMsg(String sn, String msg) {
        try {
			OscarInterface.sendBasicMessage(con, sn, msg);
//			OscarInterface.sendExtendedMessage(con, sn, msg);
		} catch (ConvertStringException e) {
			Log.info("ERROR send message: " + msg);
			e.printStackTrace();
		}catch(IOException ex){
                    Log.info(screenName+" :disconected by server");
                    reconnectingIn(30);
                }
    }

    @Override
    public void onIncomingMessage(IncomingMessageEvent e) {
        if(e.getMessage().contains("only receives messages from contacts"))return;
        if (MainProps.isIgnor(e.getSenderID())) {
            Log.flood("IGNORE LIST: " + e.getMessageId() + "->" + screenName + ": " + e.getMessage());
            return;
        }
        if (e.getSenderID().equals("1")) {
            Log.error("Ошибка совместимости клиента ICQ. Будет произведена попытка переподключения...");
            connected = false;
            return;
        }
        protList.getMsg(e.getSenderID(), screenName, e.getMessage(),false);
    }

    /**
    UNKNOWN_ERROR           = 0; "Unknown Error"
    BAD_UIN_ERROR           = 1; "Bad UIN.";
    PASSWORD_ERROR          = 2; "Password incorrect.";
    NOT_EXISTS_ERROR        = 3; "This ICQ number does not exist.";
    LIMIT_EXCEEDED_ERROR    = 4; "Rate limit exceeded. Please try to reconnect in a few minutes."
    MAXIMUM_USERS_IP_ERROR  = 5; "The amount of users connected from this IP has reached the maximum."
    OLDER_ICQ_VERSION_ERROR = 6; "You are using an older version of ICQ. Please upgrade."
    CANT_REGISTER_ERROR     = 7; "Can't register on the ICQ network. Reconnect in a few minutes."
     */
    @Override
    public void onAuthorizationFailed(LoginErrorEvent e) {
        Log.error("На uin`не " + screenName + " авторизация с сервером ICQ не удалась. Причина: " + e.getErrorMessage());
        int er =e.getErrorType().getError();
//        if(er!=1&&er!=2&&er!=3){
            reconnectingIn(30);
//        }
        connected = false;
    }

    public void onStatusChange(StatusEvent e) {
        Log.debug("StatusEvent: " + e.getStatusMode());
    }

    @Override
    public void onXStatusRequest(XStatusRequestEvent e) {
        try {
            OscarInterface.sendXStatus(con, props.getIntProperty("icq.xstatus"), props.getStringProperty("icq.STATUS_MESSAGE1"), props.getStringProperty("icq.STATUS_MESSAGE2"), System.currentTimeMillis(), e.getMsgID(), e.getSenderID(), e.getSenderTcpVersion());
        } catch (ConvertStringException ex) {
            ex.printStackTrace();
        }
    }

    public void addContactList(String uin) {
        OscarInterface.findUsersByUIN(con, uin);
        try{
            con.getContactList().addGroup("General");
            con.getContactList().addContact(uin, "General");
        } catch (ContactListOperationException ex) {
//            ex.printStackTrace();
        }
    }

    public void RemoveContactList(String uin) {
        try {
            con.getContactList().removeContact(uin);
        } catch (ContactListOperationException ex) {
////            ex.printStackTrace();
        }
    }

    @Override
    public void onLogin() {
        try {
            OscarInterface.changeStatus(con, props.getIntProperty("icq.status"));
            OscarInterface.changeXStatus(con, props.getIntProperty("icq.xstatus"));
            OscarInterface.changeXStatus(con, props.getIntProperty("icq.xstatus"), props.getIntProperty("icq.client"));
            OscarInterface.requestOfflineMessages(con);
//FullUserInfo s = new FullUserInfo();
//s.setAuthNeed(true);
//s.setWebAware(true);
//s.setNick(props.getProperty("chat.name"));
//s.setNotes(props.getProperty("chat.notes"));
//OscarInterface.SetInfo(con, s, screenName);
            Log.info("UIN - " + screenName + " online");
            connected = true;
//            con.getContactList();
//            OscarInterface.changeStatus(con, psp.getIntProperty("icq.status"));
//            OscarInterface.changePrivateStatus(con);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onLogout(Exception e) {
        Log.error("Разрыв соединения: " + screenName + " - " + server + ":" + port
                + " По причине: " + e.getMessage());
        connected = false;
        reconnectingIn(30);
    }

    @Override
    public void onMessageMissed(MessageMissedEvent e) {
        System.err.println("Message from " + e.getUin() + " can't be recieved because " + e.getReason()
                + " count=" + e.getMissedMsgCount());
    }

    @Override
    public void onMessageError(MessageErrorEvent e) {
        Log.error("Message error " + e.getError().toString());
    }

    @Override
    public void onStatusResponse(StatusEvent se) {
    }

    @Override
    public void onIncomingUrl(IncomingUrlEvent iue) {
    }

    @Override
    public void onOfflineMessage(OfflineMessageEvent ome) {
        protList.getMsg(ome.getSenderUin(), screenName, ome.getMessage(),true);
    }

//    @Override
//    public void onMessageAck(MessageAckEvent mae) {
//    }

    @Override
    public void onXStatusResponse(XStatusResponseEvent e) {
    }

//
//    @Override
//    public void onIncomingUser(IncomingUserEvent iue) {}
//
//    @Override
//    public void onOffgoingUser(OffgoingUserEvent oue) {
//        protList.getStatus(oue.getOffgoingUserId(),0xFF);
//    }
    @Override
    public void onUpdateContactList(ContactListEvent cle) {
    }

    @Override
    public void onSsiModifyingAck(SsiModifyingAckEvent smae) {
    }

    @Override
    public void onSsiFutureAuthGrant(SsiFutureAuthGrantEvent sfage) {
    }

    @Override
    public void onSsiAuthRequest(SsiAuthRequestEvent sare) {

    }

    @Override
    public void onSsiAuthReply(SsiAuthReplyEvent sare) {
    }

    public boolean isNoAuthUin(String uin){
                return props.getBooleanProperty("chat.isAuthRequest");
    }

    @Override
    public AbstractProps getProps() {
        return props;
    }

}
