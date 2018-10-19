package com.gtp.showapicturetoyourfriend;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;

public class receiverpictureactivity extends AppCompatActivity {

    //to make sure back button doesn't open old images
    @Override
    protected void onNewIntent(Intent intent) {
    finish();
    startActivity(intent);
    }

    Handler handly;
    Runnable goahead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //makes Window Fullscreen and show ontop of the Lockscreen
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_receiverpictureactivity);
        Window wind = this.getWindow();
        wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        wind.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //periodically checks if the screen is locked, if it is calls screenislocked()
        handly = new Handler();
        goahead = new Runnable() {
            @Override
            public void run() {
                KeyguardManager myKM = (KeyguardManager) getApplication().getSystemService(Context.KEYGUARD_SERVICE);
                if (myKM != null) {
                    if( myKM.inKeyguardRestrictedInputMode()) {
                        screenislocked();
                    } else {
                        handly.postDelayed(goahead, 40);
                    }
                } else {
                    handly.postDelayed(goahead, 40);
                }
            }
        };
        goahead.run();

    }

    public void buttonpressed(View view) { //called when button is pressed
        screenislocked();
    }

    public void screenislocked() {
        handly.removeCallbacks(goahead);

        PowerManager.WakeLock screenLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
        screenLock.acquire(1);

        screenLock.release();
        //removes handler, wakes up screen and realeases Wakelock immediately

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        setContentView(R.layout.activity_receivemultiple);

        ArrayList<Uri> imageUris = null;

        if(Intent.ACTION_SEND.equals(action)) { //puts Uris into an array, whether there is one or multiple
            Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            imageUris = new ArrayList<>();
            imageUris.add(imageUri);
        } else if(Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        } //puts Uris into an array, whether there is one or multiple

        DemoCollectionPagerAdapter.setCounts(imageUris.size());
        DemoCollectionPagerAdapter.setUris(imageUris, this);

        PagerAdapter mDemoCollectionPagerAdapter = new DemoCollectionPagerAdapter(getSupportFragmentManager());
        DemoCollectionPagerAdapter.setAdapter(mDemoCollectionPagerAdapter);
        ViewPager mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setAdapter(mDemoCollectionPagerAdapter);
    }

    @Override
    protected void onDestroy() {
        handly.removeCallbacks(goahead);
        super.onDestroy();
    }

    public static class DemoCollectionPagerAdapter extends FragmentStatePagerAdapter {
        public DemoCollectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        static ArrayList<Uri> uris;
        static Context context;

        public static void recreateafterpermission() { //this is called when the user gives permission to view file
            atp.notifyDataSetChanged();
        }

        public static void setUris(ArrayList<Uri> muri, Context c) {
            uris=muri;
            context = c;
        }

        public static void setAdapter(PagerAdapter adapter) {
            atp = adapter;
        }
        static PagerAdapter atp;

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new DemoObjectFragment();
            Bundle args = new Bundle();

            uris.set(3, null);

            Uri uri = uris.get(i);
            String stringuri = "";
            if(uri != null) {
                stringuri = uri.toString();
            } else {
                Toast.makeText(context, R.string.invalid, Toast.LENGTH_LONG).show();
            }

            args.putString("Uri",stringuri);
            fragment.setArguments(args);
            return fragment;
        }

        static int count;

        @Override
        public int getCount() {
            return count;
        }

        public static void setCounts(int mcount) {
            count = mcount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "OBJECT " + (position + 1);
        }
    }

    public static class DemoObjectFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View rootView = null;

            Bundle args = getArguments();
            Uri urinormal = Uri.parse(args.getString("Uri"));

            String type = null;
            String extension = MimeTypeMap.getFileExtensionFromUrl(args.getString("Uri"));
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }

            String startwith = getActivity().getContentResolver().getType(urinormal);

            if(startwith!=null) {
                if (startwith.startsWith("image/")) {
                    rootView = inflater.inflate(R.layout.adapterimage, container, false);
                    pictureSet((TouchImageView) rootView.findViewById(R.id.touchImageView), urinormal);
                } else if (startwith.startsWith("video/")) {
                    rootView = inflater.inflate(R.layout.adaptervideo, container, false);
                    videoSet((VideoView) rootView.findViewById(R.id.videoview), urinormal);
                }
            } else {
                if(type!=null) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        if (getActivity().checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            rootView=typeMethod(rootView,urinormal,container,type,inflater);
                        } else {
                            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                            Toast.makeText(getActivity(), R.string.permission, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        rootView=typeMethod(rootView,urinormal,container,type,inflater);
                    }
                }
            }

            if(viewvisibleinoncreate) {
                viewnowvisible(true);
            }

            return rootView;

        }

        private View typeMethod(View rootView, Uri urinormal, ViewGroup container, String type,LayoutInflater inflater) {
            if (type.startsWith("image/")) {
                rootView = inflater.inflate(R.layout.adapterimage, container, false);
                pictureSetFile((TouchImageView) rootView.findViewById(R.id.touchImageView), urinormal);
            } else if (type.startsWith("video/")) {
                rootView = inflater.inflate(R.layout.adaptervideo, container, false);
                videoSet((VideoView) rootView.findViewById(R.id.videoview), urinormal);
            }
            return rootView;
        }

        private void pictureSet(final TouchImageView imageset, Uri urinormal) {

            imageset.setMaxZoom(30);
            Glide
                    .with(this)
                    .load(urinormal)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .into(new GlideDrawableImageViewTarget(imageset) {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                            super.onResourceReady(resource, animation);
                            imageset.setZoom(1);
                        }
                    })
            ;
        }

        private void pictureSetFile(final TouchImageView imageset, Uri urinormal) {
            imageset.setMaxZoom(30);
            Glide
                    .with(this)
                    .load(urinormal)
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .into(new GlideDrawableImageViewTarget(imageset) {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                            super.onResourceReady(resource, animation);
                            imageset.setZoom(1);
                        }
                    })
            ;
        }

        private void videoSet(VideoView video, Uri urinormal) {
            video.setVideoURI(urinormal);
            video.seekTo(1);
            controller = new MediaController(getActivity());
            videow = video;
            isvideo = true;
        }

        VideoView videow;
        MediaController controller;
        Boolean viewvisibleinoncreate = false;
        Boolean isvideo = false;
        Boolean iscontrollershowing = false;

        @Override
        public void setUserVisibleHint(boolean isVisibleToUser) {
            super.setUserVisibleHint(isVisibleToUser);
            if (getView() != null) {
                viewnowvisible(isVisibleToUser);
            } else {
                viewvisibleinoncreate = isVisibleToUser;
            }
        }

        public void viewnowvisible(boolean isVisibleToUser) {
            if (isvideo) {
                if(isVisibleToUser) {
                    Log.d("r","VIDEO ON");
                    if(iscontrollershowing) {
                        controller.show();
                    } else {
                        controller.setAnchorView(videow);
                        controller.setMediaPlayer(videow);
                        videow.setMediaController(controller);
                        iscontrollershowing = true;
                    }
                    videow.start();
                } else {
                    Log.d("r","VIDEO OFF");
                    videow.pause();
                    controller.hide();
                }
            }
        }

    }
}
