package com.quickspend.app;

public final class UpiPaymentTest {
    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        String body = "Example Sender has sent ₹1 to your bank\naccount Central Bank Of India-1234";
        UpiPayment phonepe = UpiPayment.parse("com.phonepe.app", "Money received", body);
        check(phonepe != null && phonepe.amount == 1 && phonepe.kind.equals("Income")
                && phonepe.account.equals("Central Bank Of India 1234"), "Keep confirmed PhonePe format and bank");
        UpiPayment gpay = UpiPayment.parse("com.google.android.apps.nbu.paisa.user", "Payment successful", "You paid ₹250.50 to Example Shop");
        check(gpay != null && gpay.amount == 250.50 && gpay.kind.equals("Expense") && gpay.app.equals("Google Pay"), "Parse an explicit completed outgoing format");
        UpiPayment paytm = UpiPayment.parse("net.one97.paytm", "Payment received", "₹1,23,456.78 received from Example Sender");
        check(paytm != null && paytm.amount == 123456.78 && paytm.kind.equals("Income"), "Parse Indian number grouping");
        check(UpiPayment.parse("in.org.npci.upiapp", "Money received", "You received Rs. 20 from Example Sender") != null, "Accept BHIM package and explicit receipt format");
        check(UpiPayment.parse("com.example.lookalike", "Money received", body) == null, "Reject unknown packages");
        check(UpiPayment.parse("com.phonepe.app", "Payment pending", body) == null, "Reject pending alerts");
        check(UpiPayment.parse("com.phonepe.app", "Money received", body + " Payment failed") == null, "Reject failure appended to body");
        check(UpiPayment.parse("com.phonepe.app", "Money received", body.replace("₹1", "₹0")) == null, "Reject zero");
        check(UpiPayment.parse("com.phonepe.app", "Money received", body.replace("₹1", "₹1,2")) == null, "Reject malformed amount");
        check(UpiPayment.parse("com.phonepe.app", "Money received", "You paid ₹1 to Example") == null, "Reject conflicting direction");
        check(UpiPayment.parse("com.whatsapp", "Example Person", "You paid ₹1 to Example") == null, "Reject chat text");
        check(UpiPayment.parse("in.amazon.mShop.android.shopping", "Order confirmed", "You paid ₹1 to Example") == null, "Reject a multipurpose app without UPI title");
        check(UpiPayment.parse("net.one97.paytm", "Collect request", "You paid ₹1 to Example") == null, "Reject money requests");
        check(UpiPayment.nearby(1, 5000, 1, 10000), "Recognize nearby same amount");
        check(!UpiPayment.nearby(1, 5000, 1.01, 10000), "Keep different amounts separate");
        check(!UpiPayment.nearby(1, 5000, 1, 140000), "Keep separate payment times distinct");
        System.out.println("PASS: captured PhonePe format, generic UPI payments, unsafe notification rejection and duplicate window");
    }
}
