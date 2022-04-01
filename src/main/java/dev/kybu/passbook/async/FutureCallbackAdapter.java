package dev.kybu.passbook.async;

import com.google.common.util.concurrent.FutureCallback;

public abstract class FutureCallbackAdapter<V> implements FutureCallback<V> {

    @Override
    public void onSuccess(V v) {

    }

    @Override
    public void onFailure(Throwable throwable) {
        throwable.printStackTrace();
    }
}
