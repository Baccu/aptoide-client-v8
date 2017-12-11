package cm.aptoide.pt.billing.view.payment;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import cm.aptoide.pt.R;
import cm.aptoide.pt.billing.payment.PaymentService;
import cm.aptoide.pt.view.spannable.SpannableFactory;
import java.util.List;
import rx.Observable;
import rx.subjects.PublishSubject;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentViewHolder> {

  private final List<PaymentService> paymentMethods;
  private final PublishSubject<PaymentService> paymentSubject;
  private final LayoutInflater inflater;
  private final SpannableFactory spannableFactory;

  public PaymentAdapter(List<PaymentService> paymentMethods,
      PublishSubject<PaymentService> paymentSubject, LayoutInflater inflater,
      SpannableFactory spannableFactory) {
    this.paymentMethods = paymentMethods;
    this.paymentSubject = paymentSubject;
    this.inflater = inflater;
    this.spannableFactory = spannableFactory;
  }

  @Override public PaymentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new PaymentViewHolder(inflater.inflate(R.layout.item_payment_method, parent, false),
        paymentSubject, spannableFactory);
  }

  @Override public void onBindViewHolder(PaymentViewHolder holder, int position) {
    holder.setPaymentMethod(paymentMethods.get(position));
  }

  @Override public int getItemCount() {
    return paymentMethods.size();
  }

  public void updatePaymentMethods(List<PaymentService> newPaymentMethods) {
    paymentMethods.clear();
    paymentMethods.addAll(newPaymentMethods);
    notifyDataSetChanged();
  }

  public Observable<PaymentService> getSelectedPaymentMethod() {
    return paymentSubject;
  }
}
