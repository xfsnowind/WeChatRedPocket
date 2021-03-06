package fx.wechatredpocket.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.ArrayList;
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
    private static final String WECHAT_LUCKMONEY_RECEIVE_ACTIVITY = "LuckyMoneyReceiveUI";
    private static final String WECHAT_LUCKMONEY_DETAIL_ACTIVITY = "LuckyMoneyDetailUI";
    private static final String WECHAT_LUCKMONEY_GENERAL_ACTIVITY = "LauncherUI";
    private static final String WECHAT_LUCKMONEY_CHATTING_ACTIVITY = "ChattingUI";

    private String currentActivityName = WECHAT_LUCKMONEY_GENERAL_ACTIVITY;

    /* this is used to remeber the current group name, help to remeber
     * the red packet which is send by myself */
    private String currentGroupName = "";
    private int[] redPacketsSentSelf = null;


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
        watchList(accessibilityEvent);
//        watchNotifications(accessibilityEvent);
    }

    private boolean watchNotifications(AccessibilityEvent event) {
        // Not a notification

        System.out.println("event type: " + event.getEventType());
        if (event.getSource() != null && null != event.getSource().getText()) {
            System.out.println(event.getSource().getText().toString());
        }

        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
            return false;

        // Not a hongbao
        String tip = event.getText().toString();
//        if (!tip.contains(WECHAT_NOTIFICATION_TIP)) return true;

        Parcelable parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            Notification notification = (Notification) parcelable;
            try {
                /* 清除signature,避免进入会话后误判 */
//                signature.cleanSignature();

                 notification.contentIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean watchList(AccessibilityEvent event) {
//        if (mListMutex) return false;
//        mListMutex = true;
        AccessibilityNodeInfo eventSource = event.getSource();
        // Not a message
//        System.out.println("The event type: " + event.getEventType());
//        System.out.println("The event source: " + eventSource);
        if (
                event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                eventSource == null)
            return false;

//        List<AccessibilityNodeInfo> nodes = eventSource.findAccessibilityNodeInfosByText(WECHAT_NOTIFICATION_TIP);
//        List<AccessibilityNodeInfo> nodes = eventSource.findAccessibilityNodeInfosByText("bbbbb");

//        List<AccessibilityNodeInfo> node3s = this.rootNodeInfo.findAccessibilityNodeInfosByText(WECHAT_NOTIFICATION_TIP);
        List<AccessibilityNodeInfo> node2s = eventSource.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/apt");
        AccessibilityNodeInfo nodeInfo = null;

        for (AccessibilityNodeInfo node : node2s) {
            if (node.getText().toString().contains(WECHAT_NOTIFICATION_TIP)) {
                nodeInfo = node;
            }
        }
        //增加条件判断currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)
        //避免当订阅号中出现标题为“[微信红包]拜年红包”（其实并非红包）的信息时误判
        if (null != nodeInfo && currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)) {
//            AccessibilityNodeInfo nodeToClick = nodeInfos.get(0);
//            if (nodeToClick == null) return false;
//            CharSequence contentDescription = nodeToClick.getContentDescription();
            System.out.println("Found the red packet in the chat list and click it.");
            nodeInfo.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);

//            if (contentDescription != null
////                    && "android.view.View" == nodeToClick.getClassName()
////                    && !signature.getContentDescription().equals(contentDescription)
//                    ) {
//                nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK);
////                signature.setContentDescription(contentDescription.toString());
//                return true;
//            }
        }
        return false;
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

        List<AccessibilityNodeInfo> groupNameNodes = this.rootNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/hj");
        if (groupNameNodes == null || groupNameNodes.isEmpty()) {
            return;
        }
        this.currentGroupName = groupNameNodes.get(0).getText().toString();
        System.out.println("current group name: " + this.currentGroupName);

        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
        boolean hasNodes = this.hasOneOfThoseNodes(
                WECHAT_BETTER_LUCK_CH, WECHAT_DETAILS_CH,
                WECHAT_BETTER_LUCK_EN, WECHAT_DETAILS_EN, WECHAT_EXPIRES_CH);
        System.out.println("has nodes: " + hasNodes);
        if (
//                mMutex
//                && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
//                &&
                hasNodes &&
//                        currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)
                (currentActivityName.contains(WECHAT_LUCKMONEY_DETAIL_ACTIVITY)
                || currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY))
                ) {
//            mMutex = false;
            mLuckyMoneyPicked = false;
            mUnpackCount = 0;
            System.out.println("Go back.");
            performGlobalAction(GLOBAL_ACTION_BACK);
//            signature.commentString = generateCommentString();
        }

        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
        AccessibilityNodeInfo node2 = findOpenButton(this.rootNodeInfo);
        System.out.println("activity name: " + currentActivityName);
        if (node2 != null) {
            System.out.println("node class: " + node2.getClassName());
        }
        if (node2 != null &&
                "android.widget.Button".equals(node2.getClassName())
                && (currentActivityName.contains(WECHAT_LUCKMONEY_RECEIVE_ACTIVITY)
                        || currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY))
                ) {
            mUnpackNode = node2;
            mUnpackCount += 1;
            System.out.println("Found the red packet going to be opened.");
            return;
        }

        /* 聊天会话窗口，遍历节点匹配“领取红包”和"查看红包"
         * 但是自己发的红包在自己领取以后, 还是会显示未领取的状态, 所以通过记取当前聊天窗口的名字
         * 和红包的rect的bottom值来记录红包 */
//        AccessibilityNodeInfo node1 = (sharedPreferences.getBoolean("pref_watch_self", false)) ?
//                this.getTheLastNode(WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH) :
//                this.getTheLastNode(WECHAT_VIEW_OTHERS_CH);
//        AccessibilityNodeInfo node1 = this.getTheLastNode(WECHAT_VIEW_OTHERS_CH);
        AccessibilityNodeInfo node1 = this.getTheLastNode(WECHAT_VIEW_OTHERS_CH, WECHAT_VIEW_SELF_CH);
        if (node1 != null &&
//                currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY)
        (currentActivityName.contains(WECHAT_LUCKMONEY_CHATTING_ACTIVITY)
                        || currentActivityName.contains(WECHAT_LUCKMONEY_GENERAL_ACTIVITY))
        ) {
//            String excludeWords = sharedPreferences.getString("pref_watch_exclude_words", "");
//            if (this.signature.generateSignature(node1, excludeWords)) {
                mLuckyMoneyReceived = true;
                mReceiveNode = node1;
            System.out.println("The red packet is received");
//                Log.d("sig", this.signature.toString());
//            }
            return;
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

            System.out.println("Received red packet and pick it.");
            mReceiveNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            mLuckyMoneyReceived = false;
            mLuckyMoneyPicked = true;
        }

        /* 如果戳开但还未领取 */
        if (
//                mUnpackCount == 1 &&
                mUnpackNode != null) {
//            int delayFlag = sharedPreferences.getInt("pref_open_delay", 0) * 1000;
            System.out.println("open red packet.");
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

    private int[] getSelfRedPackets() {
        List<AccessibilityNodeInfo> nodes = this.rootNodeInfo.findAccessibilityNodeInfosByText(WECHAT_VIEW_SELF_CH);
        int[] nodeBottoms = null;
        int nodeSize = nodes.size();
        if (nodes != null && !nodes.isEmpty()) {
            nodeBottoms = new int[nodeSize];
            for (int i = 0; i < nodeSize; i++) {
                AccessibilityNodeInfo node = nodes.get(i);
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                nodeBottoms[i] = bounds.bottom;
            }
        }
        return nodeBottoms;
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
