package cm.aptoide.pt.billing.view.payment;

import cm.aptoide.pt.billing.payment.PaymentService;
import cm.aptoide.pt.presenter.View;
import java.util.List;
import rx.Observable;

public interface PaymentMethodsView extends View {

  void showNoPaymentMethodsAvailableMessage();

  void showAvailablePaymentMethods(List<PaymentService> paymentServices);

  Observable<PaymentService> getSelectedPaymentMethodEvent();

  Observable<Void> getCancelEvent();

  void showLoading();

  void hideLoading();
}
