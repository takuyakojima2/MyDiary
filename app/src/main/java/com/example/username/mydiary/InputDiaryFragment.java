package com.example.username.mydiary;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.IOException;

import io.realm.Realm;

import static android.app.Activity.RESULT_OK;


public class InputDiaryFragment extends Fragment {

    private static final String DIARY_ID = "DIARY_ID";
    private static final int REQUEST_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private long mDiaryId;
    private Realm mRealm;
    private EditText mTitleEdit;
    private EditText mBodyEdit;
    private ImageView
            mDiaryImage;

    //MainActivityで新い変種画面を生成するときに採番するよう
    public static InputDiaryFragment newInstance(long diaryId) {
        InputDiaryFragment fragment = new InputDiaryFragment();
        Bundle args = new Bundle();
        args.putLong(DIARY_ID, diaryId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDiaryId = getArguments().getLong(DIARY_ID);
        }
        mRealm = Realm.getDefaultInstance();


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_input_diary,
                container, false);
        mTitleEdit = (EditText) v.findViewById(R.id.title);
        mBodyEdit = (EditText) v.findViewById(R.id.bodyEditText);
        mDiaryImage = (ImageView) v.findViewById(R.id.diary_photo);

        mDiaryImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestReadStorage(view);
            }
        });
        //編集画面のTextView(タイトル)の文字が変更されたときに呼び出される
        mTitleEdit.addTextChangedListener(new TextWatcher() {
            //変更されるときに呼び出される？
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            //入力されるときに呼び出される
            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            //afterTextChangedで入力が終了した時点の文字列を取得する
            @Override
            public void afterTextChanged(final Editable s) {
                //executeTransactionAsync-別スレッドでDB処理をする
                mRealm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        Diary diary = realm.where(Diary.class).equalTo("id",
                                mDiaryId).findFirst();

                        diary.title = s.toString();
                    }
                });
            }
        });
        //編集画面の内容が変更されたときに呼び出される
        mBodyEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                mRealm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        Diary diary = realm.where(Diary.class).equalTo("id",
                                mDiaryId).findFirst();
                        diary.bodyText = s.toString();
                    }
                });
            }
        });
        return v;
    }

    //パーミッション(端末の画像フォルダ)へのアクセスが許可されているかどうか
    private void requestReadStorage(View view) {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            //権限を許可しないを押された場合の処理
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {


                Snackbar.make(view, R.string.rationale, Snackbar.LENGTH_LONG).show();
            }
            //許可・許可しないのダイアログを表示する
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
            }, PERMISSION_REQUEST_CODE);
        } else {
            pickImage();
        }
    }

    //画像の選択
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        //startActivityForResult指定したアクティビティを実行する
        startActivityForResult(
                //Intent.createChooserでimageを洗濯できるアプリを選ばせる
                Intent.createChooser(intent, getString(R.string.pick_image)),
                REQUEST_CODE);
    }

    //呼び出し先のアクティビティが終了したときに呼び出される
    //今回は上のstartActivityForResultで開いたアクティビティが終わったとき
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //REQUEST_CODEが呼び出し元と同じか調べて、結果がOKなら画像を処理する
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            //選択した画像は変数"data"に、な書を示す"Uri"の形で帰ってくる
            Uri
                    uri = (data == null) ? null : data.getData();
            if (uri != null) {
                try {
                    //"Uri"から画像を取り込むにはgetImageFromStreamで行う必要がある
                    Bitmap img = MyUtils.getImageFromStream(
                            getActivity().getContentResolver(), uri);
                    mDiaryImage.setImageBitmap(img);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mRealm.executeTransactionAsync(new Realm.Transaction() {
                    @Override
                    public void execute(Realm realm) {
                        //このインスタンスのIDで読み込んだ画像を登録するレコードを検索
                        Diary diary =
                                realm.where(Diary.class).equalTo("id", mDiaryId)
                                        .findFirst();
                        //上のgetImageFromStreamで取り込んだ画像をbitmapにして、それをbyteに変換。その後DBへ
                        BitmapDrawable
                                bitmap = (BitmapDrawable) mDiaryImage.getDrawable();
                        byte[] bytes = MyUtils.getByteFromImage(bitmap.getBitmap());
                        if (bytes != null && bytes.length > 0) {
                            //DBに画像を保存
                            diary.image = bytes;
                        }


                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length != 1 ||
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(mDiaryImage, R.string.permission_deny,
                        Snackbar.LENGTH_LONG).show();
            } else {
                pickImage();


            }
        }
    }


}
