package de.robv.android.xposed;

public abstract class XC_MethodHook {
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
    
    public static class MethodHookParam {
        public Object[] args;
        public Object result;
    }
}