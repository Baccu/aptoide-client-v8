package cm.aptoide.pt.billing.view.payment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import cm.aptoide.pt.AptoideApplication;
import cm.aptoide.pt.R;
import cm.aptoide.pt.analytics.ScreenTagHistory;
import cm.aptoide.pt.billing.Billing;
import cm.aptoide.pt.billing.BillingAnalytics;
import cm.aptoide.pt.billing.BillingIdManager;
import cm.aptoide.pt.billing.payment.PaymentService;
import cm.aptoide.pt.billing.product.Product;
import cm.aptoide.pt.billing.view.BillingActivity;
import cm.aptoide.pt.billing.view.BillingNavigator;
import cm.aptoide.pt.navigator.ActivityResultNavigator;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.permission.PermissionServiceFragment;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.view.rx.RxAlertDialog;
import cm.aptoide.pt.view.spannable.SpannableFactory;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxrelay.PublishRelay;
import java.util.List;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class PaymentFragment extends PermissionServiceFragment implements PaymentView {

  private static final String CHECKED_SERVICE_ID = "CHECKED_SERVICE_ID";
  private View progressView;
  private RadioGroup serviceRadioGroup;
  private ImageView productIcon;
  private TextView productName;
  private TextView merchantNameText;
  private Button buyButton;
  private TextView productPrice;
  private ClickHandler handler;
  private PublishRelay<Void> cancelRelay;

  private RxAlertDialog networkErrorDialog;
  private RxAlertDialog unknownErrorDialog;
  private SpannableFactory spannableFactory;
  private Toolbar toolbar;

  private boolean paymentLoading;
  private boolean buyLoading;

  private Billing billing;
  private BillingAnalytics billingAnalytics;
  private BillingNavigator billingNavigator;
  private BillingIdManager billingIdManager;
  private int checkedServiceId;

  public static Fragment create(Bundle bundle) {
    final PaymentFragment fragment = new PaymentFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    billing = ((AptoideApplication) getContext().getApplicationContext()).getBilling(
        getArguments().getString(BillingActivity.EXTRA_MERCHANT_PACKAGE_NAME));
    billingAnalytics =
        ((AptoideApplication) getContext().getApplicationContext()).getBillingAnalytics();
    billingNavigator = ((ActivityResultNavigator) getContext()).getBillingNavigator();
    billingIdManager = ((AptoideApplication) getContext().getApplicationContext()).getIdResolver(
        getArguments().getString(BillingActivity.EXTRA_MERCHANT_PACKAGE_NAME));
    cancelRelay = PublishRelay.create();
  }

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_payment, container, false);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setHasOptionsMenu(true);
    toolbar = (Toolbar) view.findViewById(R.id.fragment_payment_toolbar);
    ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
    ((AppCompatActivity) getActivity()).getSupportActionBar()
        .setDisplayHomeAsUpEnabled(true);

    productIcon = (ImageView) view.findViewById(R.id.include_payment_product_icon);

    spannableFactory = new SpannableFactory();
    progressView = view.findViewById(R.id.fragment_payment_global_progress_bar);

    productIcon = (ImageView) view.findViewById(R.id.include_payment_product_icon);
    productName = (TextView) view.findViewById(R.id.fragment_payment_product_name);
    merchantNameText = (TextView) view.findViewById(R.id.payment_fragment_merchant_name);

    productPrice = (TextView) view.findViewById(R.id.include_payment_product_price);
    serviceRadioGroup = (RadioGroup) view.findViewById(R.id.fragment_payment_authorization_list);

    buyButton = (Button) view.findViewById(R.id.fragment_payment_buy_button);

    networkErrorDialog =
        new RxAlertDialog.Builder(getContext()).setMessage(R.string.connection_error)
            .setPositiveButton(android.R.string.ok)
            .build();
    unknownErrorDialog =
        new RxAlertDialog.Builder(getContext()).setMessage(R.string.all_message_general_error)
            .setPositiveButton(android.R.string.ok)
            .build();

    handler = () -> {
      cancelRelay.call(null);
      return true;
    };
    registerClickHandler(handler);

    if (savedInstanceState != null) {
      checkedServiceId = savedInstanceState.getInt(CHECKED_SERVICE_ID);
    } else {
      checkedServiceId = -1;
    }

    attachPresenter(new PaymentPresenter(this, billing, billingNavigator, billingAnalytics,
        getArguments().getString(BillingActivity.EXTRA_MERCHANT_PACKAGE_NAME),
        getArguments().getString(BillingActivity.EXTRA_SKU),
        getArguments().getString(BillingActivity.EXTRA_DEVELOPER_PAYLOAD),
        AndroidSchedulers.mainThread()));
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      cancelRelay.call(null);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    if (serviceRadioGroup != null) {
      outState.putInt(CHECKED_SERVICE_ID, serviceRadioGroup.getCheckedRadioButtonId());
    }
    super.onSaveInstanceState(outState);
  }

  @Override public void onDestroyView() {
    unregisterClickHandler(handler);
    spannableFactory = null;
    progressView = null;
    productIcon = null;
    productName = null;
    merchantNameText = null;
    productPrice = null;
    serviceRadioGroup = null;
    buyButton = null;
    networkErrorDialog.dismiss();
    networkErrorDialog = null;
    unknownErrorDialog.dismiss();
    unknownErrorDialog = null;
    paymentLoading = false;
    buyLoading = false;
    super.onDestroyView();
  }

  @Override public ScreenTagHistory getHistoryTracker() {
    return ScreenTagHistory.Builder.build(this.getClass()
        .getSimpleName());
  }

  @Override public Observable<String> cancelEvent() {
    return cancelRelay.map(
        __ -> billingIdManager.generateServiceId(serviceRadioGroup.getCheckedRadioButtonId()));
  }

  @Override public Observable<String> buyEvent() {
    return RxView.clicks(buyButton)
        .map(__ -> billingIdManager.generateServiceId(serviceRadioGroup.getCheckedRadioButtonId()));
  }

  @Override public void showPaymentLoading() {
    paymentLoading = true;
    progressView.setVisibility(View.VISIBLE);
  }

  @Override public void showBuyLoading() {
    buyLoading = true;
    progressView.setVisibility(View.VISIBLE);
  }

  @Override public void showPayments(List<PaymentService> services) {
    serviceRadioGroup.removeAllViews();

    RadioButton radioButton;
    CharSequence radioText;
    for (PaymentService service : services) {

      radioButton = (RadioButton) getActivity().getLayoutInflater()
          .inflate(R.layout.payment_item, serviceRadioGroup, false);
      radioButton.setId((int) billingIdManager.resolveServiceId(service.getId()));

      Glide.with(this)
          .load(service.getIcon())
          .into(new RadioButtonTarget(AptoideUtils.ScreenU.getPixelsForDip(16, getResources()),
              radioButton));

      if (TextUtils.isEmpty(service.getDescription())) {
        radioText = service.getName();
      } else {
        radioText = spannableFactory.createTextAppearanceSpan(getContext(),
            R.style.TextAppearance_Aptoide_Small,
            service.getName() + "\n" + service.getDescription(), service.getDescription());
      }

      radioButton.setText(radioText);

      radioButton.setChecked(service.isDefaultService());

      serviceRadioGroup.addView(radioButton);
    }

    if (checkedServiceId != -1) {
      serviceRadioGroup.check(checkedServiceId);
    }
  }

  @Override public void showProduct(Product product) {
    ImageLoader.with(getContext())
        .load(product.getIcon(), productIcon);
    productName.setText(product.getTitle());
    productPrice.setText(product.getPrice()
        .getCurrencySymbol() + " " + product.getPrice()
        .getAmount());
  }

  @Override public void showMerchant(String merchantName) {
    merchantNameText.setText(merchantName);
  }

  @Override public void hidePaymentLoading() {
    paymentLoading = false;
    if (!buyLoading) {
      progressView.setVisibility(View.GONE);
    }
  }

  @Override public void hideBuyLoading() {
    buyLoading = false;
    if (!paymentLoading) {
      progressView.setVisibility(View.GONE);
    }
  }

  @Override public void showNetworkError() {
    if (!networkErrorDialog.isShowing() && !unknownErrorDialog.isShowing()) {
      networkErrorDialog.show();
    }
  }

  @Override public void showUnknownError() {
    if (!networkErrorDialog.isShowing() && !unknownErrorDialog.isShowing()) {
      unknownErrorDialog.show();
    }
  }

  private static class RadioButtonTarget extends SimpleTarget<GlideDrawable> {

    private RadioButton radioButton;

    public RadioButtonTarget(int pixels, RadioButton radioButton) {
      super(pixels, pixels);
      this.radioButton = radioButton;
    }

    @Override public void onResourceReady(GlideDrawable glideDrawable,
        GlideAnimation<? super GlideDrawable> glideAnimation) {
      radioButton.setCompoundDrawablesWithIntrinsicBounds(null, null, glideDrawable.getCurrent(),
          null);
    }

    @Override public void onDestroy() {
      radioButton = null;
      super.onDestroy();
    }
  }
}