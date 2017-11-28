/*
 * Copyright (c) 2017.
 * Modified by Marcelo Benites on 06/01/2017.
 */

package cm.aptoide.pt.billing;

import cm.aptoide.pt.billing.customer.Customer;
import rx.Observable;
import rx.Single;

public interface CustomerPersistence {

  Observable<Customer> getCustomer();

}
