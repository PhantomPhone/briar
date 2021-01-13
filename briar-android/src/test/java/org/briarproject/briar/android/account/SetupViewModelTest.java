package org.briarproject.briar.android.account;

import android.app.Application;

import org.briarproject.bramble.api.account.AccountManager;
import org.briarproject.bramble.api.crypto.PasswordStrengthEstimator;
import org.briarproject.bramble.test.BrambleMockTestCase;
import org.briarproject.bramble.test.ImmediateExecutor;
import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import static junit.framework.Assert.assertTrue;
import static org.briarproject.bramble.api.identity.AuthorConstants.MAX_AUTHOR_NAME_LENGTH;
import static org.briarproject.bramble.util.StringUtils.getRandomString;
import static org.briarproject.briar.android.account.SetupViewModel.State.CREATEACCOUNT;

public class SetupViewModelTest extends BrambleMockTestCase {

	@Rule
	public final InstantTaskExecutorRule testRule =
			new InstantTaskExecutorRule();

	private final AccountManager accountManager =
			context.mock(AccountManager.class);

	private final String authorName = getRandomString(MAX_AUTHOR_NAME_LENGTH);
	private final String password = getRandomString(10);

	private final SetupViewModel viewModel;

	public SetupViewModelTest() {
		context.setImposteriser(ClassImposteriser.INSTANCE);
		viewModel = new SetupViewModel(context.mock(Application.class),
				accountManager,
				new ImmediateExecutor(),
				context.mock(PasswordStrengthEstimator.class));
	}

	@Test
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void testCreateAccount() {
		context.checking(new Expectations() {{
			// Create the account
			oneOf(accountManager).createAccount(authorName, password);
			will(returnValue(true));
		}});

		viewModel.authorName = authorName;
		viewModel.password = password;
		viewModel.state.setValue(CREATEACCOUNT);

		AtomicBoolean called = new AtomicBoolean(false);
		viewModel.createAccount(result -> called.set(true));
		assertTrue(called.get());
	}
}
