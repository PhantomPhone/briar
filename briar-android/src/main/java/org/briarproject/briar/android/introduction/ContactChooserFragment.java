package org.briarproject.briar.android.introduction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.briarproject.bramble.api.contact.Contact;
import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.contact.BaseContactListAdapter.OnContactClickListener;
import org.briarproject.briar.android.contact.ContactListAdapter;
import org.briarproject.briar.android.contact.ContactListItem;
import org.briarproject.briar.android.contact.ContactListViewModel;
import org.briarproject.briar.android.fragment.BaseFragment;
import org.briarproject.briar.android.view.BriarRecyclerView;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import static org.briarproject.bramble.api.nullsafety.NullSafety.requireNonNull;
import static org.briarproject.briar.android.conversation.ConversationActivity.CONTACT_ID;

@UiThread
@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class ContactChooserFragment extends BaseFragment
		implements OnContactClickListener<ContactListItem> {

	public static final String TAG = ContactChooserFragment.class.getName();

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private ContactListViewModel viewModel;
	private final ContactListAdapter adapter = new ContactListAdapter(this);
	private BriarRecyclerView list;
	private ContactId contactId;

	private Contact c1;

	public static ContactChooserFragment newInstance(ContactId id) {
		Bundle args = new Bundle();

		ContactChooserFragment fragment = new ContactChooserFragment();
		args.putInt(CONTACT_ID, id.getInt());
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void injectFragment(ActivityComponent component) {
		component.inject(this);
		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(ContactListViewModel.class);
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container,
			@Nullable Bundle savedInstanceState) {

		View contentView = inflater.inflate(R.layout.list, container, false);

		list = contentView.findViewById(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		list.setAdapter(adapter);
		list.setEmptyText(R.string.no_contacts);

		contactId = new ContactId(requireArguments().getInt(CONTACT_ID));

		viewModel.getContactListItems()
				.observe(getViewLifecycleOwner(), result -> {
					result.onError(this::handleException).onSuccess(items -> {
						removeContact(items);
						adapter.submitList(items);
						if (requireNonNull(items).size() == 0) list.showData();
					});
				});

		return contentView;
	}

	private void removeContact(List<ContactListItem> items) {
		Iterator<ContactListItem> iterator = items.iterator();
		while (iterator.hasNext()) {
			ContactListItem item = iterator.next();
			if (item.getContact().getId().equals(contactId)) {
				c1 = item.getContact();
				iterator.remove();
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		viewModel.loadContacts();
		list.startPeriodicUpdate();
	}

	@Override
	public void onStop() {
		super.onStop();
		list.stopPeriodicUpdate();
	}

	@Override
	public String getUniqueTag() {
		return TAG;
	}

	private void showMessageScreen(Contact c1, Contact c2) {
		IntroductionMessageFragment messageFragment =
				IntroductionMessageFragment
						.newInstance(c1.getId().getInt(), c2.getId().getInt());
		showNextFragment(messageFragment);
	}

	@Override
	public void onItemClick(View view, ContactListItem item) {
		if (c1 == null) throw new IllegalStateException();
		Contact c2 = item.getContact();
		showMessageScreen(c1, c2);
	}
}
