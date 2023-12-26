package lin.xposed.hook.view.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import lin.xposed.R;
import lin.xposed.common.utils.ViewUtils;
import lin.xposed.hook.view.main.fragment.SettingViewFragment;
import lin.xposed.hook.view.main.itemview.info.ItemUiInfoManager;
import top.linl.activity.BaseActivity;


/**
 * @author 言子 asmclk@163.com
 * @see MainAdapter
 * @see MainLayoutManager
 * @see LinearSpacingItemDecoration
 * <p>
 * 这个包下写了所有的开关 主要的界面逻辑 比较复杂 很难阅读
 */

@SuppressLint("StaticFieldLeak")
public class MainSettingActivity extends BaseActivity {


    public static final int ITEM_LIST_CONTAINER = R.id.itemList_container;
    public static View titleLayout;
    public static TextView leftText, centerText;

    public static void setTitleLeftText(String text) {
        leftText.setText(text);
    }

    public static void setTitleCenterText(String text) {
        centerText.setText(text);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.root_layout);
        requestTranslucentStatusBar();
        ViewUtils.titleBarAdaptsToStatusBar(findViewById(R.id.setting_title_bar));
        initView();

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }

    private void initView() {
        String fragmentTag = "SettingFragment";
        ItemUiInfoManager.init();

        titleLayout = findViewById(R.id.setting_title_bar);
        leftText = titleLayout.findViewById(R.id.title_left_text);
        centerText = titleLayout.findViewById(R.id.title_center_text);
        setTitleLeftText("<");
        setTitleCenterText(getString(R.string.app_name));
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        //通过tag找到对应的fragment
        Fragment currentFragment = fragmentManager.findFragmentByTag(fragmentTag);
        if (currentFragment == null) currentFragment = new SettingViewFragment();
        fragmentTransaction.replace(ITEM_LIST_CONTAINER, currentFragment, "setting");

        fragmentTransaction.commit();

    }


}
