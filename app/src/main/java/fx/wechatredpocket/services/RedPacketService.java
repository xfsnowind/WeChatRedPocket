package fx.wechatredpocket.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Created by fx on 20.02.18.
 */

public class RedPacketService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "RedPacketService";

    private static final String WECHAT_DETAILS_EN = "Details";
    private static final String WECHAT_DETAILS_CH = "红包详情";
    private static final String WECHAT_BETTER_LUCK_EN = "Better luck next time!";
    private static final String WECHAT_BETTER_LUCK_CH = "手慢了";
    private static final String WECHAT_EXPIRES_CH = "已超过24小时";
    private static final String WECHAT_VIEW_SELF_CH = "查看红包";
    private static final String WECHAT_VIEW_OTHERS_CH = "领取红包";
    private static final String WECHAT_NOTIFICATION_TIP = "[微信红包]";
//    private static final String WECHAT_LUCKMONEY_RECEIVE_ACTIVITY = "LuckyMoneyReceiveUI";
//    private static final String WECHAT_LUCKMONEY_DETAIL_ACTIVITY = "LuckyMoneyDetailUI";
    private static final String WECHAT_LUCKMONEY_GENERAL_ACTIVITY = "LauncherUI";
//    private static final String WECHAT_LUCKMONEY_CHATTING_ACTIVITY = "ChattingUI";
    private String currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;

    private AccessibilityNodeInfo rootNodeInfo, // the root node
            mReceiveNode, // the received red pocket node
            mUnpackNode; // the unpacked red pocket node
    private boolean mLuckyMoneyPicked, // if the red pocket has been picked
            mLuckyMoneyReceived; // if the red pocket has been received
    private int mUnpackCount = 0; // the number of unpicked red pockets
    private boolean mMutex = false,
            mListMutex = false,
            mChatMutex = false;
//    private HongbaoSignature signature = new HongbaoSignature();

//    private PowerUtil powerUtil;
//    private SharedPreferences sharedPreferences;


    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

        setCurrentActivityName(accessibilityEvent);
//        if (sharedPreferences.getBoolean("pref_watch_chat", false))
        watchChat(accessibilityEvent);
    }

    private void setCurrentActivityName(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        try {
            ComponentName componentName = new ComponentName(
                    event.getPackageName().toString(),
                    event.getClassName().toString()
            );

            getPackageManager().getActivityInfo(componentName, 0);
            currentActivityName = componentName.flattenToShortString();
        } catch (PackageManager.NameNotFoundException e) {
            currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;
        }
    }

    private void checkNodeInfo(int eventType) {
        if (this.rootNodeInfo == null) return;

//        if (signature.commentString != null) {
//            sendComment();
//            signature.commentString = null;
//        }

        /* 聊天会话窗口，遍历节点匹配“领取红包”和"查看红包" */
//        AccessibilityNodeInfo node1 = (sharedPreferences.getBoolean("pref_watch_self", false)) ?
//                this.getTheLastNode(WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH) :
//                this.getTheLastNode(WECHAT_VIEW_OTHERS_CH);
        AccessibilityNodeInfo node1 = this.getTheLastNode(WECHAT_VIEW_OTHERS_CH);
        if (node1 != null &&
                currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)
//        (currentActivityName.contains(WECHAT_LUCKMONEY_CHATTING_ACTIVITY)
//                        || currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY))
        ) {
//            String excludeWords = sharedPreferences.getString("pref_watch_exclude_words", "");
//            if (this.signature.generateSignature(node1, excludeWords)) {
                mLuckyMoneyReceived = true;
                mReceiveNode = node1;
                Log.d(TAG, "node view id: " + node1.getViewIdResourceName());
                Log.d(TAG,"node window id: %" + node1.getWindowId());
//                Log.d("sig", this.signature.toString());
//            }
            return;
        }

        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
        AccessibilityNodeInfo node2 = findOpenButton(this.rootNodeInfo);
        if (node2 != null &&
                "android.widget.Button".equals(node2.getClassName())
                && currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)
                ) {
            mUnpackNode = node2;
            mUnpackCount += 1;
            Log.d(TAG, "Found the red packet going to be opened.");
            return;
        }

        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
        boolean hasNodes = this.hasOneOfThoseNodes(
                WECHAT_BETTER_LUCK_CH, WECHAT_DETAILS_CH,
                WECHAT_BETTER_LUCK_EN, WECHAT_DETAILS_EN, WECHAT_EXPIRES_CH);
        if (
//                mMutex
//                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
//                &&
                hasNodes
                && currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)
//                (currentActivityName.contains(WECHAT_LUCKMONEY_DETAIL_ACTIVITY)
//                || currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY))
        ) {
//            mMutex = false;
            mLuckyMoneyPicked = false;
            mUnpackCount = 0;
            Log.d(TAG, "Go back.");
            performGlobalAction(GLOBAL_ACTION_BACK);
//            signature.commentString = generateCommentString();
        }
    }

    private void watchChat(AccessibilityEvent event) {
        this.rootNodeInfo = getRootInActiveWindow();

        if (rootNodeInfo == null)
            return;

        mReceiveNode = null;
        mUnpackNode = null;

        checkNodeInfo(event.getEventType());

        /* 如果已经接收到红包并且还没有戳开 */
        if (mLuckyMoneyReceived && !mLuckyMoneyPicked && (mReceiveNode != null)) {
//            mMutex = true;

            Log.d(TAG,"Received node");
            mReceiveNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mLuckyMoneyReceived = false;
            mLuckyMoneyPicked = true;
        }

        Log.d(TAG,"watch chat");
        /* 如果戳开但还未领取 */
        if (
//                mUnpackCount == 1 &&
                mUnpackNode != null) {
//            int delayFlag = sharedPreferences.getInt("pref_open_delay", 0) * 1000;
            try {
                openPacket();
            } catch (Exception e) {
//                mMutex = false;
                mLuckyMoneyPicked = false;
                mUnpackCount = 0;
            }
//            int delayFlag = 3000;
//            new android.os.Handler().postDelayed(
//                    new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                openPacket();
//                            } catch (Exception e) {
//                                mMutex = false;
//                                mLuckyMoneyPicked = false;
//                                mUnpackCount = 0;
//                            }
//                        }
//                    },
//                    delayFlag);
        }
    }

    private void openPacket() {

        mUnpackNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//        DisplayMetrics metrics = getResources().getDisplayMetrics();
//        float dpi = metrics.density;
//        if (android.os.Build.VERSION.SDK_INT <= 23) {
//            mUnpackNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
//        } else {
//            if (android.os.Build.VERSION.SDK_INT > 23) {
//
//                Path path = new Path();
//                if (640 == dpi) {
//                    path.moveTo(720, 1575);
//                } else {
//                    path.moveTo(540, 1060);
//                }
//                GestureDescription.Builder builder = new GestureDescription.Builder();
//                GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 450, 50)).build();
//                dispatchGesture(gestureDescription, new GestureResultCallback() {
//                    @Override
//                    public void onCompleted(GestureDescription gestureDescription) {
//                        Log.d("test", "onCompleted");
//                        mMutex = false;
//                        super.onCompleted(gestureDescription);
//                    }
//
//                    @Override
//                    public void onCancelled(GestureDescription gestureDescription) {
//                        Log.d("test", "onCancelled");
//                        mMutex = false;
//                        super.onCancelled(gestureDescription);
//                    }
//                }, null);
//
//            }
//        }
    }


    private AccessibilityNodeInfo findOpenButton(AccessibilityNodeInfo node) {
        if (node == null)
            return null;

        //非layout元素
        if (node.getChildCount() == 0) {
            if ("android.widget.Button".equals(node.getClassName()))
                return node;
            else
                return null;
        }

        //layout元素，遍历找button
        AccessibilityNodeInfo button;
        for (int i = 0; i < node.getChildCount(); i++) {
            button = findOpenButton(node.getChild(i));
            if (button != null)
                return button;
        }
        return null;
    }



    private boolean hasOneOfThoseNodes(String... texts) {
        List<AccessibilityNodeInfo> nodes;
        for (String text : texts) {
            if (text == null) continue;

            nodes = this.rootNodeInfo.findAccessibilityNodeInfosByText(text);

            if (nodes != null && !nodes.isEmpty()) return true;
        }
        return false;
    }

    private AccessibilityNodeInfo getTheLastNode(String... texts) {
        int bottom = 0;
        AccessibilityNodeInfo lastNode = null, tempNode;
        List<AccessibilityNodeInfo> nodes;

        for (String text : texts) {
            if (text == null) continue;

            nodes = this.rootNodeInfo.findAccessibilityNodeInfosByText(text);

            if (nodes != null && !nodes.isEmpty()) {
                tempNode = nodes.get(nodes.size() - 1);
                if (tempNode == null) return null;
                Rect bounds = new Rect();
                tempNode.getBoundsInScreen(bounds);
                if (bounds.bottom > bottom) {
                    bottom = bounds.bottom;
                    lastNode = tempNode;
//                    signature.others = text.equals(WECHAT_VIEW_OTHERS_CH);
                }
            }
        }
        return lastNode;
    }

    /**
     * 服务连接
     */
    @Override
    protected void onServiceConnected() {
//        this.watchFlagsFromPreference();
        Toast.makeText(this, "抢红包服务开启", Toast.LENGTH_SHORT).show();
        super.onServiceConnected();
    }

//    private void watchFlagsFromPreference() {
//        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
//
////        this.powerUtil = new PowerUtil(this);
////        Boolean watchOnLockFlag = sharedPreferences.getBoolean("pref_watch_on_lock", false);
////        this.powerUtil.handleWakeLock(watchOnLockFlag);
//    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_watch_on_lock")) {
            Boolean changedValue = sharedPreferences.getBoolean(key, false);
//            this.powerUtil.handleWakeLock(changedValue);
        }
    }

    /**
     * 必须重写的方法：系统要中断此service返回的响应时会调用。在整个生命周期会被调用多次。
     */
    @Override
    public void onInterrupt() {
        Toast.makeText(this, "我快被终结了啊-----", Toast.LENGTH_SHORT).show();
    }

    /**
     * 服务断开
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Toast.makeText(this, "抢红包服务已被关闭", Toast.LENGTH_SHORT).show();
        return super.onUnbind(intent);
    }
}
