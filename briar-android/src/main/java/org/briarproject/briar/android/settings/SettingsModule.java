package org.briarproject.briar.android.settings;

import org.briarproject.briar.android.conversation.ImageViewModel;
import org.briarproject.briar.android.viewmodel.ViewModelKey;

import androidx.lifecycle.ViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class SettingsModule {

	@Binds
	@IntoMap
	@ViewModelKey(ImageViewModel.class)
	abstract ViewModel bindSettingsViewModel(
			SettingsViewModel settingsViewModel);

}
