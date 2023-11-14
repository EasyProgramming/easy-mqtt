package com.ep.mqtt.server.util;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zbz
 * @date 2023/11/14 10:24
 */
@Component
public class ValidationUtil {

    @Autowired
    private Validator validator;

    public <T> List<String> validate (T object, Class<?>... groups){
        Set<ConstraintViolation<T>> constraintViolationSet = validator.validate(object, groups);
        if (CollectionUtils.isEmpty(constraintViolationSet)){
            return Lists.newArrayList();
        }
        return constraintViolationSet.stream().map(ConstraintViolation::getMessage).collect(Collectors.toList());
    }

}
