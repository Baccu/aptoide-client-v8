/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 10/08/2016.
 */

package cm.aptoide.pt.billing.payment;

import cm.aptoide.pt.billing.authorization.Authorization;
import java.util.List;

public class PaymentMethod {

  private final String id;
  private final String type;
  private final String name;
  private final String description;
  private final String icon;
  private final boolean defaultMethod;
  private final List<Authorization> authorizations;
  private final Authorization selectedAuthorization;

  public PaymentMethod(String id, String type, String name, String description, String icon,
      boolean defaultMethod, List<Authorization> authorizations,
      Authorization selectedAuthorization) {
    this.id = id;
    this.type = type;
    this.name = name;
    this.description = description;
    this.icon = icon;
    this.defaultMethod = defaultMethod;
    this.authorizations = authorizations;
    this.selectedAuthorization = selectedAuthorization;
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

  public List<Authorization> getAuthorizations() {
    return authorizations;
  }

  public boolean isDefaultMethod() {
    return defaultMethod;
  }

  public Authorization getSelectedAuthorization() {
    return selectedAuthorization;
  }
}