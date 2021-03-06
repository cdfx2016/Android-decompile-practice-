package retrofit2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import retrofit2.CallAdapter.Factory;

final class DefaultCallAdapterFactory extends Factory {
    static final Factory INSTANCE = new DefaultCallAdapterFactory();

    DefaultCallAdapterFactory() {
    }

    public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (Factory.getRawType(returnType) != Call.class) {
            return null;
        }
        final Type responseType = Utils.getCallResponseType(returnType);
        return new CallAdapter<Call<?>>() {
            public Type responseType() {
                return responseType;
            }

            public <R> Call<R> adapt(Call<R> call) {
                return call;
            }
        };
    }
}
