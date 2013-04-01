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

import java.util.Arrays;

/**
 * Элемент структуры для анализа возникновения флуда
 *
 * @author Prolubnikov Dmitry
 * @author Black Kot *LAZY*
 *
 */
public class FloodElement {
	private int count=0;
	private long time=0;
//	private String lastMsg="";
    private String[] lastMsg = null;
    private int countLastMsg = 0;
    private long timeLimit = 0;

    /**
     * Новый элемент анализа флуда.
     * @param t - период, чаще которого нельзя слать одинаковые сообщения
     * @param count - количество сообщений для проверки
     */
	public FloodElement(long t, int count) {
        timeLimit = t;
        countLastMsg = count;
    }

    /**
     * Принято новое сообщение
     * @param s - текст сообщения
     * @return - число одинаковых сообщений с периодом меньше лимита
     */
	public int addMsg(String str) {
        if ((System.currentTimeMillis() - time) >= timeLimit) {
            count = 0;
        } else {
			count++;
			time = System.currentTimeMillis();
			return count;
		}
		String s = str.toLowerCase();
        if (lastMsg == null) {
			lastMsg = new String[]{s};
        } else if (isMatchingMsg(s)) {
			count++;
        } else if(lastMsg.length < countLastMsg) {
			String[] tmp = lastMsg;
			lastMsg = Arrays.copyOf(tmp, tmp.length + 1);
			lastMsg[lastMsg.length] = s;
        } else {
            String[] tmp = lastMsg;
            lastMsg = new String[tmp.length];
            lastMsg[0] = s;
            for (int i = 1, j = 0; i < tmp.length; i++, j++) {
                lastMsg[i] = tmp[j];
            }
        }
        time = System.currentTimeMillis();
        return count;
    }

    /**
     * Время прошедшее с последнего сообщения
     * @return
     */
	public long getDeltaTime(){
		return System.currentTimeMillis()-time;
    }

    /**
     * Число одинаковых сообщений с периодом меньше лимита
     * @return
     */
	public int getCount(){
        return count;
    }

    /**
     * Время последнего сообщения
     * @return
     */
	public long getLastTime(){
        return time;
    }

    /**
     * сообщение совпадает с предыдущими?
     * @param s
     * @return
     */
	public boolean isDoubleMsg(String s) {
	return addMsg(s) != 0;
    }

    /**
     * Возвращает последнее отправленное сообщение
     *
     * @return
     */
	public String[] getLastMsg() {
        return lastMsg;
    }

	private boolean isMatchingMsg(String msg) {
        for (String str : lastMsg) {
            if (str.contains(msg) || msg.contains(str)) {
                return true;
            }
        }
        return false;
    }
}
