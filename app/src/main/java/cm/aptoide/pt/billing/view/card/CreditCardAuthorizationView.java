package cm.aptoide.pt.billing.view.card;

import cm.aptoide.pt.presenter.View;
import com.adyen.core.models.PaymentMethod;
import com.adyen.core.models.paymentdetails.PaymentDetails;
import rx.Observable;

public interface CreditCardAuthorizationView extends View {

  void showLoading();

  void hideLoading();

  Observable<Void> errorDismisses();

  Observable<PaymentDetails> creditCardDetailsEvent();

  void showNetworkError();

  Observable<Void> cancelEvent();

  void showCreditCardView(PaymentMethod paymentMethod, boolean cvcStatus, String publicKey,
      String generationTime);
}
