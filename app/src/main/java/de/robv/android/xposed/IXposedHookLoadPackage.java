package de.robv.android.xposed;

public interface IXposedHookLoadPackage {
    void handleLoadPackage(LoadPackageParam lpparam) throws Throwable;
}