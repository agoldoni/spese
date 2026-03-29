package com.spese;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.spese.db.AppDatabase;
import com.spese.db.Bolletta;
import com.spese.db.BollettaDao;
import com.spese.db.PurchaseType;
import com.spese.db.PurchaseTypeDao;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MqttSyncManager {

    private static final String TAG = "MqttSyncManager";
    private static volatile MqttSyncManager INSTANCE;

    private final Context appContext;
    private final AppDatabase db;
    private final BollettaDao bollettaDao;
    private final PurchaseTypeDao purchaseTypeDao;
    private final MqttConfig config;
    private final Gson gson;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private Mqtt3AsyncClient client;
    private boolean connected;
    private OnSyncDataReceivedListener listener;

    public interface OnSyncDataReceivedListener {
        void onDataReceived();
    }

    private MqttSyncManager(Context context) {
        appContext = context.getApplicationContext();
        db = AppDatabase.getInstance(context);
        bollettaDao = db.bollettaDao();
        purchaseTypeDao = db.purchaseTypeDao();
        config = new MqttConfig(context);
        gson = new Gson();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        connected = false;
    }

    public static MqttSyncManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MqttSyncManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MqttSyncManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void setOnSyncDataReceivedListener(OnSyncDataReceivedListener listener) {
        this.listener = listener;
    }

    public void reconnectIfNeeded() {
        executor.execute(() -> {
            if (config.isConfigured() && !connected) {
                connectInternal();
            } else if (!config.isConfigured() && connected) {
                disconnectInternal();
            }
        });
    }

    public void connect() {
        executor.execute(this::connectInternal);
    }

    public void disconnect() {
        executor.execute(this::disconnectInternal);
    }

    private void connectInternal() {
        if (connected && client != null) {
            return;
        }

        try {
            Mqtt3ClientBuilder builder = MqttClient.builder()
                    .useMqttVersion3()
                    .identifier("spese-" + UUID.randomUUID().toString().substring(0, 8))
                    .serverHost(config.getBrokerUrl())
                    .serverPort(config.getPort());

            if (config.isUseTls()) {
                builder.sslWithDefaultConfig();
            }

            client = builder.buildAsync();

            String username = config.getUsername();
            String password = config.getPassword();
            if (!username.isEmpty()) {
                client.connectWith()
                        .cleanSession(false)
                        .simpleAuth()
                        .username(username)
                        .password(password.getBytes(StandardCharsets.UTF_8))
                        .applySimpleAuth()
                        .send()
                        .join();
            } else {
                client.connectWith()
                        .cleanSession(false)
                        .send()
                        .join();
            }

            connected = true;
            Log.i(TAG, "MQTT connected");

            subscribeToTopics();
            publishAll();

        } catch (Exception e) {
            Log.e(TAG, "MQTT connection failed", e);
            connected = false;
        }
    }

    private void disconnectInternal() {
        if (client != null && connected) {
            try {
                client.disconnect().join();
            } catch (Exception e) {
                Log.e(TAG, "MQTT disconnect error", e);
            }
        }
        connected = false;
        client = null;
    }

    private void subscribeToTopics() {
        if (client == null || !connected) return;

        String groupId = config.getGroupId();

        client.subscribeWith()
                .topicFilter("sync/" + groupId + "/tipologie/#")
                .callback(this::handleIncoming)
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Subscribe tipologie failed", throwable);
                    } else {
                        Log.i(TAG, "Subscribed to tipologie topic");
                    }
                });

        client.subscribeWith()
                .topicFilter("sync/" + groupId + "/bollette/#")
                .callback(this::handleIncoming)
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Subscribe bollette failed", throwable);
                    } else {
                        Log.i(TAG, "Subscribed to bollette topic");
                    }
                });
    }

    private void handleIncoming(Mqtt3Publish publish) {
        executor.execute(() -> {
            try {
                String topic = publish.getTopic().toString();
                byte[] payload = publish.getPayloadAsBytes();

                if (topic.contains("/tipologie/")) {
                    handlePurchaseType(topic, payload);
                } else if (topic.contains("/bollette/")) {
                    handleBolletta(topic, payload);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling incoming message", e);
            }
        });
    }

    private void handlePurchaseType(String topic, byte[] payload) {
        String id = topic.substring(topic.lastIndexOf('/') + 1);

        if (payload.length == 0) {
            PurchaseType existing = purchaseTypeDao.getById(id);
            if (existing != null) {
                int count = purchaseTypeDao.countBolletteByPurchaseType(id);
                if (count == 0) {
                    purchaseTypeDao.delete(existing);
                    Log.i(TAG, "Deleted tipologia: " + id);
                    showToast(R.string.mqtt_eliminato_remoto);
                    notifyListener();
                } else {
                    Log.w(TAG, "Cannot delete tipologia " + id + ": used by " + count + " bollette");
                }
            }
            return;
        }

        String json = new String(payload, StandardCharsets.UTF_8);
        PurchaseType remote = gson.fromJson(json, PurchaseType.class);
        PurchaseType local = purchaseTypeDao.getById(id);

        if (local == null) {
            // Gestione conflitto UNIQUE su name: rinominare la locale se esiste con stesso nome
            resolveNameConflict(remote.getName(), id);
            try {
                purchaseTypeDao.insert(remote);
                Log.i(TAG, "Inserted tipologia: " + remote.getName());
                showToast(R.string.mqtt_ricevuta_tipologia);
                notifyListener();
            } catch (Exception e) {
                Log.e(TAG, "Insert tipologia failed: " + e.getMessage());
            }
        } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
            // Se il nome remoto e' diverso dal locale, verificare conflitto con altre tipologie
            if (!remote.getName().equals(local.getName())) {
                resolveNameConflict(remote.getName(), id);
            }
            local.setName(remote.getName());
            local.setDescription(remote.getDescription());
            local.setUpdatedAt(remote.getUpdatedAt());
            try {
                purchaseTypeDao.update(local);
                Log.i(TAG, "Updated tipologia: " + local.getName());
                showToast(R.string.mqtt_ricevuta_tipologia);
                notifyListener();
            } catch (Exception e) {
                Log.e(TAG, "Update tipologia failed: " + e.getMessage());
            }
        }
    }

    private void resolveNameConflict(String name, String excludeId) {
        List<PurchaseType> all = purchaseTypeDao.getAll();
        for (PurchaseType pt : all) {
            if (pt.getName().equals(name) && !pt.getId().equals(excludeId)) {
                int suffix = 2;
                String newName = name + " (" + suffix + ")";
                // Trova un nome univoco
                boolean found = true;
                while (found) {
                    found = false;
                    for (PurchaseType check : all) {
                        if (check.getName().equals(newName)) {
                            suffix++;
                            newName = name + " (" + suffix + ")";
                            found = true;
                            break;
                        }
                    }
                }
                pt.setName(newName);
                pt.setUpdatedAt(System.currentTimeMillis());
                purchaseTypeDao.update(pt);
                Log.i(TAG, "Renamed local tipologia to: " + newName);
                break;
            }
        }
    }

    private void handleBolletta(String topic, byte[] payload) {
        String id = topic.substring(topic.lastIndexOf('/') + 1);

        if (payload.length == 0) {
            Bolletta existing = bollettaDao.getById(id);
            if (existing != null) {
                bollettaDao.delete(existing);
                Log.i(TAG, "Deleted bolletta: " + id);
                showToast(R.string.mqtt_eliminato_remoto);
                notifyListener();
            }
            return;
        }

        String json = new String(payload, StandardCharsets.UTF_8);
        Bolletta remote = gson.fromJson(json, Bolletta.class);

        // FK check: purchaseType must exist
        if (remote.getPurchaseTypeId() != null
                && purchaseTypeDao.getById(remote.getPurchaseTypeId()) == null) {
            Log.w(TAG, "Skipping bolletta " + id + ": purchaseType not found");
            return;
        }

        Bolletta local = bollettaDao.getById(id);

        if (local == null) {
            bollettaDao.insert(remote);
            Log.i(TAG, "Inserted bolletta: " + id);
            showToast(R.string.mqtt_ricevuta_bolletta);
            notifyListener();
        } else if (remote.getUpdatedAt() > local.getUpdatedAt()) {
            local.setPurchaseTypeId(remote.getPurchaseTypeId());
            local.setAmount(remote.getAmount());
            local.setMonth(remote.getMonth());
            local.setYear(remote.getYear());
            local.setUpdatedAt(remote.getUpdatedAt());
            bollettaDao.update(local);
            Log.i(TAG, "Updated bolletta: " + id);
            showToast(R.string.mqtt_ricevuta_bolletta);
            notifyListener();
        }
    }

    private void notifyListener() {
        if (listener != null) {
            mainHandler.post(() -> listener.onDataReceived());
        }
    }

    private void showToast(int resId) {
        mainHandler.post(() -> Toast.makeText(appContext, resId, Toast.LENGTH_SHORT).show());
    }

    // --- Full sync on reconnect ---

    private void publishAll() {
        if (!connected || client == null) return;

        try {
            String groupId = config.getGroupId();

            // Tipologie prima delle bollette (per FK)
            List<PurchaseType> types = purchaseTypeDao.getAll();
            for (PurchaseType pt : types) {
                String topic = "sync/" + groupId + "/tipologie/" + pt.getId();
                String json = gson.toJson(pt);
                client.publishWith()
                        .topic(topic)
                        .payload(json.getBytes(StandardCharsets.UTF_8))
                        .retain(true)
                        .send();
            }

            List<Bolletta> bollette = bollettaDao.getAll();
            for (Bolletta b : bollette) {
                String topic = "sync/" + groupId + "/bollette/" + b.getId();
                String json = gson.toJson(b);
                client.publishWith()
                        .topic(topic)
                        .payload(json.getBytes(StandardCharsets.UTF_8))
                        .retain(true)
                        .send();
            }

            Log.i(TAG, "Full sync: published " + types.size() + " tipologie, " + bollette.size() + " bollette");
        } catch (Exception e) {
            Log.e(TAG, "Full sync failed", e);
        }
    }

    // --- Publish methods ---

    public void publishBolletta(Bolletta b) {
        executor.execute(() -> {
            if (!connected || client == null) return;
            try {
                String topic = "sync/" + config.getGroupId() + "/bollette/" + b.getId();
                String json = gson.toJson(b);
                client.publishWith()
                        .topic(topic)
                        .payload(json.getBytes(StandardCharsets.UTF_8))
                        .retain(true)
                        .send();
                Log.i(TAG, "Published bolletta: " + b.getId());
                showToast(R.string.mqtt_inviato_bolletta);
            } catch (Exception e) {
                Log.e(TAG, "Publish bolletta failed", e);
            }
        });
    }

    public void publishPurchaseType(PurchaseType pt) {
        executor.execute(() -> {
            if (!connected || client == null) return;
            try {
                String topic = "sync/" + config.getGroupId() + "/tipologie/" + pt.getId();
                String json = gson.toJson(pt);
                client.publishWith()
                        .topic(topic)
                        .payload(json.getBytes(StandardCharsets.UTF_8))
                        .retain(true)
                        .send();
                Log.i(TAG, "Published tipologia: " + pt.getId());
                showToast(R.string.mqtt_inviata_tipologia);
            } catch (Exception e) {
                Log.e(TAG, "Publish tipologia failed", e);
            }
        });
    }

    public void publishDeleteBolletta(String id) {
        executor.execute(() -> {
            if (!connected || client == null) return;
            try {
                String topic = "sync/" + config.getGroupId() + "/bollette/" + id;
                client.publishWith()
                        .topic(topic)
                        .payload(new byte[0])
                        .retain(true)
                        .send();
                Log.i(TAG, "Published delete bolletta: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Publish delete bolletta failed", e);
            }
        });
    }

    public void publishDeletePurchaseType(String id) {
        executor.execute(() -> {
            if (!connected || client == null) return;
            try {
                String topic = "sync/" + config.getGroupId() + "/tipologie/" + id;
                client.publishWith()
                        .topic(topic)
                        .payload(new byte[0])
                        .retain(true)
                        .send();
                Log.i(TAG, "Published delete tipologia: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Publish delete tipologia failed", e);
            }
        });
    }

    public boolean isConnected() {
        return connected;
    }
}
