package cm.aptoide.pt.billing;

public class Merchant {

  private final long id;
  private final String name;
  private final int versionCode;

  public Merchant(long id, String name, int versionCode) {
    this.id = id;
    this.name = name;
    this.versionCode = versionCode;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getVersionCode() {
    return versionCode;
  }
}
