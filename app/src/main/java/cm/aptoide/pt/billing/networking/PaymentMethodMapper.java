/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 11/08/2016.
 */

package cm.aptoide.pt.billing.networking;

import cm.aptoide.pt.billing.BillingIdManager;
import cm.aptoide.pt.billing.authorization.Authorization;
import cm.aptoide.pt.billing.payment.AdyenPaymentService;
import cm.aptoide.pt.billing.payment.PayPalPaymentService;
import cm.aptoide.pt.crashreports.CrashLogger;
import cm.aptoide.pt.dataprovider.ws.v7.billing.GetPaymentMethodsRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaymentMethodMapper {

  private final CrashLogger crashLogger;
  private final BillingIdManager billingIdManager;
  private final int currentAPILevel;
  private final int minimumAPILevelAdyen;
  private final int minimumAPILevelPayPal;

  public PaymentMethodMapper(CrashLogger crashLogger, BillingIdManager billingIdManager,
      int currentAPILevel, int minimumAPILevelAdyen, int minimumAPILevelPayPal) {
    this.crashLogger = crashLogger;
    this.billingIdManager = billingIdManager;
    this.currentAPILevel = currentAPILevel;
    this.minimumAPILevelAdyen = minimumAPILevelAdyen;
    this.minimumAPILevelPayPal = minimumAPILevelPayPal;
  }

  public List<cm.aptoide.pt.billing.payment.PaymentMethod> map(
      List<GetPaymentMethodsRequest.ResponseBody.PaymentMethod> responseList,
      List<Authorization> authorizations) {

    final List<cm.aptoide.pt.billing.payment.PaymentMethod> paymentMethods = new ArrayList<>(responseList.size());
    final List<Authorization> paymentMethodAuthorizations = new ArrayList<>();
    for (GetPaymentMethodsRequest.ResponseBody.PaymentMethod paymentMethod : responseList) {

      paymentMethodAuthorizations.clear();

      for (Authorization authorization : authorizations) {
        if (authorization.getPaymentMethodId()
            .equals(paymentMethod.getId())) {
          paymentMethodAuthorizations.add(authorization);
        }
      }

      try {
        paymentMethods.add(map(paymentMethod, paymentMethodAuthorizations));
      } catch (IllegalArgumentException exception) {
        crashLogger.log(exception);
      }
    }
    return paymentMethods;
  }

  private cm.aptoide.pt.billing.payment.PaymentMethod map(
      GetPaymentMethodsRequest.ResponseBody.PaymentMethod response,
      List<Authorization> authorizations) {
    switch (response.getName()) {
      case PayPalPaymentService.TYPE:
        if (currentAPILevel >= minimumAPILevelPayPal) {
          return new cm.aptoide.pt.billing.payment.PaymentMethod(billingIdManager.generatePaymentMehtodId(response.getId()),
              response.getName(), response.getLabel(), response.getDescription(),
              response.getIcon(), true, authorizations, null);
        }
        throw new IllegalArgumentException(
            "PayPal not supported in Android API lower than " + minimumAPILevelPayPal);
      case AdyenPaymentService.TYPE:
        if (currentAPILevel >= minimumAPILevelAdyen) {
          return new cm.aptoide.pt.billing.payment.PaymentMethod(billingIdManager.generatePaymentMehtodId(response.getId()),
              response.getName(), response.getLabel(), response.getDescription(),
              response.getIcon(), false, authorizations, null);
        }
        throw new IllegalArgumentException(
            "Adyen not supported in Android API lower than " + minimumAPILevelAdyen);
      default:
        throw new IllegalArgumentException("Payment service not supported: " + response.getName());
    }
  }
}