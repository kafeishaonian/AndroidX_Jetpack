package com.example.module_fundamental.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class GlobalEventManager {
    public static final String ACTION_GLOBAL_EVENT = "com.example.module_fundamental.event.ACTION_GLOBAL_EVENT";
    public static final String EVN_NATIVE = "native";
    public static final String GLOBAL_EVENT = "global_event";
    public static final String KEY_SRC = "l_evn";
    private static volatile GlobalEventManager instance;
    private Context context;
    private final Map<String, List<Subscriber>> subscribers = new HashMap<>();

    public interface Subscriber {
        void onGlobalEventReceived(Event event);
    }

    private GlobalEventManager() {
    }

    public static GlobalEventManager getInstance() {
        if (instance == null) {
            synchronized (GlobalEventManager.class) {
                if (instance == null) {
                    instance = new GlobalEventManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        Context applicationContext = context.getApplicationContext();
        this.context = applicationContext;
        BroadcastHelper.registerBroadcast(applicationContext, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Event event = (Event) intent.getParcelableExtra(GlobalEventManager.GLOBAL_EVENT);
                if (event == null) {
                    return;
                }

                if (event.dsts == null) {
                    GlobalEventManager.this.subscribers.values().stream()
                            .filter(Objects::nonNull)
                            .flatMap(List::stream)
                            .collect(Collectors.toList())
                            .forEach(sub -> sub.onGlobalEventReceived(event));
                    return;
                }
                Arrays.stream(event.dsts)
                        .map(GlobalEventManager.this.subscribers::get)
                        .filter(subs -> subs != null && !subs.isEmpty())
                        .flatMap(subs -> new ArrayList<>(subs).stream())
                        .forEach(sub -> sub.onGlobalEventReceived(event));
            }
        }, ACTION_GLOBAL_EVENT);
    }

    public synchronized void register(Subscriber subscriber, String dst) {
        if (subscriber == null || dst == null) {
            throw new IllegalArgumentException("Subscriber and dst must not be null");
        }
        List<Subscriber> subs = this.subscribers.computeIfAbsent(dst, k -> new LinkedList<>());
        if (!subs.contains(subscriber)) {
            subs.add(subscriber);
        }
    }

    public synchronized void unregister(Subscriber subscriber, String dst) {
        if (subscriber == null || dst == null) {
            throw new IllegalArgumentException("Subscriber and dst must not be null");
        }

        this.subscribers.computeIfPresent(dst, (k, subsList) -> {
            subsList.remove(subscriber);
            return subsList.isEmpty() ? null : subsList;
        });
    }

    public synchronized void unregister(String dst) {
        if (dst == null) {
            throw new IllegalArgumentException("dst must not by null");
        }
        List<Subscriber> sub = this.subscribers.remove(dst);
        if (sub != null) {
            sub.clear();
        }
    }

    public synchronized void sendEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not by null");
        }
        event.check();
        Intent intent = new Intent(ACTION_GLOBAL_EVENT);
        intent.putExtra(GLOBAL_EVENT, event);
        BroadcastHelper.sendBroadcast(this.context, intent);
    }


    public synchronized void clear(String... dsts) {
        if (dsts == null) {
            throw new IllegalArgumentException("dsts array must not by null");
        }
        Arrays.stream(dsts)
                .filter(Objects::nonNull)
                .forEach(this.subscribers::remove);
    }

    public synchronized void clearAll() {
        this.subscribers.clear();
    }

    public static class Event implements Parcelable {

        private String[] dsts;
        private Map<String, Object> msg;
        private String name;
        private String src;


        public Event(JSONObject json) {
            this.name = (String) json.get("event_name");
            this.dsts = ((String) json.get("dst_l_evn")).split("\\|");
            this.msg = (Map) json.get("event_msg");
            this.src = (String) json.get(GlobalEventManager.KEY_SRC);
        }

        public Event(String name) {
            this.name = name;
        }

        public Event dst(String... dsts) {
            this.dsts = dsts;
            return this;
        }

        public Event src(String src) {
            this.src = src;
            return this;
        }

        public Event msg(Map<String, Object> msg) {
            this.msg = msg;
            return this;
        }

        public Event msg(String msg) {
            if (TextUtils.isEmpty(msg)) {
                this.msg = null;
            } else {
                this.msg = (Map) JSON.parse(msg);
            }
            return this;
        }

        void check() {
            String[] strArr;
            if (TextUtils.isEmpty(this.name) || (strArr = this.dsts) == null || strArr.length == 0) {
                throw new IllegalArgumentException("name dsts src cannot be empty!");
            }
        }

        public String toString() {
            return getObj().toString();
        }

        public Map<String, Object> toMap() {
            Map<String, Object> ret = new HashMap<>();
            ret.put("event_name", this.name);
            ret.put("dst_l_evn", dstToString());
            ret.put(GlobalEventManager.KEY_SRC, this.src);
            ret.put("event_msg", this.msg);
            return ret;
        }

        private JSONObject getObj() {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("event_name", (Object) this.name);
            jsonObject.put("dst_l_evn", (Object) dstToString());
            jsonObject.put(GlobalEventManager.KEY_SRC, (Object) this.src);
            jsonObject.put("event_msg", (Object) this.msg);
            return jsonObject;
        }

        public String getResult() {
            JSONObject resultObj = new JSONObject();
            resultObj.put("result", (Object) getObj());
            return resultObj.toString();
        }

        public String getName() {
            return this.name;
        }

        public String[] getDsts() {
            return this.dsts;
        }

        public String getSrc() {
            return this.src;
        }

        public Map<String, Object> getMsg() {
            return this.msg;
        }

        public int getInt(String key, int defValue) {
            Object value = getValue(key, null);
            if (value == null) {
                return defValue;
            }
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException e) {
                return defValue;
            }
        }

        public String getString(String key, String defValue) {
            Object value = getValue(key, null);
            if (value == null) {
                return defValue;
            }
            return String.valueOf(value);
        }

        private Object getValue(String key, Object defValue) {
            Map<String, Object> map = this.msg;
            if (map == null || !map.containsKey(key)) {
                return defValue;
            }
            return this.msg.get(key);
        }

        protected Event(Parcel in) {
            this.name = in.readString();
            in.readStringArray(this.dsts);
            this.src = in.readString();
            in.readMap(this.msg, Map.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.name);
            dest.writeStringArray(this.dsts);
            dest.writeString(this.src);
            dest.writeMap(this.msg);
        }

        private String dstToString() {
            String[] strArr = this.dsts;
            if (strArr == null) {
                return "";
            }
            int length = strArr.length;
            if (length <= 1) {
                return length > 0 ? strArr[0] : "";
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < length; i++) {
                if (i != 0) {
                    builder.append('|');
                }
                builder.append(this.dsts[i]);
            }
            return builder.toString();
        }

        public static final Creator<Event> CREATOR = new Creator<Event>() {
            @Override
            public Event createFromParcel(Parcel in) {
                return new Event(in);
            }

            @Override
            public Event[] newArray(int size) {
                return new Event[size];
            }
        };
    }
}
