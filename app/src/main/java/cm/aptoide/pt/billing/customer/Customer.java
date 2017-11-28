package cm.aptoide.pt.billing.customer;

public class Customer {

  private final String id;
  private final boolean authenticated;

  public Customer(String id, boolean authenticated) {
    this.id = id;
    this.authenticated = authenticated;
  }

  public String getId() {
    return id;
  }

  public boolean isAuthenticated() {
    return authenticated;
  }
}
