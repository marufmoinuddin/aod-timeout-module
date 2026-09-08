package de.robv.android.xposed;

public abstract class Hook {
    @SuppressWarnings("rawtypes")
    private static final Hook[] emptyArray = new Hook[0];
    
    protected Hook() {}
}
