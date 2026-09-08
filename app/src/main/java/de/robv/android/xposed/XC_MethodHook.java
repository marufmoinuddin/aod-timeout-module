package de.robv.android.xposed;

public abstract class XC_MethodHook extends Hook {
    
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}
    
    public static class MethodHookParam {
        public Object[] args;
        public Object result;
        public Throwable thrown;
        public boolean replaceCall = false;
        public Object newResult;
    }
}
