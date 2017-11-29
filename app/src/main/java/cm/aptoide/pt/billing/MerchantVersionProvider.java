package cm.aptoide.pt.billing;

import rx.Single;

public interface MerchantVersionProvider {

  Single<Integer> getVersionCode(String merchantName);
}
