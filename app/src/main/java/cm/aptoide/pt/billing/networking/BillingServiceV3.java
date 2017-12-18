/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 11/08/2016.
 */

package cm.aptoide.pt.billing.networking;

import android.content.SharedPreferences;
import android.content.res.Resources;
import cm.aptoide.pt.billing.BillingIdManager;
import cm.aptoide.pt.billing.BillingService;
import cm.aptoide.pt.billing.Merchant;
import cm.aptoide.pt.billing.authorization.AuthorizationService;
import cm.aptoide.pt.billing.customer.Customer;
import cm.aptoide.pt.billing.payment.PayPalPaymentService;
import cm.aptoide.pt.billing.payment.Payment;
import cm.aptoide.pt.billing.payment.PaymentMethod;
import cm.aptoide.pt.billing.product.Product;
import cm.aptoide.pt.billing.purchase.Purchase;
import cm.aptoide.pt.billing.transaction.TransactionService;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.model.v3.PaidApp;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v3.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v3.GetApkInfoRequest;
import java.util.Collections;
import java.util.List;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import rx.Completable;
import rx.Single;

public class BillingServiceV3 implements BillingService {

  private final BodyInterceptor<BaseBody> bodyInterceptorV3;
  private final OkHttpClient httpClient;
  private final Converter.Factory converterFactory;
  private final TokenInvalidator tokenInvalidator;
  private final SharedPreferences sharedPreferences;
  private final PurchaseMapperV3 purchaseMapper;
  private final ProductMapperV3 productMapper;
  private final Resources resources;
  private final BillingIdManager billingIdManager;
  private final int currentAPILevel;
  private final int serviceMinimumAPILevel;
  private final String marketName;
  private final AuthorizationService authorizationServiceV3;
  private final TransactionService transactionServiceV3;

  public BillingServiceV3(BodyInterceptor<BaseBody> bodyInterceptorV3, OkHttpClient httpClient,
      Converter.Factory converterFactory, TokenInvalidator tokenInvalidator,
      SharedPreferences sharedPreferences, PurchaseMapperV3 purchaseMapper,
      ProductMapperV3 productMapper, Resources resources, BillingIdManager billingIdManager,
      int currentAPILevel, int serviceMinimumAPILevel, String marketName,
      AuthorizationService authorizationServiceV3, TransactionService transactionServiceV3) {
    this.bodyInterceptorV3 = bodyInterceptorV3;
    this.httpClient = httpClient;
    this.converterFactory = converterFactory;
    this.tokenInvalidator = tokenInvalidator;
    this.sharedPreferences = sharedPreferences;
    this.purchaseMapper = purchaseMapper;
    this.productMapper = productMapper;
    this.resources = resources;
    this.billingIdManager = billingIdManager;
    this.currentAPILevel = currentAPILevel;
    this.serviceMinimumAPILevel = serviceMinimumAPILevel;
    this.marketName = marketName;
    this.authorizationServiceV3 = authorizationServiceV3;
    this.transactionServiceV3 = transactionServiceV3;
  }

  @Override
  public Single<Payment> getPayment(String merchantPackageName, int merchantVersionCode, String sku,
      Customer customer, String selectedPaymentMethodId) {
    return Single.zip(getMerchant(merchantPackageName, merchantVersionCode),
        getPaymentMethods(customer.getId()), getProduct(sku, merchantPackageName),
        (merchant, paymentMethods, product) -> Single.zip(
            transactionServiceV3.getTransaction(customer.getId(), product.getId()),
            getPurchase(product.getId()),
            (transaction, purchase) -> new Payment(merchant, customer, product, transaction,
                purchase, paymentMethods, selectedPaymentMethodId)))
        .flatMap(single -> single);
  }

  @Override public Single<Merchant> getMerchant(String packageName, int versionCode) {
    return Single.just(new Merchant(-1, marketName, packageName, versionCode));
  }

  @Override public Completable deletePurchase(String purchaseId) {
    return Completable.error(new IllegalStateException("Not implemented!"));
  }

  @Override public Single<List<Purchase>> getPurchases(String merchantName) {
    return Single.error(new IllegalStateException("Not implemented!"));
  }

  @Override public Single<List<Product>> getProducts(String merchantName, List<String> productIds) {
    return Single.error(new IllegalStateException("Not implemented!"));
  }

  private Single<Purchase> getPurchase(String productId) {
    return getServerPaidApp(true, billingIdManager.resolveProductId(productId)).map(
        app -> purchaseMapper.map(app, productId));
  }

  private Single<List<PaymentMethod>> getPaymentMethods(String customerId) {
    if (currentAPILevel >= serviceMinimumAPILevel) {
      return authorizationServiceV3.getAuthorizations(customerId)
          .map(authorizations -> Collections.singletonList(
              new PaymentMethod(billingIdManager.generatePaymentMehtodId(1),
                  PayPalPaymentService.TYPE, "PayPal", null, "", true, authorizations, null)));
    }
    return Single.just(Collections.emptyList());
  }

  private Single<Product> getProduct(String sku, String merchantName) {
    return getServerPaidApp(false, billingIdManager.resolveProductId(sku)).map(
        paidApp -> productMapper.map(paidApp));
  }

  private Single<PaidApp> getServerPaidApp(boolean bypassCache, long appId) {
    return GetApkInfoRequest.of(appId, bodyInterceptorV3, httpClient, converterFactory,
        tokenInvalidator, sharedPreferences, resources)
        .observe(bypassCache)
        .first()
        .toSingle();
  }
}
