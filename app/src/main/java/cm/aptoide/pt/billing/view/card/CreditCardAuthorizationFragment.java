package cm.aptoide.pt.billing.view.card;

import adyen.com.adyencse.encrypter.ClientSideEncrypter;
import adyen.com.adyencse.encrypter.exception.EncrypterException;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.analytics.ScreenTagHistory;
import cm.aptoide.pt.billing.Billing;
import cm.aptoide.pt.billing.BillingAnalytics;
import cm.aptoide.pt.billing.payment.Adyen;
import cm.aptoide.pt.billing.view.BillingActivity;
import cm.aptoide.pt.billing.view.BillingNavigator;
import cm.aptoide.pt.navigator.ActivityResultNavigator;
import cm.aptoide.pt.permission.PermissionServiceFragment;
import cm.aptoide.pt.view.rx.RxAlertDialog;
import com.adyen.core.models.PaymentMethod;
import com.adyen.core.models.paymentdetails.CreditCardPaymentDetails;
import com.adyen.core.models.paymentdetails.PaymentDetails;
import com.braintreepayments.cardform.view.CardForm;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxrelay.PublishRelay;
import org.json.JSONException;
import org.json.JSONObject;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class CreditCardAuthorizationFragment extends PermissionServiceFragment
    implements CreditCardAuthorizationView {

  private static final String TAG = CreditCardAuthorizationFragment.class.getSimpleName();

  private View progressBar;
  private RxAlertDialog networkErrorDialog;
  private ClickHandler clickHandler;
  private CardForm cardForm;
  private Button nextButton;
  private Toolbar toolbar;

  private Billing billing;
  private BillingNavigator navigator;
  private BillingAnalytics analytics;
  private Adyen adyen;
  private PublishRelay<Void> backButton;
  private PublishRelay<Void> keyboardBuyRelay;
  private String publicKey;
  private String generationTime;
  private PaymentMethod paymentMethod;

  public static CreditCardAuthorizationFragment create(Bundle bundle) {
    final CreditCardAuthorizationFragment fragment = new CreditCardAuthorizationFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    billing = ((AptoideApplication) getContext().getApplicationContext()).getBilling(
        getArguments().getString(BillingActivity.EXTRA_MERCHANT_PACKAGE_NAME));
    navigator = ((ActivityResultNavigator) getActivity()).getBillingNavigator();
    analytics = ((AptoideApplication) getContext().getApplicationContext()).getBillingAnalytics();
    backButton = PublishRelay.create();
    keyboardBuyRelay = PublishRelay.create();
    adyen = ((AptoideApplication) getContext().getApplicationContext()).getAdyen();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setHasOptionsMenu(true);
    toolbar = (Toolbar) view.findViewById(R.id.fragment_credit_card_authorization_toolbar);
    ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
    ((AppCompatActivity) getActivity()).getSupportActionBar()
        .setDisplayHomeAsUpEnabled(true);

    progressBar = view.findViewById(R.id.fragment_credit_card_authorization_progress_bar);
    nextButton = (Button) view.findViewById(R.id.fragment_credit_card_authorization_next_button);
    cardForm = (CardForm) view.findViewById(R.id.fragment_credit_card_authorization_form);

    networkErrorDialog =
        new RxAlertDialog.Builder(getContext()).setMessage(R.string.connection_error)
            .setPositiveButton(R.string.ok)
            .build();

    clickHandler = new ClickHandler() {
      @Override public boolean handle() {
        backButton.call(null);
        return true;
      }
    };
    registerClickHandler(clickHandler);

    cardForm.setOnCardFormValidListener(valid -> {
      if (valid) {
        nextButton.setEnabled(true);
      } else {
        nextButton.setEnabled(false);
      }
    });
    cardForm.setOnCardFormSubmitListener(() -> {
      keyboardBuyRelay.call(null);
    });

    attachPresenter(new CreditCardAuthorizationPresenter(this,
        getArguments().getString(BillingActivity.EXTRA_SKU), billing, navigator, analytics,
        getArguments().getString(BillingActivity.EXTRA_SERVICE_NAME), adyen,
        AndroidSchedulers.mainThread()));
  }

  @Override public ScreenTagHistory getHistoryTracker() {
    return ScreenTagHistory.Builder.build(this.getClass()
        .getSimpleName());
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_credit_card_authorization, container, false);
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      backButton.call(null);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public void onDestroyView() {
    unregisterClickHandler(clickHandler);
    toolbar = null;
    progressBar = null;
    networkErrorDialog.dismiss();
    networkErrorDialog = null;
    nextButton = null;
    cardForm.setOnCardFormSubmitListener(null);
    cardForm.setOnCardFormValidListener(null);
    cardForm = null;
    super.onDestroyView();
  }

  @Override public void showLoading() {
    progressBar.setVisibility(View.VISIBLE);
  }

  @Override public void hideLoading() {
    progressBar.setVisibility(View.GONE);
  }

  @Override public Observable<Void> errorDismisses() {
    return networkErrorDialog.dismisses()
        .map(dialogInterface -> null);
  }

  @Override public Observable<PaymentDetails> creditCardDetailsEvent() {
    return Observable.merge(keyboardBuyRelay, RxView.clicks(nextButton))
        .map(__ -> getPaymentDetails(publicKey, generationTime));
  }

  @Override public void showNetworkError() {
    if (!networkErrorDialog.isShowing()) {
      networkErrorDialog.show();
    }
  }

  @Override public Observable<Void> cancelEvent() {
    return backButton;
  }

  @Override
  public void showCreditCardView(PaymentMethod paymentMethod, boolean cvcRequired, String publicKey,
      String generationTime) {
    this.paymentMethod = paymentMethod;
    this.publicKey = publicKey;
    this.generationTime = generationTime;
    cardForm.cardRequired(true)
        .expirationRequired(true)
        .cvvRequired(cvcRequired)
        .postalCodeRequired(false)
        .mobileNumberRequired(false)
        .actionLabel(getString(R.string.fragment_credit_card_authorization_next_button))
        .setup(getActivity());
  }

  private PaymentDetails getPaymentDetails(String publicKey, String generationTime) {

    final CreditCardPaymentDetails creditCardPaymentDetails =
        new CreditCardPaymentDetails(paymentMethod.getInputDetails());
    try {
      final JSONObject sensitiveData = new JSONObject();

      sensitiveData.put("holderName", "Checkout Shopper Placeholder");
      sensitiveData.put("number", cardForm.getCardNumber());
      sensitiveData.put("expiryMonth", cardForm.getExpirationMonth());
      sensitiveData.put("expiryYear", cardForm.getExpirationYear());
      sensitiveData.put("generationtime", generationTime);
      sensitiveData.put("cvc", cardForm.getCvv());
      creditCardPaymentDetails.fillCardToken(
          new ClientSideEncrypter(publicKey).encrypt(sensitiveData.toString()));
    } catch (JSONException e) {
      Log.e(TAG, "JSON Exception occurred while generating token.", e);
    } catch (EncrypterException e) {
      Log.e(TAG, "EncrypterException occurred while generating token.", e);
    }
    creditCardPaymentDetails.fillStoreDetails(true);
    return creditCardPaymentDetails;
  }
}
