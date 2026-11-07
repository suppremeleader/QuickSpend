package com.quickspend.app;

public final class PhonePePaymentTest {
    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        String body = "Example Sender has sent ₹1 to your bank\naccount Central Bank Of India-1234";
        PhonePePayment payment = PhonePePayment.parse("com.phonepe.app", "Money received", body);
        check(payment != null && payment.amount == 1 && payment.sender.equals("Example Sender")
                && payment.account.equals("Central Bank Of India 1234"), "Parse the confirmed incoming alert and masked bank");
        check(PhonePePayment.parse("com.example.lookalike", "Money received", body) == null, "Reject other packages");
        check(PhonePePayment.parse("com.phonepe.app", "Payment pending", body) == null, "Reject pending title");
        check(PhonePePayment.parse("com.phonepe.app", "Money received", body + " Payment failed") == null, "Reject failure appended to the body");
        check(PhonePePayment.parse("com.phonepe.app", "Money received", body.replace("₹1", "₹0")) == null, "Reject zero");
        check(PhonePePayment.parse("com.phonepe.app", "Money received", body.replace("₹1", "₹1,2")) == null, "Reject malformed amounts");
        PhonePePayment decimal = PhonePePayment.parse("com.phonepe.app", "Money received", body.replace("₹1", "₹1,23,456.78"));
        check(decimal != null && decimal.amount == 123456.78, "Recognize Indian amount grouping");
        check(PhonePePayment.nearbyIncome(1, 5000, 1, 10000), "Recognize nearby same amount");
        check(!PhonePePayment.nearbyIncome(1, 5000, 1.01, 10000), "Keep different amounts separate");
        check(!PhonePePayment.nearbyIncome(1, 5000, 1, 140000), "Keep separate payment times distinct");
        System.out.println("PASS: confirmed PhonePe format, package filtering, invalid alerts, currency, and duplicate window");
    }
}
