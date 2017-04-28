package ca.valacware.cryptchat;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;

import java.io.File;

public class IOSLauncher extends IOSApplication.Delegate implements NativeInterface {
    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        return new IOSApplication(new CryptChat(this), config);
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }

    @Override
    public void setInputBox(String str) {

    }

    @Override
    public File getFile(String file) {
        return null;
    }

    @Override
    public boolean isMobile() {
        return false;
    }

    @Override
    public boolean isDesktop() {
        return false;
    }
}