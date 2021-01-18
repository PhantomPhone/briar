package org.briarproject.bramble.autodelete;

import org.briarproject.bramble.api.autodelete.AutoDeleteManager;
import org.briarproject.bramble.api.event.EventBus;
import org.briarproject.bramble.api.lifecycle.LifecycleManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AutoDeleteModule {

	public static class EagerSingletons {
		@Inject
		AutoDeleteManager autoDeleteManager;
	}

	@Provides
	@Singleton
	AutoDeleteManager getAutoDeleteManager(LifecycleManager lifecycleManager,
			EventBus eventBus, AutoDeleteManagerImpl autoDeleteManager) {
		lifecycleManager.registerOpenDatabaseHook(autoDeleteManager);
		eventBus.addListener(autoDeleteManager);
		return autoDeleteManager;
	}
}