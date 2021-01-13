package org.briarproject.briar.android.account;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;

import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.activity.BaseActivity;
import org.briarproject.briar.android.controller.handler.UiResultHandler;
import org.briarproject.briar.android.fragment.BaseFragment.BaseFragmentListener;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.lifecycle.ViewModelProvider;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_TASK_ON_HOME;
import static org.briarproject.briar.android.BriarApplication.ENTRY_ACTIVITY;
import static org.briarproject.briar.android.account.SetupViewModel.State.AUTHORNAME;
import static org.briarproject.briar.android.account.SetupViewModel.State.CREATEACCOUNT;
import static org.briarproject.briar.android.account.SetupViewModel.State.DOZE;
import static org.briarproject.briar.android.account.SetupViewModel.State.SETPASSWORD;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
public class SetupActivity extends BaseActivity
		implements BaseFragmentListener {

	@Inject
	ViewModelProvider.Factory viewModelFactory;
	SetupViewModel viewModel;

	@Override
	public void onCreate(@Nullable Bundle state) {
		super.onCreate(state);
		// fade-in after splash screen instead of default animation
		overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
		setContentView(R.layout.activity_fragment_container);

		viewModel = new ViewModelProvider(this, viewModelFactory)
				.get(SetupViewModel.class);

		viewModel.state.observe(this, this::onStateChanged);

// TODO so i should not have to care about the incoming state Bundle here,
// since we have the ViewModel to keep our state, right?
//		if (state == null) {
//			if (accountManager.accountExists()) throw new AssertionError();
//			showInitialFragment(AuthorNameFragment.newInstance());
//		} else {
//			authorName = viewModel.authorName
//			password = viewModel.password
//		}
	}

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
// TODO do I need to care about this, ViewModel saves us?
//		if (authorName != null)
//			state.putString(STATE_KEY_AUTHOR_NAME, authorName);
//		if (password != null)
//			state.putString(STATE_KEY_PASSWORD, password);
	}

	private void onStateChanged(SetupViewModel.State state) {
		if (state == AUTHORNAME) {
			if (viewModel.accountExists()) throw new AssertionError();
			showInitialFragment(AuthorNameFragment.newInstance());
		} else if (state == SETPASSWORD) {
			showPasswordFragment();
		} else if (state == DOZE) {
			showDozeFragment();
		} else if (state == CREATEACCOUNT) {
			createAccount();
		}
	}

	void showPasswordFragment() {
		if (viewModel.authorName == null) throw new IllegalStateException();
		showNextFragment(SetPasswordFragment.newInstance());
	}

	@TargetApi(23)
	void showDozeFragment() {
		if (viewModel.authorName == null) throw new IllegalStateException();
		if (viewModel.password == null) throw new IllegalStateException();
		showNextFragment(DozeFragment.newInstance());
	}

	public void createAccount() {
		UiResultHandler<Boolean> resultHandler =
				new UiResultHandler<Boolean>(this) {
					@Override
					public void onResultUi(Boolean result) {
						showApp();
					}
				};
		viewModel.createAccount(resultHandler);
	}

	void showApp() {
		Intent i = new Intent(this, ENTRY_ACTIVITY);
		i.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_TASK_ON_HOME |
				FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(i);
		supportFinishAfterTransition();
		overridePendingTransition(R.anim.screen_new_in, R.anim.screen_old_out);
	}

	@Override
	@Deprecated
	public void runOnDbThread(Runnable runnable) {
		throw new RuntimeException("Don't use this deprecated method here.");
	}

}
