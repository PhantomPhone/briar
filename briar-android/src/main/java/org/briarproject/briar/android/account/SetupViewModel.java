package org.briarproject.briar.android.account;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SetupViewModel extends ViewModel {
	enum State {AUTHORNAME, SETPASSWORD, DOZE, CREATEACCOUNT}

	@Nullable
	public String authorName, password;

	public final MutableLiveData<State> state = new MutableLiveData<>(State.AUTHORNAME);

	@Inject
	SetupViewModel() {
	}
}
