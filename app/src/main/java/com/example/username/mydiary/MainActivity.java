package com.example.username.mydiary;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;

public class MainActivity extends AppCompatActivity
        implements DiaryListFragment.OnFragmentInteractionListener {
    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mRealm = Realm.getDefaultInstance();

        createTestData();
        showDiaryList();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    private void createTestData() {
        mRealm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                // idフィールドの最大値を取得
                Number maxId = mRealm.where(Diary.class).max("id");
                long nextId = 0;
                if (maxId != null) nextId = maxId.longValue() + 1;
                // createObjectではIDを渡してオブジェクトを生成する
                Diary diary = realm.createObject(Diary.class, new Long(nextId));
                diary.title = "テストタイトル";
                diary.bodyText = "テスト本文です。";
                diary.date = "Feb 22";
            }
        });
    }


    //アクティビティからFragmentManagerのインスタンスを取得する
    private void showDiaryList() {
        FragmentManager manager = getSupportFragmentManager();
        //クラスのインスタンスを生成する
        Fragment fragment = manager.findFragmentByTag("DiaryListFragment");
        if (fragment == null) {
            fragment = new DiaryListFragment();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.add(R.id.content, fragment, "DiaryListFragment");
            transaction.commit();
        }
    }

    @Override
    public void onAddDiarySelected() {
        mRealm.beginTransaction();
        //編集画面の生成前に、採番してDBにレコードを作成する
        Number maxId = mRealm.where(Diary.class).max("id");
        long nextId = 0;
        if (maxId != null) nextId = maxId.longValue() + 1;
        Diary diary = mRealm.createObject(Diary.class, new Long(nextId));
        diary.date = new SimpleDateFormat("MMM d", Locale.US).format(new Date());
        mRealm.commitTransaction();
        //新しい編集画面を生成する
        InputDiaryFragment inputDiaryFragment =
                InputDiaryFragment.newInstance(nextId);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        //replaceは既にあるフラグメントを消して新たに追加する
        transaction.replace(R.id.content, inputDiaryFragment,
                "InputDiaryFragment");
        //戻るボタンで一覧に戻れるようにしている
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
