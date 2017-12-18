package cm.aptoide.pt.billing.authorization;

import java.util.List;
import rx.Single;

public interface AuthorizationService {

  Single<Authorization> createAuthorization(String customerId, String transactionId,
      String metadata);

  Single<List<Authorization>> getAuthorizations(String customerId);
}
