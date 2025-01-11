package com.ep.mqtt.server.util;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import javax.annotation.Resource;
import java.util.function.Supplier;

/**
 * @author zbz
 * @date 2023/4/20 11:42
 */
@Component
public class TransactionUtil {

    @Resource
    private DataSourceTransactionManager dataSourceTransactionManager;

    @Resource
    private TransactionDefinition defaultTransactionDefinition;


    public <T> T transaction(Supplier<T> supplier){
       return transaction(null, supplier);
    }

    public <T> T transaction(TransactionDefinition transactionDefinition, Supplier<T> supplier){
        TransactionStatus transactionStatus;
        if (transactionDefinition == null){
            transactionStatus = dataSourceTransactionManager.getTransaction(defaultTransactionDefinition);
        }
        else {
            transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        }

        if (!transactionStatus.isNewTransaction()){
            return supplier.get();
        }

        boolean isCommit = true;
        try {
            return supplier.get();
        }
        catch (Throwable throwable){
            isCommit = false;
            throw throwable;
        }
        finally {
            if(isCommit){
                dataSourceTransactionManager.commit(transactionStatus);
            }
            else {
                dataSourceTransactionManager.rollback(transactionStatus);
            }
        }
    }


}
