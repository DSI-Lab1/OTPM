/**
 * Copyright (C), 2015-2020, HITSZ
 * FileName: UtilityList_New
 * Author:   qj Dai
 * Date:     2020/11/20 16:51
 * Description: Add a new domain "prel" into UtilityList.
 */

package TUSQ;

import java.util.ArrayList;
import java.util.List;

public class TargetedList {
    public class UtilityElement {

        /** itemset  **/
        public int tid;

        /** 效用 **/
        public int acu;

        /** 剩余效用 **/
        public int ru;

        public UtilityElement(int tid, int acu, int ru) {
            this.tid=tid;
            this.acu=acu;
            this.ru=ru;
        }
    }

    //sid
    public int sid;

    //prel：当前模式包含目标序列的前缀的长度
    public int prel;

    //UtilityList:
    List<UtilityElement> List = new ArrayList<UtilityElement>();

    //length
    public int LengthOfUtilityList;

    //SRU：Suffix Remain Utility
    public int SRU;

    public TargetedList() {
        this.LengthOfUtilityList=0;
        this.SRU = 0;
    }

    public void add(int tid, int acu, int ru) {
        this.List.add(new UtilityElement(tid, acu, ru));
        this.LengthOfUtilityList++;
    }

    public void set_SRU(int sru) {
        this.SRU = sru;
    }

    public void set_sid(int sid) {
        this.sid = sid;
    }

    public int get_sid() {
        return this.sid;
    }

    public void set_prel(int prel) {
        this.prel = prel;
    }

    public int get_prel() {
        return this.prel;
    }
}