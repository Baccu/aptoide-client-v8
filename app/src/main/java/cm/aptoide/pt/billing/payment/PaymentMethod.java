/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 10/08/2016.
 */

package cm.aptoide.pt.billing.payment;

public class PaymentMethod {

  private final String id;
  private final String type;
  private final String name;
  private final String description;
  private final String icon;
  private final boolean defaultMethod;

  public PaymentMethod(String id, String type, String name, String description, String icon,
      boolean defaultMethod) {
    this.id = id;
    this.type = type;
    this.name = name;
    this.description = description;
    this.icon = icon;
    this.defaultMethod = defaultMethod;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public String getIcon() {
    return icon;
  }

  public boolean isDefaultMethod() {
    return defaultMethod;
  }
}