package cm.aptoide.pt.billing.view.payment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.analytics.ScreenTagHistory;
import cm.aptoide.pt.billing.Billing;
import cm.aptoide.pt.billing.payment.PaymentService;
import cm.aptoide.pt.billing.view.BillingActivity;
import cm.aptoide.pt.billing.view.BillingNavigator;
import cm.aptoide.pt.navigator.ActivityResultNavigator;
import cm.aptoide.pt.permission.PermissionServiceFragment;
import cm.aptoide.pt.view.spannable.SpannableFactory;
import com.jakewharton.rxrelay.PublishRelay;
import java.util.ArrayList;
import java.util.List;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;

public class PaymentMethodsFragment extends PermissionServiceFragment
    implements PaymentMethodsView {

  private Billing billing;
  private BillingNavigator navigator;
  private PublishRelay<Void> backButton;
  private ClickHandler handler;

  private Toolbar toolbar;
  private RecyclerView list;
  private PaymentAdapter adapter;
  private TextView noPaymentsMessage;
  private View progressBarContainer;

  public static Fragment create(Bundle bundle) {
    PaymentMethodsFragment fragment = new PaymentMethodsFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    billing = ((AptoideApplication) getContext().getApplicationContext()).getBilling(
        getArguments().getString(BillingActivity.EXTRA_MERCHANT_PACKAGE_NAME));
    navigator = ((ActivityResultNavigator) getActivity()).getBillingNavigator();
    backButton = PublishRelay.create();
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_payment_methods, container, false);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setHasOptionsMenu(true);
    toolbar = (Toolbar) view.findViewById(R.id.fragment_payment_methods_toolbar);
    ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
    ((AppCompatActivity) getActivity()).getSupportActionBar()
        .setDisplayHomeAsUpEnabled(true);

    handler = () -> {
      backButton.call(null);
      return true;
    };
    registerClickHandler(handler);

    progressBarContainer = view.findViewById(R.id.fragment_payment_methods_progress_bar);
    noPaymentsMessage = (TextView) view.findViewById(R.id.fragment_payment_methods_empty_message);
    list = (RecyclerView) view.findViewById(R.id.fragment_payment_methods_list);
    list.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new PaymentAdapter(new ArrayList<>(), PublishSubject.create(),
        LayoutInflater.from(getContext()), new SpannableFactory());
    list.setAdapter(adapter);

    new PaymentMethodsPresenter(this, billing, getArguments().getString(BillingActivity.EXTRA_SKU),
        AndroidSchedulers.mainThread(), navigator,
        getArguments().getString(BillingActivity.EXTRA_MERCHANT_PACKAGE_NAME),
        getArguments().getString(BillingActivity.EXTRA_DEVELOPER_PAYLOAD)).present();
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      backButton.call(null);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public void onDestroyView() {
    unregisterClickHandler(handler);
    progressBarContainer = null;
    noPaymentsMessage = null;
    toolbar = null;
    adapter = null;
    list = null;
    super.onDestroyView();
  }

  @Override public ScreenTagHistory getHistoryTracker() {
    return ScreenTagHistory.Builder.build(this.getClass()
        .getSimpleName());
  }

  @Override public Observable<PaymentService> getSelectedPaymentMethodEvent() {
    return adapter.getSelectedPaymentMethod();
  }

  @Override public Observable<Void> getCancelEvent() {
    return backButton;
  }

  @Override public void showNoPaymentMethodsAvailableMessage() {
    noPaymentsMessage.setVisibility(View.VISIBLE);
    list.setVisibility(View.GONE);
  }

  @Override public void showAvailablePaymentMethods(List<PaymentService> paymentMethods) {
    list.setVisibility(View.VISIBLE);
    noPaymentsMessage.setVisibility(View.GONE);
    adapter.updatePaymentMethods(paymentMethods);
  }

  @Override public void showLoading() {
    progressBarContainer.setVisibility(View.VISIBLE);
  }

  @Override public void hideLoading() {
    progressBarContainer.setVisibility(View.GONE);
  }
}
