package com.quickspend.app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A conservative parser for completed payment alerts from recognized UPI apps. */
public final class UpiPayment {
    static final String PHONEPE_PACKAGE = "com.phonepe.app";
    static final String KEY_PREFIX = "upi:";
    static final long DUPLICATE_WINDOW_MS = 120_000L;

    private static final String NAME = "[\\p{L}\\p{M}\\p{N} .&'’()_@/-]{1,80}?";
    private static final String NUMBER = "(?:[0-9]{1,3}(?:,[0-9]{2})*,[0-9]{3}|[0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:\\.[0-9]{1,2})?";
    private static final String MONEY = "(?:₹|INR\\b|Rs\\.?)\\s*(" + NUMBER + ")";
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern PHONEPE_RECEIVED = Pattern.compile(
            "^(" + NAME + ") has sent " + MONEY + " to your bank account (" + NAME + ")[-–]([0-9]{4})\\.?$", FLAGS);
    private static final Pattern REJECT = Pattern.compile(
            "\\b(?:failed|failure|declined|unsuccessful|pending|processing|request(?:ed|ing)?|collect|otp|verification|offer|reward|cashback|scheduled|reminder|mandate|balance|refund|reversal|cancelled|canceled|due|pay now|will be|not paid|not sent|not received|could not)\\b", FLAGS);
    private static final Pattern INCOMING = Pattern.compile(
            "^(?:(?:you (?:have )?)?received " + MONEY + "|" + MONEY + " (?:was )?received) from (" + NAME + ")\\.?$", FLAGS);
    private static final Pattern OUTGOING = Pattern.compile(
            "^(?:(?:you (?:have )?)?(?:paid|sent) " + MONEY + "|" + MONEY + " (?:was )?(?:paid|sent)) to (" + NAME + ")\\.?$", FLAGS);

    public final double amount;
    public final String kind;
    public final String counterparty;
    public final String account;
    public final String app;

    private UpiPayment(double amount, String kind, String counterparty, String account, String app) {
        this.amount = amount;
        this.kind = kind;
        this.counterparty = counterparty;
        this.account = account;
        this.app = app;
    }

    static String appName(String packageName) {
        if (packageName == null) return null;
        switch (packageName) {
            case PHONEPE_PACKAGE: return "PhonePe";
            case "com.google.android.apps.nbu.paisa.user": return "Google Pay";
            case "net.one97.paytm": return "Paytm";
            case "in.org.npci.upiapp": return "BHIM";
            case "com.dreamplug.androidapp": return "CRED";
            case "com.naviapp": return "Navi";
            case "com.supermoney": return "super.money";
            case "com.mobikwik_new": return "MobiKwik";
            case "com.freecharge.android": return "Freecharge";
            case "com.fampay.in": return "FamApp";
            case "com.samsung.android.spay": return "Samsung Wallet";
            case "com.myairtelapp": return "Airtel Thanks";
            case "com.jio.myjio": return "MyJio";
            case "in.amazon.mShop.android.shopping": return "Amazon Pay";
            case "com.whatsapp": return "WhatsApp Pay";
            default: return null;
        }
    }

    static boolean needsUpiMarker(String packageName) {
        return "in.amazon.mShop.android.shopping".equals(packageName) || "com.myairtelapp".equals(packageName)
                || "com.jio.myjio".equals(packageName) || "com.samsung.android.spay".equals(packageName)
                || "com.whatsapp".equals(packageName);
    }

    private static final Pattern UPI_MARKER = Pattern.compile("\\bUPI\\b", FLAGS);
    private static final Pattern WHATSAPP_PAYMENT_TITLE = Pattern.compile("^UPI (?:payment (?:received|sent)|money (?:received|sent))$", FLAGS);

    public static UpiPayment parse(String packageName, String title, String body) {
        String app = appName(packageName);
        if (app == null || title == null || title.length() > 160 || body == null || body.length() > 512) return null;
        String text = body.replaceAll("[\\s\\u00a0]+", " ").trim();
        if (REJECT.matcher(title + " " + text).find()) return null;
        if (PHONEPE_PACKAGE.equals(packageName) && "Money received".equalsIgnoreCase(title.trim())) {
            Matcher matched = PHONEPE_RECEIVED.matcher(text);
            if (matched.matches()) {
                return payment(matched.group(2), "Income", matched.group(1),
                        matched.group(3).trim() + " " + matched.group(4), app);
            }
        }
        if (needsUpiMarker(packageName) && !UPI_MARKER.matcher(title).find()) return null;
        if ("com.whatsapp".equals(packageName) && !WHATSAPP_PAYMENT_TITLE.matcher(title.trim()).matches()) return null;
        Matcher incoming = INCOMING.matcher(text);
        if (incoming.matches() && !title.matches("(?iu).*\\b(?:money sent|payment sent|debited)\\b.*")) {
            return payment(amount(incoming), "Income", incoming.group(3), "", app);
        }
        Matcher outgoing = OUTGOING.matcher(text);
        if (outgoing.matches() && !title.matches("(?iu).*\\b(?:money received|payment received|credited)\\b.*")) {
            return payment(amount(outgoing), "Expense", outgoing.group(3), "", app);
        }
        return null;
    }

    private static String amount(Matcher match) {
        return match.group(1) != null ? match.group(1) : match.group(2);
    }

    private static UpiPayment payment(String raw, String kind, String name, String account, String app) {
        double value = Double.parseDouble(raw.replace(",", ""));
        String person = name.trim();
        if (!(value > 0) || value > 1_000_000_000 || person.isEmpty()) return null;
        return new UpiPayment(value, kind, person, account, app);
    }

    static boolean isAppAlert(String key) {
        return key.startsWith(KEY_PREFIX) || key.startsWith("phonepe:");
    }

    static boolean nearby(double amount, long time, double otherAmount, long otherTime) {
        return amount > 0 && otherAmount > 0 && time > 0 && otherTime > 0
                && Math.round(amount * 100) == Math.round(otherAmount * 100)
                && Math.abs(time - otherTime) <= DUPLICATE_WINDOW_MS;
    }
}
