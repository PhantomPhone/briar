package org.briarproject.briar.android.contact;

import android.app.Application;

import org.briarproject.bramble.api.connection.ConnectionRegistry;
import org.briarproject.bramble.api.contact.Contact;
import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.contact.ContactManager;
import org.briarproject.bramble.api.contact.event.ContactAddedEvent;
import org.briarproject.bramble.api.contact.event.ContactRemovedEvent;
import org.briarproject.bramble.api.contact.event.PendingContactAddedEvent;
import org.briarproject.bramble.api.contact.event.PendingContactRemovedEvent;
import org.briarproject.bramble.api.db.DatabaseExecutor;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.Transaction;
import org.briarproject.bramble.api.db.TransactionManager;
import org.briarproject.bramble.api.event.Event;
import org.briarproject.bramble.api.event.EventBus;
import org.briarproject.bramble.api.event.EventListener;
import org.briarproject.bramble.api.lifecycle.LifecycleManager;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.plugin.event.ContactConnectedEvent;
import org.briarproject.bramble.api.plugin.event.ContactDisconnectedEvent;
import org.briarproject.bramble.api.system.AndroidExecutor;
import org.briarproject.briar.android.viewmodel.DbViewModel;
import org.briarproject.briar.android.viewmodel.LiveResult;
import org.briarproject.briar.api.android.AndroidNotificationManager;
import org.briarproject.briar.api.client.MessageTracker;
import org.briarproject.briar.api.conversation.ConversationManager;
import org.briarproject.briar.api.conversation.ConversationMessageHeader;
import org.briarproject.briar.api.conversation.event.ConversationMessageReceivedEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.util.LogUtils.logDuration;
import static org.briarproject.bramble.util.LogUtils.logException;
import static org.briarproject.bramble.util.LogUtils.now;

@NotNullByDefault
class ContactListViewModel extends DbViewModel implements EventListener {

	private static final Logger LOG =
			getLogger(ContactListViewModel.class.getName());

	private final ContactManager contactManager;
	private final ConversationManager conversationManager;
	private final ConnectionRegistry connectionRegistry;
	private final EventBus eventBus;
	private final AndroidNotificationManager notificationManager;

	private final MutableLiveData<LiveResult<List<ContactListItem>>>
			contactListItems = new MutableLiveData<>();

	private final MutableLiveData<Boolean> hasPendingContacts =
			new MutableLiveData<>();

	@Inject
	ContactListViewModel(Application application,
			@DatabaseExecutor Executor dbExecutor,
			LifecycleManager lifecycleManager, TransactionManager db,
			AndroidExecutor androidExecutor, ContactManager contactManager,
			ConversationManager conversationManager,
			ConnectionRegistry connectionRegistry, EventBus eventBus,
			AndroidNotificationManager notificationManager) {
		super(application, dbExecutor, lifecycleManager, db, androidExecutor);
		this.contactManager = contactManager;
		this.conversationManager = conversationManager;
		this.connectionRegistry = connectionRegistry;
		this.eventBus = eventBus;
		this.notificationManager = notificationManager;
		this.eventBus.addListener(this);
	}

	@Override
	protected void onCleared() {
		super.onCleared();
		eventBus.removeListener(this);
	}

	void loadContacts() {
		loadList(this::loadContacts, contactListItems::setValue);
	}

	private List<ContactListItem> loadContacts(Transaction txn)
			throws DbException {
		long start = now();
		List<ContactListItem> contacts = new ArrayList<>();
		for (Contact c : contactManager.getContacts(txn)) {
			ContactId id = c.getId();
			MessageTracker.GroupCount count =
					conversationManager.getGroupCount(txn, id);
			boolean connected = connectionRegistry.isConnected(c.getId());
			contacts.add(new ContactListItem(c, connected, count));
		}
		Collections.sort(contacts);
		logDuration(LOG, "Full load", start);
		return contacts;
	}

	@Override
	public void eventOccurred(Event e) {
		if (e instanceof ContactAddedEvent) {
			LOG.info("Contact added, reloading");
			loadContacts();
		} else if (e instanceof ContactConnectedEvent) {
			updateItem(((ContactConnectedEvent) e).getContactId(),
					item -> new ContactListItem(item, true), false);
		} else if (e instanceof ContactDisconnectedEvent) {
			updateItem(((ContactDisconnectedEvent) e).getContactId(),
					item -> new ContactListItem(item, false), false);
		} else if (e instanceof ContactRemovedEvent) {
			LOG.info("Contact removed, removing item");
			removeItem(((ContactRemovedEvent) e).getContactId());
		} else if (e instanceof ConversationMessageReceivedEvent) {
			LOG.info("Conversation message received, updating item");
			ConversationMessageReceivedEvent<?> p =
					(ConversationMessageReceivedEvent<?>) e;
			ConversationMessageHeader h = p.getMessageHeader();
			updateItem(p.getContactId(), item -> new ContactListItem(item, h),
					true);
		} else if (e instanceof PendingContactAddedEvent ||
				e instanceof PendingContactRemovedEvent) {
			checkForPendingContacts();
		}
	}

	LiveData<LiveResult<List<ContactListItem>>> getContactListItems() {
		return contactListItems;
	}

	LiveData<Boolean> getHasPendingContacts() {
		return hasPendingContacts;
	}

	private void updateItem(ContactId c,
			Function<ContactListItem, ContactListItem> replacer, boolean sort) {
		List<ContactListItem> list = updateListItems(contactListItems,
				itemToTest -> itemToTest.getContact().getId().equals(c),
				replacer);
		if (list == null) return;
		if (sort) Collections.sort(list);
		contactListItems.setValue(new LiveResult<>(list));
	}

	private void removeItem(ContactId c) {
		List<ContactListItem> list = removeListItems(contactListItems,
				itemToTest -> itemToTest.getContact().getId().equals(c));
		if (list == null) return;
		contactListItems.setValue(new LiveResult<>(list));
	}

	void checkForPendingContacts() {
		runOnDbThread(() -> {
			try {
				boolean hasPending =
						!contactManager.getPendingContacts().isEmpty();
				hasPendingContacts.postValue(hasPending);
			} catch (DbException e) {
				logException(LOG, WARNING, e);
			}
		});
	}

	void clearAllContactNotifications() {
		notificationManager.clearAllContactNotifications();
	}

	void clearAllContactAddedNotifications() {
		notificationManager.clearAllContactAddedNotifications();
	}

}
