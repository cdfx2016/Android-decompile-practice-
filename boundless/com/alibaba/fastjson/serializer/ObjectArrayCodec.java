package com.alibaba.fastjson.serializer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONLexer;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.util.TypeUtils;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public class ObjectArrayCodec implements ObjectSerializer, ObjectDeserializer {
    public static final ObjectArrayCodec instance = new ObjectArrayCodec();

    public final void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.out;
        Object[] array = (Object[]) object;
        if (object == null) {
            out.writeNull(SerializerFeature.WriteNullListAsEmpty);
            return;
        }
        int size = array.length;
        int end = size - 1;
        if (end == -1) {
            out.append((CharSequence) "[]");
            return;
        }
        SerialContext context = serializer.context;
        serializer.setContext(context, object, fieldName, 0);
        Class<?> preClazz = null;
        ObjectSerializer preWriter = null;
        out.append('[');
        int i;
        if (out.isEnabled(SerializerFeature.PrettyFormat)) {
            serializer.incrementIndent();
            serializer.println();
            for (i = 0; i < size; i++) {
                if (i != 0) {
                    out.write(44);
                    serializer.println();
                }
                serializer.write(array[i]);
            }
            serializer.decrementIdent();
            serializer.println();
            out.write(93);
            return;
        }
        Object item;
        i = 0;
        while (i < end) {
            try {
                item = array[i];
                if (item == null) {
                    out.append((CharSequence) "null,");
                } else {
                    if (serializer.containsReference(item)) {
                        serializer.writeReference(item);
                    } else {
                        Class<?> clazz = item.getClass();
                        if (clazz == preClazz) {
                            preWriter.write(serializer, item, null, null, 0);
                        } else {
                            preClazz = clazz;
                            preWriter = serializer.getObjectWriter(clazz);
                            preWriter.write(serializer, item, null, null, 0);
                        }
                    }
                    out.append(',');
                }
                i++;
            } finally {
                serializer.context = context;
            }
        }
        item = array[end];
        if (item == null) {
            out.append((CharSequence) "null]");
        } else {
            if (serializer.containsReference(item)) {
                serializer.writeReference(item);
            } else {
                serializer.writeWithFieldName(item, Integer.valueOf(end));
            }
            out.append(']');
        }
        serializer.context = context;
    }

    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        JSONLexer lexer = parser.lexer;
        if (lexer.token() == 8) {
            lexer.nextToken(16);
            return null;
        } else if (lexer.token() == 4) {
            T bytes = lexer.bytesValue();
            lexer.nextToken(16);
            return bytes;
        } else {
            Class componentClass;
            if (type instanceof GenericArrayType) {
                Type componentType = ((GenericArrayType) type).getGenericComponentType();
                if (componentType instanceof TypeVariable) {
                    TypeVariable typeVar = (TypeVariable) componentType;
                    Type objType = parser.getContext().type;
                    if (objType instanceof ParameterizedType) {
                        ParameterizedType objParamType = (ParameterizedType) objType;
                        Type objRawType = objParamType.getRawType();
                        Type actualType = null;
                        if (objRawType instanceof Class) {
                            TypeVariable[] objTypeParams = ((Class) objRawType).getTypeParameters();
                            for (int i = 0; i < objTypeParams.length; i++) {
                                if (objTypeParams[i].getName().equals(typeVar.getName())) {
                                    actualType = objParamType.getActualTypeArguments()[i];
                                }
                            }
                        }
                        if (actualType instanceof Class) {
                            componentClass = (Class) actualType;
                        } else {
                            componentClass = Object.class;
                        }
                    } else {
                        componentClass = TypeUtils.getClass(typeVar.getBounds()[0]);
                    }
                } else {
                    componentClass = TypeUtils.getClass(componentType);
                }
            } else {
                componentClass = ((Class) type).getComponentType();
                Class cls = componentClass;
            }
            JSONArray array = new JSONArray();
            parser.parseArray(componentClass, array, fieldName);
            return toObjectArray(parser, componentClass, array);
        }
    }

    private <T> T toObjectArray(DefaultJSONParser parser, Class<?> componentType, JSONArray array) {
        if (array == null) {
            return null;
        }
        int size = array.size();
        T objArray = Array.newInstance(componentType, size);
        for (int i = 0; i < size; i++) {
            Object value = array.get(i);
            if (value == array) {
                Array.set(objArray, i, objArray);
            } else if (componentType.isArray()) {
                if (componentType.isInstance(value)) {
                    element = value;
                } else {
                    element = toObjectArray(parser, componentType, (JSONArray) value);
                }
                Array.set(objArray, i, element);
            } else {
                element = null;
                if (value instanceof JSONArray) {
                    boolean contains = false;
                    JSONArray valueArray = (JSONArray) value;
                    int valueArraySize = valueArray.size();
                    for (int y = 0; y < valueArraySize; y++) {
                        if (valueArray.get(y) == array) {
                            valueArray.set(i, objArray);
                            contains = true;
                        }
                    }
                    if (contains) {
                        element = valueArray.toArray();
                    }
                }
                if (element == null) {
                    element = TypeUtils.cast(value, (Class) componentType, parser.getConfig());
                }
                Array.set(objArray, i, element);
            }
        }
        array.setRelatedArray(objArray);
        array.setComponentType(componentType);
        return objArray;
    }

    public int getFastMatchToken() {
        return 14;
    }
}
