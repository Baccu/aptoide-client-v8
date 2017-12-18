package cm.aptoide.pt.billing;

import cm.aptoide.pt.billing.customer.Customer;
import cm.aptoide.pt.billing.payment.Payment;
import cm.aptoide.pt.billing.product.Product;
import cm.aptoide.pt.billing.purchase.Purchase;
import java.util.List;
import rx.Completable;
import rx.Single;

public interface BillingService {

  Single<Payment> getPayment(String merchantPackageName, int merchantVersionCode, String sku,
      Customer customer, String selectedPaymentMethodId);

  Single<Merchant> getMerchant(String packageName, int versionCode);

  Completable deletePurchase(String purchaseId);

  Single<List<Purchase>> getPurchases(String merchantName);

  Single<List<Product>> getProducts(String merchantName, List<String> skus);

}