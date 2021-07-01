package io.operatr.kpow;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.Topology;

import java.util.ArrayList;
import java.util.Properties;

public class StreamsRegistry implements AutoCloseable {

    public static class StreamsAgent {
        private final String _id;

        StreamsAgent(String id) {
            _id = id;
        }

        public String getId() {
            return _id;
        }
    }

    private final Object agent;

    public static Properties filterProperties(Properties props) {
        ArrayList<String> allowedKeys = new ArrayList<>();
        allowedKeys.add("security.protocol");
        allowedKeys.add("sasl.mechanism");
        allowedKeys.add("sasl.jaas.config");
        allowedKeys.add("sasl.login.callback.handler.class");
        allowedKeys.add("ssl.keystore.location");
        allowedKeys.add("ssl.keystore.password");
        allowedKeys.add("ssl.key.password");
        allowedKeys.add("ssl.keystore.type");
        allowedKeys.add("ssl.keymanager.algorithm");
        allowedKeys.add("ssl.truststore.location");
        allowedKeys.add("ssl.truststore.password");
        allowedKeys.add("ssl.truststore.type");
        allowedKeys.add("ssl.trustmanager.algorithm");
        allowedKeys.add("ssl.endpoint.identification.algorithm");
        allowedKeys.add("ssl.provider");
        allowedKeys.add("ssl.cipher.suites");
        allowedKeys.add("ssl.protocol");
        allowedKeys.add("ssl.enabled.protocols");
        allowedKeys.add("ssl.secure.random.implementation");
        allowedKeys.add("ssl.keystore.key");
        allowedKeys.add("ssl.keystore.certificate.chain");
        allowedKeys.add("ssl.truststore.certificates");
        allowedKeys.add("bootstrap.servers");
        Properties nextProps = new Properties();
        for (String key : allowedKeys) {
            if (props.containsKey(key)) {
                nextProps.setProperty(key, String.valueOf(props.get(key)));
            }
        }
        return nextProps;
    }

    public StreamsRegistry(Properties props) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("io.operatr.kpow.agent"));
        IFn agentFn = Clojure.var("io.operatr.kpow.agent", "init-registry");
        require.invoke(Clojure.read("io.operatr.kpow.serdes"));
        IFn serdesFn = Clojure.var("io.operatr.kpow.serdes", "transit-json-serializer");
        Serializer keySerializer = (Serializer) serdesFn.invoke();
        Serializer valSerializer = (Serializer) serdesFn.invoke();
        Properties producerProps = filterProperties(props);
        KafkaProducer producer = new KafkaProducer<>(producerProps, keySerializer, valSerializer);
        agent = agentFn.invoke(producer);
    }

    public StreamsAgent register(KafkaStreams streams, Topology topology) {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("io.operatr.kpow.agent"));
        IFn registerFn = Clojure.var("io.operatr.kpow.agent", "register");
        String id = (String) registerFn.invoke(agent, streams, topology);
        if (id != null) {
            return new StreamsAgent(id);
        } else {
            return null;
        }
    }

    public void unregister(StreamsAgent agent) {
        if (agent != null) {
            IFn require = Clojure.var("clojure.core", "require");
            require.invoke(Clojure.read("io.operatr.kpow.agent"));
            IFn unregisterFn = Clojure.var("io.operatr.kpow.agent", "unregister");
            unregisterFn.invoke(agent.getId());
        }
    }

    @Override
    public void close() throws Exception {
        IFn require = Clojure.var("clojure.core", "require");
        require.invoke(Clojure.read("io.operatr.kpow.agent"));
        IFn closeFn = Clojure.var("io.operatr.kpow.agent", "close-registry");
        closeFn.invoke(agent);
    }
}