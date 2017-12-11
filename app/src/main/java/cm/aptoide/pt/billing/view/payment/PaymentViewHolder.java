package cm.aptoide.pt.billing.view.payment;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import cm.aptoide.pt.R;
import cm.aptoide.pt.billing.payment.PaymentService;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.view.spannable.SpannableFactory;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import rx.subjects.PublishSubject;

public class PaymentViewHolder extends RecyclerView.ViewHolder {

  private final PublishSubject<PaymentService> paymentSubject;
  private final TextView paymentText;
  private final SpannableFactory spannableFactory;

  public PaymentViewHolder(View itemView, PublishSubject<PaymentService> paymentSubject,
      SpannableFactory spannableFactory) {
    super(itemView);
    this.paymentSubject = paymentSubject;
    this.paymentText = (TextView) itemView.findViewById(R.id.item_payment_method_text);
    this.spannableFactory = spannableFactory;
  }

  public void setPaymentMethod(PaymentService service) {
    itemView.setOnClickListener(view -> paymentSubject.onNext(service));
    Glide.with(itemView.getContext())
        .load(service.getIcon())
        .into(new TextViewTarget(AptoideUtils.ScreenU.getPixelsForDip(16, itemView.getResources()),
            paymentText));

    CharSequence text;
    if (TextUtils.isEmpty(service.getDescription())) {
      text = service.getName();
    } else {
      text = spannableFactory.createTextAppearanceSpan(itemView.getContext(),
          R.style.TextAppearance_Aptoide_Small, service.getName() + "\n" + service.getDescription(),
          service.getDescription());
    }
    paymentText.setText(text);
  }

  private static class TextViewTarget extends SimpleTarget<GlideDrawable> {

    private TextView text;

    public TextViewTarget(int pixels, TextView text) {
      super(pixels, pixels);
      this.text = text;
    }

    @Override public void onResourceReady(GlideDrawable glideDrawable,
        GlideAnimation<? super GlideDrawable> glideAnimation) {
      text.setCompoundDrawablesWithIntrinsicBounds(null, null, glideDrawable.getCurrent(), null);
    }

    @Override public void onDestroy() {
      text = null;
      super.onDestroy();
    }
  }
}
