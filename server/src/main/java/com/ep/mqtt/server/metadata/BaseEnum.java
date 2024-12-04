package com.ep.mqtt.server.metadata;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * 枚举基类
 * 
 * @author zbz
 * @date 2024/7/16 16:50
 */
public interface BaseEnum<T> {

    String GET_CODE_METHOD_NAME = "getCode";

    String GET_DESC_METHOD_NAME = "getDesc";


    /**
     * 获取编码
     * 
     * @return 枚举编码
     */
    T getCode();

    /**
     * 获取描述
     * 
     * @return 枚举描述
     */
    String getDesc();

    /**
     * 根据枚举值获取对应枚举
     * @param code 枚举值
     * @param clazz 枚举类
     * @param <E> 枚举类的泛型
     * @param <T> 枚举值的泛型
     * @return 枚举
     */
    static <E extends Enum<E>, T> E getByCode(T code, Class<E> clazz) {
       check(clazz);

        E[] enumConstants = clazz.getEnumConstants();

        for (E enumConstant : enumConstants) {
            Object enumConstantCode = invokeGetMethod(enumConstant, GET_CODE_METHOD_NAME);

            if (code == null) {
                if (enumConstantCode == null) {
                    return enumConstant;
                }
            } else {
                if (code.equals(enumConstantCode)) {
                    return enumConstant;
                }
            }
        }

        return null;
    }

    /**
     * 判断是否实现了BaseEnum
     * 
     * @param clazz
     *            enum类型
     * @return 结果
     */
    static boolean isBaseEnum(Class<?> clazz) {
        return clazz != null && clazz.isEnum() && (BaseEnum.class.isAssignableFrom(clazz));
    }

    /**
     * 通过反射执行枚举的get方法
     * 
     * @param object
     *            实例
     * @param methodName 方法名
     * @return 方法返回值
     */
    static Object invokeGetMethod(Enum<?> object, String methodName) {
        check(object.getDeclaringClass());
        Method getMethod = ReflectionUtils.findMethod(object.getDeclaringClass(), methodName);
        Assert.notNull(getMethod, String.format("未找到[%s]方法", methodName));
        return ReflectionUtils.invokeMethod(getMethod, object);
    }

    /**
     * 必要校验
     * @param clazz 枚举类型
     * @param <E> 枚举泛型
     */
    static <E extends Enum<E>> void check(Class<E> clazz){
        if (clazz == null) {
            throw new IllegalArgumentException("枚举类型不能为null");
        }

        if (!isBaseEnum(clazz)) {
            throw new IllegalArgumentException(String.format("[%s]必须实现BaseEnum", clazz.getName()));
        }
    }
}
