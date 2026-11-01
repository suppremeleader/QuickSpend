package com.quickspend.app;
public final class UpdatePolicy {
    private UpdatePolicy() { }
    public static final long MAX_VERSION=2100000000L;
    public static long version(String value) {
        try { if(value==null||!value.matches("[1-9][0-9]{0,9}"))return 0;long code=Long.parseLong(value);return code<=MAX_VERSION?code:0; }
        catch(NumberFormatException e){return 0;}
    }
    public static boolean notify(long installed,long offered,long notified,boolean enabled){return enabled&&offered>installed&&offered>notified&&offered<=MAX_VERSION;}
    public static boolean prompt(long offered,long dismissed,long now,long lastTime,boolean manual){return manual||offered>dismissed||now-lastTime>=86400000L;}
}
