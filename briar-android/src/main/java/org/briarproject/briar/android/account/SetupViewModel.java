package org.briarproject.briar.android.account;

import android.app.Application;

import org.briarproject.bramble.api.account.AccountManager;
import org.briarproject.bramble.api.crypto.PasswordStrengthEstimator;
import org.briarproject.bramble.api.lifecycle.IoExecutor;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import static java.util.logging.Logger.getLogger;
import static org.briarproject.briar.android.util.UiUtils.needsDozeWhitelisting;

class SetupViewModel extends AndroidViewModel {
	enum State {AUTHORNAME, SETPASSWORD, DOZE, CREATEACCOUNT, CREATED, FAILED}

	private static final Logger LOG =
			getLogger(SetupActivity.class.getName());

	@Nullable
	private String authorName, password;

	final MutableLiveData<State> state = new MutableLiveData<>(State.AUTHORNAME);

	private final Application app;
	private final AccountManager accountManager;
	private final Executor ioExecutor;
	private final PasswordStrengthEstimator strengthEstimator;

	@Inject
	SetupViewModel(Application app,
			AccountManager accountManager,
			@IoExecutor Executor ioExecutor,
			PasswordStrengthEstimator strengthEstimator) {
		super(app);
		this.app = app;
		this.accountManager = accountManager;
		this.ioExecutor = ioExecutor;
		this.strengthEstimator = strengthEstimator;

		if (accountManager.accountExists()) throw new AssertionError();
	}

	@Nullable
	String getAuthorName() {
		return authorName;
	}

	void setAuthorName(String authorName) {
		this.authorName = authorName;
		state.setValue(State.SETPASSWORD);
	}

	@Nullable
	String getPassword() {
		return password;
	}

	void setPassword(String password) {
		this.password = password;
		if (needToShowDozeFragment()) {
			state.setValue(State.DOZE);
		} else {
			state.setValue(State.CREATEACCOUNT);
		}
	}

	float estimatePasswordStrength(String password) {
	    return strengthEstimator.estimateStrength(password);
    }

	boolean needToShowDozeFragment() {
		return needsDozeWhitelisting(app.getApplicationContext()) ||
				HuaweiView.needsToBeShown(app.getApplicationContext());
	}

	// Package access for testing
	void createAccount() {
		if (state.getValue() != State.CREATEACCOUNT) throw new IllegalStateException();
		if (getAuthorName() == null) throw new IllegalStateException();
		if (getPassword() == null) throw new IllegalStateException();
		ioExecutor.execute(() -> {
			if (accountManager.createAccount(getAuthorName(), getPassword())) {
				LOG.info("Created account");
				this.state.postValue(State.CREATED);
			} else {
				LOG.warning("Failed to create account");
				this.state.postValue(State.FAILED);
			}
		});
	}
}
