package com.chtj.base_framework.upgrade;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chtj.base_framework.FBaseTools;
import com.chtj.base_framework.R;
import com.chtj.base_framework.entity.UpgradeBean;

/**
 * @author chtj
 */
public class FUpgradeDialog {
    private static final String TAG = "FUpgradeDialog";
    private static volatile FUpgradeDialog fDialog;
    private AlertDialog dialog = null;
    private boolean isShow=false;
    private String otaPath;
    private ProgressBar progressBar;
    private TextView tvResult;
    private static final int TASK_WARNING = 0x1001;
    private static final int TASK_ERR = 0x1002;
    private static final int TASK_PROGRESS = 0x1003;

    Handler mHandler = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TASK_PROGRESS:
                    tvResult.setTextColor(Color.WHITE);
                    int installStatus = Integer.parseInt(msg.obj.toString());
                    if (installStatus == FExtraTools.I_CHECK) {
                        tvResult.setText(R.string.status_check_firmware);
                    } else if (installStatus == FExtraTools.I_COPY) {
                        tvResult.setText(R.string.status_start_copefw);
                    } else if (installStatus == FExtraTools.I_INSTALLING) {
                        tvResult.setText(R.string.status_start_writefw);
                        dismissDialog();
                    }
                    break;
                case TASK_WARNING:

                    break;
                case TASK_ERR:
                    tvResult.setTextColor(Color.RED);
                    tvResult.setText(String.format(FBaseTools.getContext().getString(R.string.status_update_fail),msg.obj.toString()));
                    break;
            }
        }
    };

    /**
     * 单例模式
     */
    private static FUpgradeDialog instance() {
        if (fDialog == null) {
            synchronized (FUpgradeDialog.class) {
                if (fDialog == null) {
                    fDialog = new FUpgradeDialog();
                }
            }
        }
        return fDialog;
    }

    /**
     * 显示ota升级弹出框
     */
    public static void showUpdateDialog(String otaPath, Context context) {
        if (!instance().isShow){
            instance().otaPath=otaPath;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View pbView = LayoutInflater.from(context).inflate(R.layout.view_alert, null);
            builder.setTitle(R.string.dialog_title);
            builder.setMessage(String.format(context.getString(R.string.dialog_chek_otapath),instance().otaPath));
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setPositiveButton(R.string.dialog_confirm,null);
            fDialog.dialog = builder.create();
            fDialog.dialog.setView(pbView);
            fDialog.dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    instance().isShow=false;
                }
            });
            fDialog.dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            fDialog.dialog.show();
            fDialog.dialog.setCanceledOnTouchOutside(true);
            fDialog.progressBar = fDialog.dialog.findViewById(R.id.pbView);
            fDialog.tvResult = fDialog.dialog.findViewById(R.id.tvResult);
            fDialog.dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    instance().progressBar.setVisibility(View.VISIBLE);
                    instance().tvResult.setVisibility(View.VISIBLE);
                    Log.d(TAG, "onClick: setSingleChoiceItems selectPathInfo=" + instance().otaPath);
                    FUpgradeTools.firmwareUpgrade(new UpgradeBean(instance().otaPath, new FUpgradeInterface() {
                        @Override
                        public void installStatus(int installStatus) {
                            Log.d(TAG, "operating:installStatus =" + installStatus);
                            instance().mHandler.sendMessage(instance().mHandler.obtainMessage(TASK_PROGRESS,installStatus));
                        }

                        @Override
                        public void error(String error) {
                            Log.d(TAG, "error:error =" + error);
                            instance().mHandler.sendMessage(instance().mHandler.obtainMessage(TASK_ERR,error));
                        }

                        @Override
                        public void warning(String warning) {
                            Log.d(TAG, "warning:warning = " + warning);
                            instance().mHandler.sendMessage(instance().mHandler.obtainMessage(TASK_WARNING,warning));
                        }
                    }));
                }
            });
            instance().isShow=true;
        }
    }

    public static void dismissDialog() {
        if (instance().dialog != null) {
            instance().dialog.dismiss();
        }
    }

    public static void dismissDialog(String otaPath) {
        boolean nowOtaPath=TextUtils.isEmpty(otaPath);
        boolean oldOtaPath=TextUtils.isEmpty(instance().otaPath);
        if (nowOtaPath&&oldOtaPath){
            dismissDialog();
        }
        if (!nowOtaPath&&!oldOtaPath&&instance().otaPath.contains(otaPath)){
            dismissDialog();
        }
    }

    public static  boolean isShow(){
        return instance().isShow;
    }
}
