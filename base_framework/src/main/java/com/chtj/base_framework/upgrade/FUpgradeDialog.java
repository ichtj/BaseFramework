package com.chtj.base_framework.upgrade;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.chtj.base_framework.R;
import com.chtj.base_framework.entity.UpgradeBean;

/**
 * @author chtj
 */
public class FUpgradeDialog {
    private static final String TAG = "FUpgradeDialog";
    private static volatile FUpgradeDialog fDialog;
    private AlertDialog dialog = null;
    private ProgressBar progressBar;
    private TextView tvResult;
    private static final int TASK_WARNING = 0x1001;
    private static final int TASK_ERR = 0x1002;
    private static final int TASK_PROGRESS = 0x1003;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TASK_PROGRESS:
                    tvResult.setTextColor(Color.BLACK);
                    int installStatus = Integer.parseInt(msg.obj.toString());
                    if (installStatus == FUpgradeTools.I_CHECK) {
                        tvResult.setText("当前状态：开始检查固件");
                    } else if (installStatus == FUpgradeTools.I_COPY) {
                        tvResult.setText("当前状态：开始拷贝固件");
                    } else if (installStatus == FUpgradeTools.I_INSTALLING) {
                        tvResult.setText("当前状态：即将进入刷机模式");
                        dismissDialog();
                    }
                    break;
                case TASK_WARNING:

                    break;
                case TASK_ERR:
                    tvResult.setTextColor(Color.RED);
                    tvResult.setText("升级异常："+msg.obj.toString());
                    break;
            }
        }
    };

    /**
     * 单例模式
     */
    public static FUpgradeDialog instance() {
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
    public void showUpdateDialog(String otaPath, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View pbView = LayoutInflater.from(context).inflate(R.layout.view_alert, null);
        builder.setTitle("U盘升级");
        builder.setMessage("检测到固件包,路径为：" + otaPath);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("确定",null);
        fDialog.dialog = builder.create();
        fDialog.dialog.setView(pbView);
        fDialog.dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
            }
        });
        fDialog.dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        fDialog.dialog.show();
        // dialog弹出后，点击界面其他部分dialog消失
        fDialog.dialog.setCanceledOnTouchOutside(true);
        fDialog.progressBar = fDialog.dialog.findViewById(R.id.pbView);
        fDialog.tvResult = fDialog.dialog.findViewById(R.id.tvResult);
        //防止点击AlertDialog.BUTTON_POSITIVE 后自动关闭窗口
        fDialog.dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: setSingleChoiceItems selectPathInfo=" + otaPath);
                FUpgradeTools.firmwareUpgrade(new UpgradeBean(otaPath, new FUpgradeInterface() {
                    @Override
                    public void installStatus(int installStatus) {
                        Log.d(TAG, "operating:installStatus =" + installStatus);
                        Message message = mHandler.obtainMessage();
                        message.what = TASK_ERR;
                        message.obj = installStatus;
                        mHandler.sendMessage(message);
                    }

                    @Override
                    public void error(String error) {
                        Log.d(TAG, "error:error =" + error);
                        Message message = mHandler.obtainMessage();
                        message.what = TASK_ERR;
                        message.obj = error;
                        mHandler.sendMessage(message);
                    }

                    @Override
                    public void warning(String warning) {
                        Log.d(TAG, "warning:warning = " + warning);
                        Message message = mHandler.obtainMessage();
                        message.what = TASK_WARNING;
                        message.obj = warning;
                        mHandler.sendMessage(message);
                    }
                }));
            }
        });
    }

    public void dismissDialog() {
        if (fDialog.dialog != null) {
            fDialog.dialog.dismiss();
        }
    }
}
