/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 11/08/2016.
 */

package cm.aptoide.pt.billing.networking;

import cm.aptoide.pt.billing.BillingIdManager;
import cm.aptoide.pt.billing.payment.AdyenPaymentService;
import cm.aptoide.pt.billing.payment.PayPalPaymentService;
import cm.aptoide.pt.billing.payment.PaymentMethod;
import cm.aptoide.pt.crashreports.CrashLogger;
import cm.aptoide.pt.dataprovider.ws.v7.billing.GetServicesRequest;
import java.util.ArrayList;
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

  public List<PaymentMethod> map(List<GetServicesRequest.ResponseBody.Service> responseList) {

    final List<PaymentMethod> paymentMethods = new ArrayList<>(responseList.size());
    for (GetServicesRequest.ResponseBody.Service service : responseList) {
      try {
        paymentMethods.add(map(service));
      } catch (IllegalArgumentException exception) {
        crashLogger.log(exception);
      }
    }
    return paymentMethods;
  }

  private PaymentMethod map(GetServicesRequest.ResponseBody.Service response) {
    switch (response.getName()) {
      case PayPalPaymentService.TYPE:
        if (currentAPILevel >= minimumAPILevelPayPal) {
          return new PaymentMethod(billingIdManager.generateServiceId(response.getId()),
              response.getName(), response.getLabel(), response.getDescription(),
              response.getIcon(), true);
        }
        throw new IllegalArgumentException(
            "PayPal not supported in Android API lower than " + minimumAPILevelPayPal);
      case AdyenPaymentService.TYPE:
        if (currentAPILevel >= minimumAPILevelAdyen) {
          return new PaymentMethod(billingIdManager.generateServiceId(response.getId()),
              response.getName(), response.getLabel(), response.getDescription(),
              response.getIcon(), false);
        }
        throw new IllegalArgumentException(
            "Adyen not supported in Android API lower than " + minimumAPILevelAdyen);
      default:
        throw new IllegalArgumentException("Payment service not supported: " + response.getName());
    }
  }
}