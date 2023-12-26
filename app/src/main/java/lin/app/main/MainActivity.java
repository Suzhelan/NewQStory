package lin.app.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.util.ArrayList;
import java.util.List;

import lin.xposed.BuildConfig;
import lin.xposed.R;
import lin.xposed.databinding.ActivityMainBinding;
import lin.xposed.hook.view.main.itemview.AddTelegramChannel;
import lin.xposed.hook.view.main.itemview.GithubSourceCode;
import top.linl.annotationprocessor.AnnotationClassNameTools;


public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS_STORAGE = {
            "android.permission.WRITE_EXTERNAL_STORAGE",//外部写
            "android.permission.READ_EXTERNAL_STORAGE"//外部读
    };
    public List<MainItemListView.ItemInfo> itemInfoList = new ArrayList<>();
    private ActivityMainBinding binding;
    private ListView itemListView;
    private ArrayAdapter<MainItemListView.ItemInfo> itemInfoArrayAdapter;

    @Deprecated
    public static boolean verifyStoragePermissions(Context context) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(context,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            return permission == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void RequestPermissions() {
        requestPermissions( PERMISSIONS_STORAGE, 2376);

        /*if (Build.VERSION.SDK_INT >= 23) {// 6.0
            String[] perms = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_PHONE_STATE};
            for (String p : perms) {
                int f = ContextCompat.checkSelfPermission(this, p);
                if (f != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(perms, 0XCF);
                    break;
                }
            }
        }*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                startActivity(intent);
            }
        }

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] android_13_permissions = new String[]{
                    READ_MEDIA_IMAGES,
                    READ_MEDIA_AUDIO,
                    READ_MEDIA_VIDEO
            };
            ActivityCompat.requestPermissions(activity, android_13_permissions, 2);
        }*/

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        this.setContentView(binding.getRoot());
        requestTranslucentStatusBar();

        // 修改状态栏字体颜色，用AndroidX官方兼容API
        WindowInsetsControllerCompat wic = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (wic != null) {
            // true表示Light Mode，状态栏字体呈黑色，反之呈白色
            wic.setAppearanceLightStatusBars(true);
        }

        //查找控件
        itemListView = binding.mainItemListview;
        itemInfoArrayAdapter = new MainItemListView(MainActivity.this, R.id.main_item_listview, itemInfoList);
        itemListView.setAdapter(itemInfoArrayAdapter);
        initView();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void initView() {
        initItem();
        itemInfoArrayAdapter.notifyDataSetChanged();
//        RequestPermissions();
    }

    private void initItem() {
        try {
            String githubOpenSourceCode = "github 开源地址\nSuzhelan/NewQstory";
            MainItemListView.ItemInfo githubOpenSourceCodeItem = new MainItemListView.ItemInfo(githubOpenSourceCode,new GithubSourceCode(this).getOnClick()){
                @Override
                public void onTextViewCreate(TextView textView) {
                    super.onTextViewCreate(textView);
                    textView.setText(MainItemListView.setStringColor(githubOpenSourceCode,githubOpenSourceCode.indexOf("Suzhelan"),githubOpenSourceCode.length(),getColor(R.color.秘色)));
                }
            };
            itemInfoList.add(githubOpenSourceCodeItem);

            String telegram = "Telegram channel(telegram构建频道)\nhttps://t.me/WhenFlowersAreInBloom";
            MainItemListView.ItemInfo telegramItem = new MainItemListView.ItemInfo(telegram,new AddTelegramChannel(this).getOnClick()){
                @Override
                public void onTextViewCreate(TextView textView) {
                    super.onTextViewCreate(textView);
                    textView.setText(MainItemListView.setStringColor(telegram,telegram.indexOf("https:"),telegram.length(),getColor(R.color.秘色)));
                }
            };
            itemInfoList.add(telegramItem);

            //获取和设置构建时间
            String buildTime = (String) MainActivity.class.getClassLoader().loadClass(AnnotationClassNameTools.getClassName()).getField("BUILD_TIME").get(null);
            String versionAndBuildTimeInfo = String.format("Current version : %s%nBuild time : %s", BuildConfig.VERSION_NAME, buildTime);
            itemInfoList.add(new MainItemListView.ItemInfo(versionAndBuildTimeInfo, null));

        } catch (Exception e) {

        }
    }

    protected void requestTranslucentStatusBar() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(R.color.透明));
        window.setNavigationBarColor(getColor(R.color.透明));
    }

    private String getApk(String packageName) {
        String appDir = null;
        try {
            //通过包名获取程序源文件路径
            appDir = getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appDir;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
