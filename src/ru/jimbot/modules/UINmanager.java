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

package ru.jimbot.modules;

import java.util.HashMap;
import ru.jimbot.protocol.IcqProtocol;
import ru.jimbot.protocol.Protocol;
import ru.jimbot.protocol.XmmpProtocol;

/**
 *
 * @author Black_Kot
 */
public class UINmanager implements Runnable {
    private Thread th;
    public HashMap<String,Protocol> proc;

    /** Creates a new instance of UINmanager */
    public UINmanager(String[] ic, String[] ps, AbstractProps psp) {
        proc = new HashMap<String,Protocol>(psp.uinCount());
        for(int i=0;i<ic.length;i++){
            if(isuin(ic[i])){
            IcqProtocol iprot = new IcqProtocol(psp);
            iprot.server="login.icq.com";
            iprot.screenName = ic[i];
            iprot.password = ps[i];
            iprot.baseUin=ic[i];
            proc.put(ic[i], iprot);
            }else{
            XmmpProtocol xmmp = new XmmpProtocol(psp);
            xmmp.port=5222;
            xmmp.server=ic[i].split("@")[1];
            xmmp.screenName = ic[i].split("@")[0];
            xmmp.password = ps[i];
            xmmp.baseUin=ic[i];
            proc.put(ic[i], xmmp);
            }
        }
    }

    public void stopс() {
        for(Protocol p:proc.values()){
            p.disconnect();
        }
    }

    public void start(){
        th = new Thread(this);
        th.setPriority(Thread.NORM_PRIORITY);
        th.start();
    }

    public synchronized void stop() {
        th = null;
        notify();
    }

    @Override
    public void run() {
        for(Protocol p:proc.values()){
            p.connect();
            if(isuin(
                p.baseUin))
             try {
                Thread.sleep(22000);
            } catch (InterruptedException e) {}
           }
        stop();
    }

    public static boolean isuin(String uin){
        try{
            Integer.parseInt(uin);
        }catch(Exception e){
            return false;
        }
        return true;
    }

    public void update(Protocol p){
        proc.put(p.baseUin, p);
    }


}
