package cm.aptoide.pt.billing.networking;

import cm.aptoide.pt.billing.BillingIdManager;
import cm.aptoide.pt.billing.authorization.Authorization;
import cm.aptoide.pt.billing.authorization.AuthorizationFactory;
import cm.aptoide.pt.billing.authorization.AuthorizationService;
import java.util.Collections;
import java.util.List;
import rx.Single;

public class AuthorizationServiceV3 implements AuthorizationService {

  private final AuthorizationFactory authorizationFactory;
  private final BillingIdManager billingIdManager;

  public AuthorizationServiceV3(AuthorizationFactory authorizationFactory,
      BillingIdManager billingIdManager) {
    this.authorizationFactory = authorizationFactory;
    this.billingIdManager = billingIdManager;
  }

  @Override
  public Single<Authorization> createAuthorization(String customerId, String paymentMethodId,
      String metadata) {
    return Single.just(getActiveAuthorization(customerId, paymentMethodId));
  }

  @Override public Single<List<Authorization>> getAuthorizations(String customerId) {
    return Single.just(Collections.singletonList(
        getActiveAuthorization(customerId, billingIdManager.generatePaymentMehtodId(1))));
  }

  private Authorization getActiveAuthorization(String customerId, String paymentMethodId) {
    return authorizationFactory.create(billingIdManager.generateAuthorizationId(-1), customerId,
        AuthorizationFactory.PAYPAL_SDK, Authorization.Status.ACTIVE, null, null, null,
        paymentMethodId, null);
  }
}