/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 26/08/2016.
 */

package cm.aptoide.pt.billing.view;

import android.app.Activity;
import android.os.Bundle;
import cm.aptoide.pt.billing.external.ExternalBillingBinder;
import cm.aptoide.pt.billing.purchase.Purchase;
import cm.aptoide.pt.billing.purchase.PurchaseFactory;
import java.util.ArrayList;
import java.util.List;

import static cm.aptoide.pt.billing.external.ExternalBillingBinder.RESPONSE_CODE;
import static cm.aptoide.pt.billing.external.ExternalBillingBinder.RESULT_OK;

public class PurchaseBundleMapper {

  private static final String INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
  private static final String INAPP_DATA_SIGNATURE = "INAPP_DATA_SIGNATURE";
  private static final String INAPP_PURCHASE_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
  private static final String INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
  private static final String INAPP_DATA_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";
  private static final String PRODUCT_ID = "PRODUCT_ID";
  private static final String STATUS = "STATUS";
  private static final String SKU = "SKU";
  private final PaymentThrowableCodeMapper throwableCodeMapper;
  private final PurchaseFactory purchaseFactory;

  public PurchaseBundleMapper(PaymentThrowableCodeMapper throwableCodeMapper,
      PurchaseFactory purchaseFactory) {
    this.throwableCodeMapper = throwableCodeMapper;
    this.purchaseFactory = purchaseFactory;
  }

  public Bundle map(List<Purchase> purchases) {

    final Bundle result = new Bundle();
    final List<String> dataList = new ArrayList<>();
    final List<String> signatureList = new ArrayList<>();
    final List<String> skuList = new ArrayList<>();

    for (Purchase purchase : purchases) {
      dataList.add(purchase.getSignatureData());
      signatureList.add(purchase.getSignature());
      skuList.add(purchase.getSku());
    }

    result.putStringArrayList(INAPP_PURCHASE_DATA_LIST, (ArrayList<String>) dataList);
    result.putStringArrayList(INAPP_PURCHASE_ITEM_LIST, (ArrayList<String>) skuList);
    result.putStringArrayList(INAPP_DATA_SIGNATURE_LIST, (ArrayList<String>) signatureList);
    result.putInt(RESPONSE_CODE, RESULT_OK);
    return result;
  }

  public Bundle map(Purchase purchase) {

    final Bundle bundle = new Bundle();

    bundle.putString(INAPP_PURCHASE_DATA, purchase.getSignatureData());
    bundle.putInt(RESPONSE_CODE, RESULT_OK);

    if (purchase.getSignature() != null) {
      bundle.putString(INAPP_DATA_SIGNATURE, purchase.getSignature());
    }
    bundle.putInt(RESPONSE_CODE, RESULT_OK);
    bundle.putString(PRODUCT_ID, purchase.getProductId());
    bundle.putString(SKU, purchase.getSku());
    bundle.putString(STATUS, purchase.getStatus().name());
    return bundle;
  }

  public Purchase map(int resultCode, Bundle data) throws Throwable {
    if (resultCode == Activity.RESULT_OK) {
      if (data != null) {
        return purchaseFactory.create(data.getString(PRODUCT_ID),
            data.getString(INAPP_DATA_SIGNATURE), data.getString(INAPP_PURCHASE_DATA),
            Purchase.Status.valueOf(data.getString(STATUS)), data.getString(SKU));
      }
      throw new IllegalArgumentException("No purchase provided in result intent.");
    } else if (resultCode == Activity.RESULT_CANCELED) {

      if (data != null && data.containsKey(RESPONSE_CODE)) {
        throw throwableCodeMapper.map(data.getInt(RESPONSE_CODE, -1));
      }
    }

    throw throwableCodeMapper.map(ExternalBillingBinder.RESULT_ERROR);
  }

  public Bundle map(Throwable throwable) {
    final Bundle bundle = new Bundle();
    bundle.putInt(RESPONSE_CODE, throwableCodeMapper.map(throwable));
    return bundle;
  }

  public Bundle mapCancellation() {
    final Bundle bundle = new Bundle();
    bundle.putInt(RESPONSE_CODE, ExternalBillingBinder.RESULT_USER_CANCELLED);
    return bundle;
  }
}
