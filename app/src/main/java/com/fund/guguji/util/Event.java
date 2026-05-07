package com.fund.guguji.util;

/**
 * 一次性事件包装，解决 LiveData 重复触发的问题
 */
public class Event<T> {
    private T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    public T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }

    public boolean hasBeenHandled() {
        return handled;
    }
}
