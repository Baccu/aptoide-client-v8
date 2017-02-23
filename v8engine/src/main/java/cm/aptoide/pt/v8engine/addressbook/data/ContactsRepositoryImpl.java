package cm.aptoide.pt.v8engine.addressbook.data;

import android.support.annotation.NonNull;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.dataprovider.DataProvider;
import cm.aptoide.pt.dataprovider.repository.IdsRepositoryImpl;
import cm.aptoide.pt.dataprovider.ws.v7.SyncAddressBookRequest;
import cm.aptoide.pt.interfaces.AptoideClientUUID;
import cm.aptoide.pt.model.v7.Comment;
import cm.aptoide.pt.model.v7.GetFollowers;
import cm.aptoide.pt.preferences.secure.SecurePreferencesImplementation;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.addressbook.utils.ContactUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jdandrade on 15/02/2017.
 */

public class ContactsRepositoryImpl implements ContactsRepository {

  public ContactsRepositoryImpl() {
  }

  @Override public void getContacts(@NonNull LoadContactsCallback callback) {

    ContactUtils contactUtils = new ContactUtils();

    ContactsModel contacts = contactUtils.getContacts(V8Engine.getContext());

    List<String> numbers = contacts.getMobileNumbers();
    List<String> emails = contacts.getEmails();

    AptoideClientUUID aptoideClientUUID =
        new IdsRepositoryImpl(SecurePreferencesImplementation.getInstance(),
            DataProvider.getContext());
    SyncAddressBookRequest.of(AptoideAccountManager.getAccessToken(),
        aptoideClientUUID.getAptoideClientUUID(), numbers, emails)
        .observe()
        .subscribe(getFollowers -> {
          List<Contact> contactList = new ArrayList<>();
          for (GetFollowers.TimelineUser user : getFollowers.getDatalist().getList()) {
            Contact contact = new Contact();
            contact.setStore(user.getStore());
            Comment.User person = new Comment.User();
            person.setAvatar(user.getAvatar());
            person.setName(user.getName());
            contact.setPerson(person);
            contactList.add(contact);
          }
          callback.onContactsLoaded(contactList);
        }, (throwable) -> {
          throwable.printStackTrace();
        });
  }
}
