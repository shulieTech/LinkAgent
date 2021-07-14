package com.pamirs.attach.plugin.mongodb.obj;

import com.mongodb.*;
import com.mongodb.binding.ClusterBinding;
import com.mongodb.binding.ReadBinding;
import com.mongodb.binding.ReadWriteBinding;
import com.mongodb.binding.WriteBinding;
import com.mongodb.client.ClientSession;
import com.mongodb.client.internal.ClientSessionBinding;
import com.mongodb.client.internal.MongoClientDelegate;
import com.mongodb.client.internal.OperationExecutor;
import com.mongodb.lang.Nullable;
import com.mongodb.operation.*;
import com.pamirs.attach.plugin.mongodb.common.ClientSessionBindingBuilderProvider;
import com.pamirs.attach.plugin.mongodb.common.ReadWriteBindingBuilder;
import com.pamirs.pradar.Pradar;
import com.pamirs.pradar.exception.PressureMeasureError;

import static com.mongodb.MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL;
import static com.mongodb.MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL;
import static com.mongodb.ReadPreference.primary;
import static com.mongodb.assertions.Assertions.isTrue;

/**
 * @author angju
 * @date 2020/8/17 14:15
 */
public class BusinessDelegateOperationExecutor implements OperationExecutor {
    private MongoClientDelegate mongoClientDelegate;

    private Object originator;

    private BusinessDelegateOperationExecutor() {
    }

    public BusinessDelegateOperationExecutor(MongoClientDelegate mongoClientDelegate, Object originator) {
        this.mongoClientDelegate = mongoClientDelegate;
        this.originator = originator;
    }


    @Override
    public <T> T execute(final ReadOperation<T> operation, final ReadPreference readPreference, final ReadConcern readConcern) {
        check(operation);
        return execute(operation, readPreference, readConcern, null);
    }

    @Override
    public <T> T execute(final WriteOperation<T> operation, final ReadConcern readConcern) {
        check(operation);
        return execute(operation, readConcern, null);
    }

    @Override
    public <T> T execute(final ReadOperation<T> operation, final ReadPreference readPreference, final ReadConcern readConcern,
                         @Nullable final ClientSession session) {
        check(operation);
        ClientSession actualClientSession = getClientSession(session);
        ReadBinding binding = getReadBinding(readPreference, readConcern, actualClientSession,
                session == null && actualClientSession != null);
        try {
            if (session != null && session.hasActiveTransaction() && !binding.getReadPreference().equals(primary())) {
                throw new MongoClientException("Read preference in a transaction must be primary");
            }
            return operation.execute(binding);
        } catch (MongoException e) {
            labelException(session, e);
            throw e;
        } finally {
            binding.release();
        }
    }

    @Override
    public <T> T execute(final WriteOperation<T> operation, final ReadConcern readConcern, @Nullable final ClientSession session) {
        check(operation);
        ClientSession actualClientSession = getClientSession(session);
        WriteBinding binding = getWriteBinding(readConcern, actualClientSession, session == null && actualClientSession != null);

        try {
            return operation.execute(binding);
        } catch (MongoException e) {
            labelException(session, e);
            throw e;
        } finally {
            binding.release();
        }
    }

    WriteBinding getWriteBinding(final ReadConcern readConcern, @Nullable final ClientSession session, final boolean ownsSession) {
        return getReadWriteBinding(primary(), readConcern, session, ownsSession);
    }

    ReadBinding getReadBinding(final ReadPreference readPreference, final ReadConcern readConcern,
                               @Nullable final ClientSession session, final boolean ownsSession) {
        return getReadWriteBinding(readPreference, readConcern, session, ownsSession);
    }

    ReadWriteBinding getReadWriteBinding(final ReadPreference readPreference, final ReadConcern readConcern,
                                         @Nullable final ClientSession session, final boolean ownsSession) {
        ReadWriteBinding readWriteBinding = new ClusterBinding(mongoClientDelegate.getCluster(), getReadPreferenceForBinding(readPreference, session),
                readConcern);
        if (session != null) {
            ReadWriteBinding readWriteBinding1 = ClientSessionBindingBuilderProvider.build(ClientSessionBinding.class, session, ownsSession, readWriteBinding);
            if(readWriteBinding1 != null){
                readWriteBinding = readWriteBinding1;
            }
        }
        return readWriteBinding;
    }

    private void labelException(final @Nullable ClientSession session, final MongoException e) {
        if ((e instanceof MongoSocketException || e instanceof MongoTimeoutException)
                && session != null && session.hasActiveTransaction() && !e.hasErrorLabel(UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL)) {
            e.addLabel(TRANSIENT_TRANSACTION_ERROR_LABEL);
        }
    }

    private ReadPreference getReadPreferenceForBinding(final ReadPreference readPreference, @Nullable final ClientSession session) {
        if (session == null) {
            return readPreference;
        }
        if (session.hasActiveTransaction()) {
            ReadPreference readPreferenceForBinding = session.getTransactionOptions().getReadPreference();
            if (readPreferenceForBinding == null) {
                throw new MongoInternalException("Invariant violated.  Transaction options read preference can not be null");
            }
            return readPreferenceForBinding;
        }
        return readPreference;
    }

    @Nullable
    ClientSession getClientSession(@Nullable final ClientSession clientSessionFromOperation) {
        ClientSession session;
        if (clientSessionFromOperation != null) {
            isTrue("ClientSession from same MongoClient", clientSessionFromOperation.getOriginator() == originator);
            session = clientSessionFromOperation;
        } else {
            session = mongoClientDelegate.createClientSession(ClientSessionOptions.builder().causallyConsistent(false).build(), ReadConcern.DEFAULT,
                    WriteConcern.ACKNOWLEDGED, ReadPreference.primary());
        }
        return session;
    }

    private void check(boolean isOperateClusterTestArea, boolean isClusterTest) {
        if (isClusterTest && !isOperateClusterTestArea) {
            throw new PressureMeasureError("[error] biz operation get a pressure request");
        } else if (!isClusterTest && isOperateClusterTestArea) {
            throw new PressureMeasureError("[error] cluster test operation get a biz request");
        }
    }

    /**
     * 检查
     */
    private void check(Object t) {
        Boolean ok = true;

        /**
         * 不确认检测的是不是全，所以针对这几种已知的操作进行检测，其他情况下则不检测
         * 此操作类所有的流量都会请求过来,所以需要同时检测压测和正常流量
         */
        boolean isOperateClusterTestArea = false;
        if (t instanceof FindOperation) {
            FindOperation operation = (FindOperation) t;
            isOperateClusterTestArea = Pradar.isClusterTestPrefix(operation.getNamespace().getCollectionName())
                    || Pradar.isClusterTestPrefix(operation.getNamespace().getDatabaseName());
            check(isOperateClusterTestArea, Pradar.isClusterTest());
        } else if (t instanceof InsertOperation) {
            InsertOperation operation = (InsertOperation) t;
            isOperateClusterTestArea = Pradar.isClusterTestPrefix(operation.getNamespace().getCollectionName())
                    || Pradar.isClusterTestPrefix(operation.getNamespace().getDatabaseName());
            check(isOperateClusterTestArea, Pradar.isClusterTest());
        } else if (t instanceof DeleteOperation) {
            InsertOperation operation = (InsertOperation) t;
            isOperateClusterTestArea = Pradar.isClusterTestPrefix(operation.getNamespace().getCollectionName())
                    || Pradar.isClusterTestPrefix(operation.getNamespace().getDatabaseName());
            check(isOperateClusterTestArea, Pradar.isClusterTest());
        } else if (t instanceof UpdateOperation) {
            UpdateOperation operation = (UpdateOperation) t;
            isOperateClusterTestArea = Pradar.isClusterTestPrefix(operation.getNamespace().getCollectionName())
                    || Pradar.isClusterTestPrefix(operation.getNamespace().getDatabaseName());
            check(isOperateClusterTestArea, Pradar.isClusterTest());
        } else if (t instanceof DeleteOperation) {
            DeleteOperation operation = (DeleteOperation) t;
            isOperateClusterTestArea = Pradar.isClusterTestPrefix(operation.getNamespace().getCollectionName())
                    || Pradar.isClusterTestPrefix(operation.getNamespace().getDatabaseName());
            check(isOperateClusterTestArea, Pradar.isClusterTest());
        } else if (t instanceof DeleteOperation) {
            DeleteOperation operation = (DeleteOperation) t;
            isOperateClusterTestArea = Pradar.isClusterTestPrefix(operation.getNamespace().getCollectionName())
                    || Pradar.isClusterTestPrefix(operation.getNamespace().getDatabaseName());
            check(isOperateClusterTestArea, Pradar.isClusterTest());
        } else if (t instanceof FindAndDeleteOperation) {
            FindAndDeleteOperation operation = (FindAndDeleteOperation) t;
            isOperateClusterTestArea = Pradar.isClusterTestPrefix(operation.getNamespace().getCollectionName())
                    || Pradar.isClusterTestPrefix(operation.getNamespace().getDatabaseName());
            check(isOperateClusterTestArea, Pradar.isClusterTest());
        } else if (t instanceof FindAndReplaceOperation) {
            FindAndReplaceOperation operation = (FindAndReplaceOperation) t;
            isOperateClusterTestArea = Pradar.isClusterTestPrefix(operation.getNamespace().getCollectionName())
                    || Pradar.isClusterTestPrefix(operation.getNamespace().getDatabaseName());
            check(isOperateClusterTestArea, Pradar.isClusterTest());
        } else if (t instanceof FindAndUpdateOperation) {
            FindAndUpdateOperation operation = (FindAndUpdateOperation) t;
            isOperateClusterTestArea = Pradar.isClusterTestPrefix(operation.getNamespace().getCollectionName())
                    || Pradar.isClusterTestPrefix(operation.getNamespace().getDatabaseName());
            check(isOperateClusterTestArea, Pradar.isClusterTest());
        } else if (t instanceof MixedBulkWriteOperation) {
            MixedBulkWriteOperation operation = (MixedBulkWriteOperation) t;
            isOperateClusterTestArea = Pradar.isClusterTestPrefix(operation.getNamespace().getCollectionName())
                    || Pradar.isClusterTestPrefix(operation.getNamespace().getDatabaseName());
            check(isOperateClusterTestArea, Pradar.isClusterTest());
        }
    }

}
