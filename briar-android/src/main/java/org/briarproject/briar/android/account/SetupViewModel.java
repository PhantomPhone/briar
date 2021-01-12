package org.briarproject.briar.android.account;

import android.app.Application;

import org.briarproject.bramble.api.crypto.PasswordStrengthEstimator;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import static org.briarproject.briar.android.util.UiUtils.needsDozeWhitelisting;

public class SetupViewModel extends AndroidViewModel {
	enum State {AUTHORNAME, SETPASSWORD, DOZE, CREATEACCOUNT}

	@Nullable
	public String authorName, password;

	public final MutableLiveData<State> state = new MutableLiveData<>(State.AUTHORNAME);

	@Inject
	PasswordStrengthEstimator strengthEstimator;

	private final Application app;

	@Inject
	SetupViewModel(Application app) {
		super(app);
		this.app = app;
	}

    public float estimatePasswordStrength(String password) {
	    return strengthEstimator.estimateStrength(password);
    }

	boolean needToShowDozeFragment() {
		return needsDozeWhitelisting(app.getApplicationContext()) ||
				HuaweiView.needsToBeShown(app.getApplicationContext());
	}
}
