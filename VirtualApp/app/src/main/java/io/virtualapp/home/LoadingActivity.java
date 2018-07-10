package io.virtualapp.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.env.Constants;
import com.lody.virtual.client.ipc.VActivityManager;

import java.util.Locale;

import io.virtualapp.R;
import io.virtualapp.abs.ui.VActivity;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.home.models.PackageAppData;
import io.virtualapp.home.repo.PackageAppDataStorage;
import io.virtualapp.widgets.EatBeansView;

/**
 * @author Lody
 */

public class LoadingActivity extends VActivity {

    private PackageAppData appModel;
    private EatBeansView loadingView;

    public static void launch(Context context, String packageName, int userId) {
        Intent intent = VirtualCore.get().getLaunchIntent(packageName, userId);
        if (intent != null) {
            Intent loadingPageIntent = new Intent(context, LoadingActivity.class);
            loadingPageIntent.putExtra(Constants.PASS_PKG_NAME_ARGUMENT, packageName);
            loadingPageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            loadingPageIntent.putExtra(Constants.PASS_KEY_INTENT, intent);
            loadingPageIntent.putExtra(Constants.PASS_KEY_USER, userId);
            context.startActivity(loadingPageIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        loadingView = findViewById(R.id.loading_anim);
        int userId = getIntent().getIntExtra(Constants.PASS_KEY_USER, -1);
        String pkg = getIntent().getStringExtra(Constants.PASS_PKG_NAME_ARGUMENT);
        appModel = PackageAppDataStorage.get().acquire(pkg);
        if (appModel == null) {
            Toast.makeText(getApplicationContext(), "Open App:" + pkg + " failed.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView iconView = findViewById(R.id.app_icon);
        iconView.setImageDrawable(appModel.icon);
        TextView nameView = findViewById(R.id.app_name);
        nameView.setText(String.format(Locale.ENGLISH, "Opening %s...", appModel.name));
        Intent intent = getIntent().getParcelableExtra(Constants.PASS_KEY_INTENT);
        if (intent == null) {
            finish();
            return;
        }
        VirtualCore.get().setUiCallback(intent, mUiCallback);
        VUiKit.defer().when(() -> {
            if (!appModel.fastOpen) {
                try {
                    VirtualCore.get().preOpt(appModel.packageName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            VActivityManager.get().startActivity(intent, userId);
        });

    }

    private final VirtualCore.UiCallback mUiCallback = new VirtualCore.UiCallback() {

        @Override
        public void onAppOpened(String packageName, int userId) {
            finish();
        }

        @Override
        public void onOpenFailed(String packageName, int userId) {
            VUiKit.defer().when(() -> {
            }).done((v) -> {
                if (!isFinishing()) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.start_app_failed, packageName),
                            Toast.LENGTH_SHORT).show();
                }
            });

            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (loadingView != null) {
            loadingView.startAnim();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (loadingView != null) {
            loadingView.stopAnim();
        }
    }
}
