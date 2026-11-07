package com.quickspend.app;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The confirmed incoming alert format shown by the PhonePe Android app. */
public final class PhonePePayment {
    static final String PACKAGE = "com.phonepe.app";
    static final String KEY_PREFIX = "phonepe:";
    static final long DUPLICATE_WINDOW_MS = 120_000L;
    private static final String NAME = "[\\p{L}\\p{M}\\p{N} .&'’()_-]";
    private static final String AMOUNT = "[0-9]+(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?";
    private static final Pattern RECEIVED = Pattern.compile(
            "^(" + NAME + "{1,80}?) has sent ₹\\s*(" + AMOUNT + ") to your bank account (" +
            NAME + "{1,80}?)[-–]([0-9]{4})\\.?$", Pattern.CASE_INSENSITIVE);

    public final double amount;
    public final String sender;
    public final String account;

    private PhonePePayment(double amount, String sender, String account) {
        this.amount = amount;
        this.sender = sender;
        this.account = account;
    }

    public static PhonePePayment parse(String packageName, String title, String body) {
        if (!PACKAGE.equals(packageName) || title == null || !"Money received".equalsIgnoreCase(title.trim())
                || body == null || body.length() > 512) return null;
        Matcher match = RECEIVED.matcher(body.replaceAll("[\\s\\u00a0]+", " ").trim());
        if (!match.matches()) return null;
        String digits = match.group(2);
        if (digits.contains(",") && !digits.matches("[0-9]{1,3}(?:,[0-9]{2})*,[0-9]{3}(?:\\.[0-9]{1,2})?")
                && !digits.matches("[0-9]{1,3}(?:,[0-9]{3})+(?:\\.[0-9]{1,2})?")) return null;
        double amount = Double.parseDouble(digits.replace(",", ""));
        if (!(amount > 0) || amount > 1_000_000_000) return null;
        String sender = match.group(1).trim(), bank = match.group(3).trim();
        if (sender.isEmpty() || bank.isEmpty()) return null;
        return new PhonePePayment(amount, sender, bank + " " + match.group(4));
    }

    static boolean nearbyIncome(double a, long time, double otherAmount, long otherTime) {
        return a > 0 && otherAmount > 0 && time > 0 && otherTime > 0
                && Math.round(a * 100) == Math.round(otherAmount * 100)
                && Math.abs(time - otherTime) <= DUPLICATE_WINDOW_MS;
    }
}
