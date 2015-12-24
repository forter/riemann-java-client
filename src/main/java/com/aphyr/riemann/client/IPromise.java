package com.aphyr.riemann.client;

import com.aphyr.riemann.client.lang.IDeref;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface IPromise<T> extends IDeref {
  public void deliver(Object value);
  public T deref() throws IOException;
  public T deref(long time, TimeUnit unit) throws IOException;
  public T deref(long time, TimeUnit unit, T timeoutValue) throws IOException;
}
