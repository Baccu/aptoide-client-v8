package cm.aptoide.pt.spotandshare.socket.message.messages;

import cm.aptoide.pt.spotandshare.socket.entities.AndroidAppInfo;
import cm.aptoide.pt.spotandshare.socket.entities.Host;
import java.io.Serializable;
import lombok.ToString;

/**
 * Created by neuro on 29-01-2017.
 */
@ToString(callSuper = true) public class RequestPermissionToSend extends AndroidAppInfoMessage
    implements Serializable {

  public RequestPermissionToSend(Host localhost, AndroidAppInfo androidAppInfo) {
    super(localhost, androidAppInfo);
  }
}