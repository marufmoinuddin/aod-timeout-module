package de.robv.android.xposed;

import java.lang.reflect.Method;

public class XposedHelpers {
    public static Class<?> findClass(String className, ClassLoader loader) {
        return null;
    }
    
    public static void findAndHookMethod(String className, ClassLoader loader, String methodName, Object... paramAndCallback) {}
    
    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return null;
    }
}