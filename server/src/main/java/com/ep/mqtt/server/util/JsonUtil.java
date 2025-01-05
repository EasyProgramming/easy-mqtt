package com.ep.mqtt.server.util;

import java.io.IOException;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;

import com.ep.mqtt.server.metadata.BaseEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * @author : zbz
 * @date : 2021/4/16
 */
public class JsonUtil {
    private static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 对象的所有字段全部列入
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        // 取消默认转换timestamps形式
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 忽略空Bean转json的错误
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 所有的日期格式都统一为以下的样式，即yyyy-MM-dd HH:mm:ss
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat(STANDARD_FORMAT));
        // 忽略 在json字符串中存在，但是在java对象中不存在对应属性的情况。防止错误
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Enum.class, new EnumSerializer());
        simpleModule.addDeserializer(Enum.class, new EnumDeserializer());

        OBJECT_MAPPER.registerModule(simpleModule);
    }

    /**
     * 对象转Json格式字符串
     * 
     * @param obj
     *            对象
     * @return Json格式字符串
     */
    public static <T> String obj2String(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * json字符串转对象
     * 
     * @param str
     *            json字符串
     * @param typeReference
     *            反序列化类型
     * @param <T>
     *            类型
     */
    public static <T> T string2Obj(String str, TypeReference<T> typeReference) {
        if (StringUtils.isEmpty(str) || typeReference == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(str, typeReference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * json字符串转对象
     *
     * @param str
     *            json字符串
     * @param tClass
     *            反序列化类型
     * @param <T>
     *            类型
     */
    public static <T> T string2Obj(String str, Class<T> tClass) {
        if (StringUtils.isEmpty(str) || tClass == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(str, tClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class EnumSerializer extends JsonSerializer<Enum> {

        @Override
        public void serialize(Enum anEnum, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
            jsonGenerator.writeObject(BaseEnum.invokeGetMethod(anEnum, BaseEnum.GET_CODE_METHOD_NAME));
        }

    }

    public static class EnumDeserializer extends JsonDeserializer<Enum> implements ContextualDeserializer {

        private Class<?> enumClass;

        public EnumDeserializer() {

        }

        public EnumDeserializer(Class<?> enumClass) {
            this.enumClass = enumClass;
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public Enum deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException {
            JsonToken token = p.getCurrentToken();

            Object value = null;
            switch (token) {
                case VALUE_STRING:
                    value = p.getValueAsString();
                    break;
                case VALUE_NUMBER_INT:
                    value = p.getValueAsInt();
                    break;
                case VALUE_NUMBER_FLOAT:
                    value = p.getValueAsDouble();
                    break;
                case VALUE_TRUE:
                case VALUE_FALSE:
                    value = p.getValueAsBoolean();
                    break;
                case VALUE_NULL:
                    break;
                default:
                    return null;
            }

            return BaseEnum.getByCode(value, (Class<Enum>)this.enumClass);
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext,
            BeanProperty beanProperty) {
            Class<?> clazz = deserializationContext.getContextualType() != null
                ? deserializationContext.getContextualType().getRawClass()
                : beanProperty.getMember().getType().getRawClass();
            return new EnumDeserializer(clazz);
        }
    }

}
