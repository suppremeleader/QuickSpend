package com.quickspend.app;
import java.util.Set;
/** Appearance and device-audio gates, independent of the capture ledger. */
public final class CaptureSoundPolicy {
    private CaptureSoundPolicy() {}
    public static boolean announce(boolean appeared,Set<String> current,Set<String> seen){
        if(!appeared||current.isEmpty())return false;
        for(String key:current)if(!seen.contains(key))return true;
        return false;
    }
    public static boolean audible(boolean normalRinger,int volume,boolean noDnd){return normalRinger&&volume>0&&noDnd;}
}
