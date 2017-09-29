/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 10/08/2016.
 */

package cm.aptoide.pt.billing;

public class PaymentMethod {

  private final int id;
  private final String name;
  private final String description;
  private final String icon;

  public PaymentMethod(int id, String name, String description, String icon) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.icon = icon;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getIcon() {
    return icon;
  }
}