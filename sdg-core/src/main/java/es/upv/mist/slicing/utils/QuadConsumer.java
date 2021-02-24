package es.upv.mist.slicing.utils;

public interface QuadConsumer<T,U,V,W> {
    void accept(T arg1, U arg2, V arg3, W arg4);
}
