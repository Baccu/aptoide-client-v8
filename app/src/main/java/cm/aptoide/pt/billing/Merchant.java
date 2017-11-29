package cm.aptoide.pt.billing;

public class Merchant {

  private final long id;
  private final String name;
  private final String packageName;
  private final int versionCode;

  public Merchant(long id, String name, String packageName, int versionCode) {
    this.id = id;
    this.name = name;
    this.packageName = packageName;
    this.versionCode = versionCode;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getPackageName() {
    return packageName;
  }

  public int getVersionCode() {
    return versionCode;
  }
}
