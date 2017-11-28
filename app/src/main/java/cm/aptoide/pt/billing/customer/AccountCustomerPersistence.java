package cm.aptoide.pt.billing.customer;

import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.billing.CustomerPersistence;
import rx.Observable;

public class AccountCustomerPersistence implements CustomerPersistence {

  private final AptoideAccountManager accountManager;

  public AccountCustomerPersistence(AptoideAccountManager accountManager) {
    this.accountManager = accountManager;
  }

  @Override public Observable<Customer> getCustomer() {
    return accountManager.accountStatus()
        .map(account -> new Customer(account.getId(), account.isLoggedIn()));
  }
}
