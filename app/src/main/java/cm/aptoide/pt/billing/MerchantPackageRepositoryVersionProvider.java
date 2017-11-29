package cm.aptoide.pt.billing;

import cm.aptoide.pt.install.PackageRepository;
import rx.Single;

public class MerchantPackageRepositoryVersionProvider implements MerchantVersionProvider {

  private final PackageRepository packageRepository;

  public MerchantPackageRepositoryVersionProvider(PackageRepository packageRepository) {
    this.packageRepository = packageRepository;
  }

  @Override public Single<Integer> getVersionCode(String merchantName) {
    return packageRepository.getPackageVersionCode(merchantName);
  }
}
