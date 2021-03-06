package org.aospextended.android.systemui.dagger;

import com.android.systemui.dagger.DefaultComponentBinder;
import com.android.systemui.dagger.DependencyProvider;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.SystemUIBinder;
import com.android.systemui.dagger.SysUIComponent;
import com.android.systemui.dagger.SystemUIModule;

import org.aospextended.android.systemui.keyguard.KeyguardSliceProviderAEX;
import org.aospextended.android.systemui.smartspace.KeyguardSmartspaceController;

import dagger.Subcomponent;

@SysUISingleton
@Subcomponent(modules = {
        DefaultComponentBinder.class,
        DependencyProvider.class,
        SystemUIBinder.class,
        SystemUIModule.class,
        SystemUIAEXModule.class})
public interface SysUIComponentAEX extends SysUIComponent {
    @SysUISingleton
    @Subcomponent.Builder
    interface Builder extends SysUIComponent.Builder {
        SysUIComponentAEX build();
    }

    /**
     * Member injection into the supplied argument.
     */
    void inject(KeyguardSliceProviderAEX keyguardSliceProviderAEX);

    @SysUISingleton
    KeyguardSmartspaceController createKeyguardSmartspaceController();
}
