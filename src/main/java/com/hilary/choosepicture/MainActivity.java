package com.hilary.choosepicture;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.hilary.choosepicture.util.FileUtil;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    /** output EXTRA */
    // return string array
    public static final String EXTRA_PICTURE_PATH = "extraPicturePath";

    /** input EXTRA */
    // number <= 1, optional choose one；
    // number > 1, optional choose multiple (<number).
    public static final String EXTRA_PICURE_NUMBER = "extraPictureNumber";
    //true or false
    public static final String EXTRA_CAMERA = "extraCamera";
    // (width or height) < minSize, don't show in the list(default size = 200);
    public static final String EXTRA_FILTER_PICUTURE_MIN_SIZE = "extraFilterPictureMinSize";

    private final int REQUEST_CODE_TAKE_PICTURE = 1001;

    private final int UN_POSITION = -1;

    private static final String[] STORE_IMAGES = {
            MediaStore.Images.Media._ID,
//            MediaStore.Images.Media.TITLE,
//            MediaStore.Images.Media.DISPLAY_NAME,
//            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DEFAULT_SORT_ORDER,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
//            MediaStore.Images.Media.MIME_TYPE,
//            MediaStore.Images.Media.BUCKET_ID,
//            MediaStore.Images.Media.DATE_TAKEN,
//            MediaStore.Images.Media._ID,
//            MediaStore.Images.Media.DESCRIPTION,
//            MediaStore.Images.Media.LATITUDE,
//            MediaStore.Images.Media.LONGITUDE,
//            MediaStore.Images.Media.MINI_THUMB_MAGIC
    };

    private MenuItem mMenuItem;

    private RecyclerView mRecyclerView;
    private android.app.LoaderManager mLoaderManager;
    private GridLayoutManager mGridLayoutManager;
    private PictureAdapter mPictureAdapter;
    private int mChoosePictureId[] = new int[0];
    private int mFilterPictureSize;
    private int mChooseNumber = 0;

    private int mRootWidth;
    private int mImagePixels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mImagePixels = getResources().getDimensionPixelOffset(R.dimen.image_size);
        int n = getIntent().getIntExtra(EXTRA_PICURE_NUMBER, 1);
        initSelectedArray(n > 1 ? n : 1);
        mFilterPictureSize = getIntent().getIntExtra(EXTRA_FILTER_PICUTURE_MIN_SIZE, 200);
        mPictureAdapter = new PictureAdapter(getIntent().getBooleanExtra(EXTRA_CAMERA, true));
        mRecyclerView.setAdapter(mPictureAdapter);
        mGridLayoutManager = new GridLayoutManager(this, 1);
        mGridLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mLoaderManager = getLoaderManager();

        mLoaderManager.initLoader(1000, null, new android.app.LoaderManager.LoaderCallbacks<Cursor>() {

            @Override
            public android.content.Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
                String orderString = MediaStore.Images.Media.DATE_ADDED + " DESC";
                return new android.content.CursorLoader(MainActivity.this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, STORE_IMAGES, null, null, orderString);
            }

            @Override
            public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {
                List<String> list = new LinkedList<>();
                data.moveToFirst();
                do {
                    int width = data.getInt(data.getColumnIndex(MediaStore.Images.Media.WIDTH));
                    int height = data.getInt(data.getColumnIndex(MediaStore.Images.Media.HEIGHT));
                    if (Math.min(width, height) > mFilterPictureSize) {
                        list.add(data.getString(data.getColumnIndex(MediaStore.Images.Media.DATA)));
                    }
                } while (data.moveToNext());
                mPictureAdapter.setDatas(list);
            }

            @Override
            public void onLoaderReset(android.content.Loader<Cursor> loader) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        mMenuItem = menu.findItem(R.id.ok);
        mMenuItem.setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.ok) {
            String path[] = new String[mChooseNumber];
            if (mChooseNumber > 0) {
                for (int i = 0; i < mChooseNumber; i++) {
                    int id = mChoosePictureId[i];
                    if (id != UN_POSITION) {
                        path[i] = mPictureAdapter.getData(id);
                    }
                }
                setResultPath(path);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            if (mRecyclerView.getWidth() != mRootWidth) {
                mRootWidth = mRecyclerView.getWidth();
                mGridLayoutManager.setSpanCount(computeGrideSize());
                mRecyclerView.setLayoutManager(mGridLayoutManager);
                mPictureAdapter.notifyDataSetChanged();
            }
        }
    }

    private int computeGrideSize() {
        final int MIN_SPAN_COUNT = 3;

        int rootWidth = mRootWidth;
        int spanCount = MIN_SPAN_COUNT;
        int spanMax = rootWidth / mImagePixels;
        if (spanMax < MIN_SPAN_COUNT) {
            mImagePixels = rootWidth / MIN_SPAN_COUNT;
        } else {
            spanCount = spanMax;
            mImagePixels = rootWidth / spanMax;
        }
        return spanCount;
    }

    private void initSelectedArray(int count) {
        mChoosePictureId = new int[count];
        Arrays.fill(mChoosePictureId, UN_POSITION);
    }

    private boolean setPicture(int position) {
        boolean find = false;

        for (int i = 0; i < mChoosePictureId.length; i++) {
            if (find) {
                mChoosePictureId[i - 1] = mChoosePictureId[i];
                mChoosePictureId[i] = UN_POSITION;
            } else if (mChoosePictureId[i] == position) {
                find = true;
                mChoosePictureId[i] = UN_POSITION;
            } else if (mChoosePictureId.length == 1 || mChoosePictureId[i] == UN_POSITION) {
                mChoosePictureId[i] = position;
                i = mChoosePictureId.length;
            } else if (i == mChoosePictureId.length - 1) {
                if (mChoosePictureId[mChoosePictureId.length - 1] != UN_POSITION) {
                    Toast.makeText(this, R.string.image_choose_limit, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        int position = mRecyclerView.getChildLayoutPosition(view);
        if (mPictureAdapter.isCamera(position)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File file = new File(FileUtil.getInstance().getPictureTempPath());
            intent.putExtra(MediaStore.Images.Media.ORIENTATION, 0);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
        } else {
            if (mChoosePictureId.length == 1) {
                setResultPath(mPictureAdapter.getData(position));
            } else {
                setPicture(position);
                int chooseNumber = 0;
                for (int i: mChoosePictureId) {
                    if (i != UN_POSITION) {
                        chooseNumber++;
                    }
                }
                mChooseNumber = chooseNumber;
                mMenuItem.setVisible(mChooseNumber > 0);
                mMenuItem.setTitle(String.format(getString(R.string.choose_ok), mChooseNumber, mChoosePictureId.length));
                mPictureAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_TAKE_PICTURE:
                    setResultPath(FileUtil.getInstance().getPictureTempPath());
                    break;
            }
        }
    }

    private void setResultPath(String...picturePath) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_PICTURE_PATH, picturePath);
        setResult(RESULT_OK, intent);
        finish();
    }

    class PictureAdapter extends RecyclerView.Adapter<PictureAdapter.ViewHodler> {

        private List<String> datas = new LinkedList<>();
        private boolean showCamera = false;
        private final String CAMERA_URI = "camera";

        PictureAdapter(boolean showCamera) {
            this.showCamera = showCamera;
        }

        @Override
        public ViewHodler onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_item, null);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)view.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new RelativeLayout.LayoutParams(mImagePixels, mImagePixels);
            } else {
                layoutParams.width = mImagePixels;
                layoutParams.height = mImagePixels;
            }
            view.setLayoutParams(layoutParams);
            return new ViewHodler(view, viewType == 1);
        }

        @Override
        public int getItemViewType(int position) {
            for (int aMSelectedPictureId : mChoosePictureId) {
                if (aMSelectedPictureId == position) {
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public void onBindViewHolder(ViewHodler holder, int position) {
            holder.itemView.setOnClickListener(MainActivity.this);
            String url = datas.get(position);
            if (position == 0 && CAMERA_URI.equals(url)) {
                holder.imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                holder.imageView.setImageResource(R.drawable.ic_camera_alt_white);
                holder.chooseView.setVisibility(View.GONE);
                return;
            }
            if (mChoosePictureId.length > 1) {
                holder.chooseView.setVisibility(View.VISIBLE);
                holder.imageStatusView.setImageLevel(holder.isSelected ? 1 : 0);
            } else {
                holder.chooseView.setVisibility(View.GONE);
            }
            Glide.with(holder.imageView.getContext())
                    .load(url).override(460, 460).centerCrop().into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return datas.size();
        }

        void setDatas(List<String> datas) {
            this.datas.clear();
            if (showCamera) {
                this.datas.add(CAMERA_URI);
            }
            this.datas.addAll(datas);
        }

        String getData(int position) {

            if (position <= 0 || position > datas.size()) {
                return null;
            }
            position--;
            return datas.get(position);
        }

        boolean isCamera(int position) {
            return CAMERA_URI.equals(datas.get(position));
        }

        class ViewHodler extends RecyclerView.ViewHolder {
            ImageView imageView;
            ImageView imageStatusView;
            View chooseView;
            boolean isSelected;

            ViewHodler(View itemView, boolean isSelected) {
                super(itemView);
                imageView = (ImageView) itemView.findViewById(R.id.image_view);
                imageStatusView = (ImageView) itemView.findViewById(R.id.image_selected_ic);
                chooseView = itemView.findViewById(R.id.image_selected_layout);
                this.isSelected = isSelected;
            }
        }

    }

}
