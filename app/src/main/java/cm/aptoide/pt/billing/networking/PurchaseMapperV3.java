/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 25/08/2016.
 */

package cm.aptoide.pt.billing.networking;

import cm.aptoide.pt.billing.purchase.Purchase;
import cm.aptoide.pt.billing.purchase.PurchaseFactory;
import cm.aptoide.pt.dataprovider.model.v3.PaidApp;

public class PurchaseMapperV3 {

  private final PurchaseFactory purchaseFactory;

  public PurchaseMapperV3(PurchaseFactory purchaseFactory) {
    this.purchaseFactory = purchaseFactory;
  }

  public Purchase map(PaidApp response, String productId) {

    if (response.isOk() && response.isPaid() && response.getPayment().isPaid()) {
      return purchaseFactory.create(productId, null, response.getPath()
          .getStringPath(), Purchase.Status.COMPLETED, null);
    } else {
      return purchaseFactory.create(productId, null, null, Purchase.Status.FAILED, null);
    }
  }
}
