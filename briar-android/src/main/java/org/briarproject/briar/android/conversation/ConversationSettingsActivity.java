package org.briarproject.briar.android.conversation;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.activity.BriarActivity;
import org.briarproject.briar.android.fragment.BaseFragment.BaseFragmentListener;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;

import static org.briarproject.briar.android.conversation.ConversationActivity.CONTACT_ID;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class ConversationSettingsActivity extends BriarActivity implements
		BaseFragmentListener {

	@Inject
	ViewModelProvider.Factory viewModelFactory;

	private ConversationViewModel viewModel;
	private ContactId contactId;

	@Override
	public void onCreate(@Nullable Bundle bundle) {
		super.onCreate(bundle);

		Intent i = getIntent();
		int id = i.getIntExtra(CONTACT_ID, -1);
		if (id == -1) throw new IllegalStateException();
		contactId = new ContactId(id);

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		setContentView(R.layout.activity_conversation_settings);

		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(ConversationViewModel.class);
		viewModel.setContactId(contactId);
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return false;
	}

}
