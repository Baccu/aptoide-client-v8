/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 19/08/2016.
 */

package cm.aptoide.pt.billing.view.payment;

import cm.aptoide.pt.billing.payment.PaymentMethod;
import cm.aptoide.pt.billing.product.Product;
import cm.aptoide.pt.presenter.View;
import java.util.List;
import rx.Observable;

public interface PaymentView extends View {

  Observable<String> cancelEvent();

  Observable<String> buyEvent();

  void showPaymentLoading();

  void showBuyLoading();

  void showPayments(List<PaymentMethod> paymentList);

  void showProduct(Product product);

  void showMerchant(String merchantName);

  void hidePaymentLoading();

  void hideBuyLoading();

  void showNetworkError();

  void showUnknownError();
}
