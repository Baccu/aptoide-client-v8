package cm.aptoide.pt.dataprovider.ws.v7.billing;

import android.content.SharedPreferences;
import cm.aptoide.pt.dataprovider.BuildConfig;
import cm.aptoide.pt.dataprovider.interfaces.TokenInvalidator;
import cm.aptoide.pt.dataprovider.model.v7.BaseV7Response;
import cm.aptoide.pt.dataprovider.ws.BodyInterceptor;
import cm.aptoide.pt.dataprovider.ws.v7.BaseBody;
import cm.aptoide.pt.dataprovider.ws.v7.V7;
import cm.aptoide.pt.preferences.toolbox.ToolboxManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import okhttp3.OkHttpClient;
import retrofit2.Converter;
import retrofit2.Response;
import rx.Observable;

public class GetAuthorizationsRequest
    extends V7<Response<GetAuthorizationsRequest.ResponseBody>, Void> {

  private final String accessToken;
  private final String customerId;

  public GetAuthorizationsRequest(Void body, String baseHost, OkHttpClient httpClient,
      Converter.Factory converterFactory, BodyInterceptor bodyInterceptor,
      TokenInvalidator tokenInvalidator, String accessToken, String customerId) {
    super(body, baseHost, httpClient, converterFactory, bodyInterceptor, tokenInvalidator);
    this.accessToken = accessToken;
    this.customerId = customerId;
  }

  public static String getHost(SharedPreferences sharedPreferences) {
    return (ToolboxManager.isToolboxEnableHttpScheme(sharedPreferences) ? "http"
        : BuildConfig.APTOIDE_WEB_SERVICES_SCHEME)
        + "://"
        + BuildConfig.APTOIDE_WEB_SERVICES_WRITE_V7_HOST
        + "/api/7/";
  }

  public static GetAuthorizationsRequest of(SharedPreferences sharedPreferences,
      OkHttpClient httpClient, Converter.Factory converterFactory,
      BodyInterceptor<BaseBody> bodyInterceptor, TokenInvalidator tokenInvalidator,
      String accessToken, String customerId) {
    return new GetAuthorizationsRequest(null, getHost(sharedPreferences), httpClient,
        converterFactory, bodyInterceptor, tokenInvalidator, accessToken, customerId);
  }

  @Override
  protected Observable<Response<GetAuthorizationsRequest.ResponseBody>> loadDataFromNetwork(
      Interfaces interfaces, boolean bypassCache) {
    return interfaces.getBillingAuthorizations("Bearer " + accessToken, customerId);
  }

  public static class ResponseBody extends BaseV7Response {

    @JsonProperty("datalist") private Data data;

    public Data getData() {
      return data;
    }

    public void setData(Data data) {
      this.data = data;
    }

    public static class Data {

      private List<GetAuthorizationRequest.ResponseBody.Authorization> list;

      public List<GetAuthorizationRequest.ResponseBody.Authorization> getList() {
        return list;
      }

      public void setList(List<GetAuthorizationRequest.ResponseBody.Authorization> list) {
        this.list = list;
      }
    }
  }
}
