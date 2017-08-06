package com.pashkobohdan.fastreadinglite;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.pashkobohdan.fastreadinglite.library.bookTextWorker.BookInfo;
import com.pashkobohdan.fastreadinglite.library.bookTextWorker.BookInfosList;
import com.pashkobohdan.fastreadinglite.library.bookTextWorker.Word;
import com.pashkobohdan.fastreadinglite.library.ui.button.ButtonContinuesClickAction;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_VOLUME_DOWN;
import static android.view.KeyEvent.KEYCODE_VOLUME_UP;

public class CurrentBook extends AppCompatActivity {
    public static final String REWARD_ADS_ID = "ca-app-pub-5629629863028147/2516191553";//"ca-app-pub-5629629863028147/3889904518";


    public static final String PRO_VERSION_PACKAGE = "com.pashkobohdan.fastreading";

    public static final String BOOK_INFO_EXTRA_NAME = "serializable_book_file";
    public static final double TIME_DELTA_LONG_WORDS = 1.5;
    public static final double TIME_DELTA_DOT = 1.5;
    public static final double TIME_DELTA_COMA = 1.3;
    public static final int RESTART_TIMER_TASK_ONLINE = -1;
    public static final int SPEED_CHANGE_STEP = 20;
    public static final int SPEED_MIN_VALUE = 20;
    public static final int DEFAULT_SPEED_MAX_VALUE = 2000;
    public static final int SPEED_MAX_VALUE = 200;
    public static final int REWIND_WORD_DELAY = 100;
    public static final int SPEED_CHANGING_DELAY = 50;
    public static final int NANOSECONDS_IN_ONE_SECOND = 1000 * 1000 * 1000;
    public static final double MILLISECONDS_IN_ONE_MINUTE = 60000.0;

    /**
     * Main object (activity works with it)
     */
    private BookInfo bookInfo;

    /**
     * UI elements (layouts and views)
     */
    private AppBarLayout appBarLayout;
    private LinearLayout topManagePanel, bottomManagePanel;
    private SeekBar currentPositionSeekBar;
    private TextView currentBookProgress;

    private RelativeLayout readingPanel;
    private TextView topBoundaryLine, bottomBoundaryLine;
    private TextView currentWordLeftPart, currentWordCenterPart, currentWordRightPart;
    //private TextView currentSpeed;
    private TextView newSpeedOnPlaying;

    private ImageButton positionForwardBack, positionBack, positionUp, positionForwardUp, speedPlus, speedMinus;

    /**
     * Reading help objects
     */
    enum ReadingStatus {
        STATUS_PLAYING,
        STATUS_PAUSE
    }

    private volatile ReadingStatus currentReadingStatus;
    private ArrayList<Word> words;
    private int readingPosition;

    /**
     * Text showing schedulers
     */
    private Timer timer;
    private TimerTask timerTask;
    private Handler handler = new Handler();

    /**
     * Good features
     */
    private boolean isUserRewind = false;
    private int lastPositionBeforeRewind = 0;

    private View mDecorView;
    private boolean hideStatusBarForReading;

    boolean speedChangingWhenReading = false;
    long lastUserChangingReading = 0;

    /**
     * Only for Lite version !
     */
    private TextView wordCountLeft;
    private AtomicInteger availableWordCount;
    public static final int DEFAULT_AVAILABLE_WORD_COUNT = 2000;
    private RewardedVideoAd mAd;
    private AdRequest mAdRequest;
    private AlertDialog maxSpeedAlertDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_book);

        findUiElements();


        Toolbar toolbar = (Toolbar) findViewById(R.id.current_book_toolbar);
        setSupportActionBar(toolbar);

        wordCountLeft = (TextView) toolbar.findViewById(R.id.word_count_left);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mDecorView = getWindow().getDecorView();

        // check bookInfo for cracks
        if (!getBookInfo()) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.error)
                    .setMessage(R.string.book_loading_error)
                    .setPositiveButton(R.string.ok, (dialog, which) -> finish())
                    .show();

            return;
        }

        // actionBar changing
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(bookInfo.getName());
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        /**
         *  Here's all right ! (if we're here)
         */
        // Use an activity context to get the rewarded video instance.
        mAd = MobileAds.getRewardedVideoAdInstance(this);
        mAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {
            @Override
            public void onRewardedVideoAdLoaded() {
            }

            @Override
            public void onRewardedVideoAdOpened() {

            }

            @Override
            public void onRewardedVideoStarted() {

            }

            @Override
            public void onRewardedVideoAdClosed() {
                loadRewardedVideoAd();
            }

            @Override
            public void onRewarded(RewardItem rewardItem) {
                Toast.makeText(CurrentBook.this, "Reward : " + rewardItem.getAmount() + " words", Toast.LENGTH_SHORT).show();
                availableWordCount.set(availableWordCount.get() + rewardItem.getAmount());
                wordCountLeft.setText(availableWordCount.get() + "");

                PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit().putInt("available_word_count", availableWordCount.get()).apply();

            }

            @Override
            public void onRewardedVideoAdLeftApplication() {

            }

            @Override
            public void onRewardedVideoAdFailedToLoad(int i) {
                Toast.makeText(CurrentBook.this, "Ads loading error. Try later", Toast.LENGTH_SHORT).show();
            }
        });
        mAdRequest = new AdRequest.Builder()
                .build();
        loadRewardedVideoAd();


        bookInfo.setLastOpeningDate((int) (new Date().getTime() / 1000));

        parseWords();

        initializeStartReadingValues();

        initializeListeners();

        /**
         *  start reading (pause mode)
         */
        refreshStatus(ReadingStatus.STATUS_PAUSE);
    }


    private void loadRewardedVideoAd() {
        mAd.loadAd(REWARD_ADS_ID, mAdRequest);
    }

    private void findUiElements() {
        appBarLayout = (AppBarLayout) findViewById(R.id.current_book_app_bar_layout);

        topManagePanel = (LinearLayout) findViewById(R.id.current_book_top_manage_panel);
        bottomManagePanel = (LinearLayout) findViewById(R.id.current_book_bottom_manage_panel);
        readingPanel = (RelativeLayout) findViewById(R.id.current_book_reading_space);

        currentPositionSeekBar = (SeekBar) findViewById(R.id.current_book_current_position_seek_bar);
        currentBookProgress = (TextView) findViewById(R.id.current_book_progress);
        currentWordLeftPart = (TextView) findViewById(R.id.current_book_left_part);
        currentWordCenterPart = (TextView) findViewById(R.id.current_book_center_part);
        currentWordRightPart = (TextView) findViewById(R.id.current_book_right_part);
        //currentSpeed = (TextView) findViewById(R.id.current_book_current_speed);
        positionForwardBack = (ImageButton) findViewById(R.id.current_book_speed_forward_back);
        positionBack = (ImageButton) findViewById(R.id.current_book_speed_back);
        positionUp = (ImageButton) findViewById(R.id.current_book_speed_up);
        positionForwardUp = (ImageButton) findViewById(R.id.current_book_speed_forward_up);

        speedPlus = (ImageButton) findViewById(R.id.current_book_speed_plus);
        speedMinus = (ImageButton) findViewById(R.id.current_book_speed_minus);

        topBoundaryLine = (TextView) findViewById(R.id.current_book_top_boundary_line);
        bottomBoundaryLine = (TextView) findViewById(R.id.current_book_bottom_boundary_line);

        newSpeedOnPlaying = (TextView) findViewById(R.id.current_book_online_new_speed);
        newSpeedOnPlaying.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mAd != null) {
            mAd.resume(this);
        }

        if (bookInfo == null) {
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean showBoundaryLines = preferences.getBoolean("boundary_lines", true);
        boolean anotherCenterColor = preferences.getBoolean("another_center_color", true);
        int wordColor = preferences.getInt("word_color", getResources().getColor(R.color.word_color_default));
        int centerLetterColor = preferences.getInt("center_letter_color", getResources().getColor(R.color.center_letter_color_default));
        int textSize = Integer.parseInt(preferences.getString("text_size", "20"));
        int boundaryLinesColor = preferences.getInt("boundary_lines_color", R.color.boundary_lines_color_default);
        int backgroundColor = preferences.getInt("background_color", getResources().getColor(R.color.white));
        int boundaryLines_thickness = Integer.parseInt(preferences.getString("boundary_lines_thickness", "2"));

        hideStatusBarForReading = preferences.getBoolean("hide_status_bar", false);


        availableWordCount = new AtomicInteger(PreferenceManager.getDefaultSharedPreferences(this).getInt("available_word_count", DEFAULT_AVAILABLE_WORD_COUNT));
        wordCountLeft.setText(availableWordCount.get() + "");

        //getWindow().getDecorView().setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));

        topManagePanel.setBackgroundColor(backgroundColor);
        readingPanel.setBackgroundColor(backgroundColor);
        if (isColorDark(backgroundColor)) {
            currentBookProgress.setTextColor(Color.WHITE);
            newSpeedOnPlaying.setTextColor(Color.WHITE);
        } else {
            currentBookProgress.setTextColor(Color.BLACK);
            newSpeedOnPlaying.setTextColor(Color.BLACK);
        }

        if (showBoundaryLines) {
            topBoundaryLine.setVisibility(View.VISIBLE);
            bottomBoundaryLine.setVisibility(View.VISIBLE);


            ViewGroup.LayoutParams params = topBoundaryLine.getLayoutParams();
            params.height = boundaryLines_thickness;
            topBoundaryLine.setLayoutParams(params);

            params = bottomBoundaryLine.getLayoutParams();
            params.height = boundaryLines_thickness;
            bottomBoundaryLine.setLayoutParams(params);


            topBoundaryLine.setBackgroundColor(boundaryLinesColor);
            bottomBoundaryLine.setBackgroundColor(boundaryLinesColor);
        } else {
            topBoundaryLine.setVisibility(View.GONE);
            bottomBoundaryLine.setVisibility(View.GONE);
        }

        currentWordLeftPart.setTextColor(wordColor);
        currentWordRightPart.setTextColor(wordColor);

        if (anotherCenterColor) {
            currentWordCenterPart.setTextColor(centerLetterColor);
        } else {
            currentWordCenterPart.setTextColor(wordColor);
        }

        currentWordLeftPart.setTextSize(textSize);
        currentWordCenterPart.setTextSize(textSize);
        currentWordRightPart.setTextSize(textSize);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mAd != null) {
            mAd.pause(this);
        }

        if (bookInfo == null) {
            return;
        }

        refreshStatus(ReadingStatus.STATUS_PAUSE);

        /**
         * Save left words cound
         */
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("available_word_count", availableWordCount.get()).apply();

        /**
         * Save vurrent position
         */
        bookInfo.setCurrentWordNumber(readingPosition);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (bookInfo == null) {
            return;
        }

        bookInfo.setCurrentWordNumber(readingPosition);
        refreshStatus(ReadingStatus.STATUS_PAUSE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mAd != null) {
            mAd.destroy(this);
        }


        bookInfo.setCurrentWordNumber(readingPosition);
        refreshStatus(ReadingStatus.STATUS_PAUSE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_current_book, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;

            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;

            case R.id.action_to_start:
                lastPositionBeforeRewind = readingPosition;
                setReadingPosition(0);
                isUserRewind = true;
                break;

            case R.id.action_cancel_last_rewind:
                if (isUserRewind) {
                    setReadingPosition(lastPositionBeforeRewind);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void speedMinus() {
        if (bookInfo.getCurrentSpeed() <= SPEED_MIN_VALUE) {

            // To Do
            bookInfo.setCurrentSpeed(SPEED_MIN_VALUE);
            //currentSpeed.setText(SPEED_MIN_VALUE + "");
            newSpeedOnPlaying.setText(getResources().getString(R.string.speed) + " : " + bookInfo.getCurrentSpeed());

            return;
        }

        bookInfo.setCurrentSpeed(bookInfo.getCurrentSpeed() - SPEED_CHANGE_STEP);
        //currentSpeed.setText(bookInfo.getCurrentSpeed() + "");

        if (currentReadingStatus == ReadingStatus.STATUS_PLAYING) {
            speedChangingWhenReading = true;
            lastUserChangingReading = System.nanoTime();

            newSpeedOnPlaying.setVisibility(View.VISIBLE);
            newSpeedOnPlaying.setText(getResources().getString(R.string.speed) + " : " + bookInfo.getCurrentSpeed());
            new Handler().postDelayed(() -> {
                if (System.nanoTime() - lastUserChangingReading > NANOSECONDS_IN_ONE_SECOND) {
                    newSpeedOnPlaying.setVisibility(View.GONE);
                    speedChangingWhenReading = false;
                }

            }, 1000);

            startOfRestartPlaying(RESTART_TIMER_TASK_ONLINE);
        } else {
            newSpeedOnPlaying.setText(getResources().getString(R.string.speed) + " : " + bookInfo.getCurrentSpeed());
        }
    }

    private void speedPlus() {
        if (maxSpeedAlertDialog == null) {
            maxSpeedAlertDialog = new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.information)
                    .setMessage(R.string.read_up_to_2k_pro)
                    .setPositiveButton(R.string.buy_pro, (dialog, which) -> {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PRO_VERSION_PACKAGE)));
                        } catch (ActivityNotFoundException anfe) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + PRO_VERSION_PACKAGE)));
                        }
                    })
                    .setNeutralButton(R.string.cant_buy_pro, (dialog, which) -> {
                        if (mAd.isLoaded()) {
                            mAd.show();
                        }
                    })

                    .create();
        }

        if (availableWordCount.get() > 0) {
            if (bookInfo.getCurrentSpeed() >= DEFAULT_SPEED_MAX_VALUE) {
                return;
            }
        } else {
            if (bookInfo.getCurrentSpeed() >= SPEED_MAX_VALUE) {

                if (!maxSpeedAlertDialog.isShowing()) {
                    maxSpeedAlertDialog.show();
                }

                return;
            }
        }

        bookInfo.setCurrentSpeed(bookInfo.getCurrentSpeed() + SPEED_CHANGE_STEP);
        // currentSpeed.setText(bookInfo.getCurrentSpeed() + "");
        newSpeedOnPlaying.setText(getResources().getString(R.string.speed) + " : " + bookInfo.getCurrentSpeed());

        if (currentReadingStatus == ReadingStatus.STATUS_PLAYING) {
            speedChangingWhenReading = true;
            lastUserChangingReading = System.nanoTime();

            newSpeedOnPlaying.setVisibility(View.VISIBLE);
            newSpeedOnPlaying.setText(getResources().getString(R.string.speed) + " : " + bookInfo.getCurrentSpeed());
            new Handler().postDelayed(() -> {
                if (System.nanoTime() - lastUserChangingReading > NANOSECONDS_IN_ONE_SECOND) {
                    newSpeedOnPlaying.setVisibility(View.GONE);
                    speedChangingWhenReading = false;
                }

            }, 1000);

            startOfRestartPlaying(RESTART_TIMER_TASK_ONLINE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {


        switch (keyCode) {
            case KEYCODE_VOLUME_DOWN:
                speedMinus();
                return true;

            case KEYCODE_VOLUME_UP:
                speedPlus();

                return true;

            case KEYCODE_BACK:
                //tryExitToBookList();
                finish();
                break;

        }

        return super.onKeyDown(keyCode, event);
    }


    /**
     * Business logic
     */

    private boolean getBookInfo() {
        Intent i = getIntent();
        File bookFile = (File) i.getSerializableExtra(BOOK_INFO_EXTRA_NAME);

        bookInfo = BookInfosList.get(bookFile);
        return bookInfo != null;
    }


    public boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }

    private void parseWords() {
        words = new ArrayList<>();

        for (String word : bookInfo.getWords()) {
            words.add(Word.newInstance(word));
        }
    }


    private void initializeStartReadingValues() {
        currentPositionSeekBar.setMax(words.size() - 1);

        setReadingPosition(bookInfo.getCurrentWordNumber());

        //currentSpeed.setText(bookInfo.getCurrentSpeed() + "");
        newSpeedOnPlaying.setText(getResources().getString(R.string.speed) + " : " + bookInfo.getCurrentSpeed());
    }

    private void initializeListeners() {
        mDecorView.setOnSystemUiVisibilityChangeListener(flags -> {
            boolean fullscreen = (flags & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
            if (!fullscreen) {
                showSystemUI();
                refreshStatus(ReadingStatus.STATUS_PAUSE);
            }
        });


        readingPanel.setOnClickListener(v -> {
            if (currentReadingStatus == ReadingStatus.STATUS_PAUSE) {
                if (hideStatusBarForReading) {
                    hideSystemUI();
                }
                refreshStatus(ReadingStatus.STATUS_PLAYING);
            } else {
                refreshStatus(ReadingStatus.STATUS_PAUSE);
            }
        });

        ButtonContinuesClickAction.setContinuesClickAction(positionForwardBack, () -> {
            int position;
            for (position = readingPosition - 2; position >= 0; position--) {
                if (bookInfo.getWords()[position].endsWith(".") ||
                        bookInfo.getWords()[position].endsWith("?") ||
                        bookInfo.getWords()[position].endsWith("!") ||
                        bookInfo.getWords()[position].endsWith(":")) {
                    break;
                }
            }

            setReadingPosition(position <= 0 ? 0 : position + 1);
        }, REWIND_WORD_DELAY);

        ButtonContinuesClickAction.setContinuesClickAction(positionBack,
                () -> setReadingPosition(getReadingPosition() == 0 ? 0 : getReadingPosition() - 1), REWIND_WORD_DELAY);

        ButtonContinuesClickAction.setContinuesClickAction(positionUp, () ->
                setReadingPosition(getReadingPosition() ==
                        bookInfo.getWords().length - 1 ? bookInfo.getWords().length - 1 : getReadingPosition() + 1), REWIND_WORD_DELAY);

        ButtonContinuesClickAction.setContinuesClickAction(positionForwardUp, () -> {
            int position;
            for (position = readingPosition + 1; position < bookInfo.getWords().length; position++) {
                if (bookInfo.getWords()[position].endsWith(".") ||
                        bookInfo.getWords()[position].endsWith("?") ||
                        bookInfo.getWords()[position].endsWith("!") ||
                        bookInfo.getWords()[position].endsWith(":")) {
                    break;
                }
            }

            setReadingPosition(position >= bookInfo.getWords().length - 1 ? bookInfo.getWords().length - 1 : position + 1);
        }, REWIND_WORD_DELAY);


        ButtonContinuesClickAction.setContinuesClickAction(speedPlus, this::speedPlus, SPEED_CHANGING_DELAY);

        ButtonContinuesClickAction.setContinuesClickAction(speedMinus, this::speedMinus, SPEED_CHANGING_DELAY);


        currentPositionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setReadingPosition(progress);

                    isUserRewind = true;
                } else {
                    isUserRewind = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopPlaying();
                lastPositionBeforeRewind = seekBar.getProgress();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void initializeTimerTask() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    if (currentReadingStatus == ReadingStatus.STATUS_PAUSE) {
                        stopPlaying();
                    }

                    if (getReadingPosition() < words.size() - 1) {
                        setReadingPosition(getReadingPosition() + 1);

                        refreshAvailableWordCount();
                    } else {
                        refreshStatus(ReadingStatus.STATUS_PAUSE);
                        showBookEndDialog();
                    }

                    if (getReadingPosition() < words.size() - 1) {
                        if (words.get(getReadingPosition()).toString().length() > 10) {
                            startOfRestartPlaying((int) (MILLISECONDS_IN_ONE_MINUTE * TIME_DELTA_LONG_WORDS / bookInfo.getCurrentSpeed()));
                        } else {
                            if (words.get(getReadingPosition()).toString().endsWith(".") ||
                                    words.get(getReadingPosition()).toString().endsWith("!") ||
                                    words.get(getReadingPosition()).toString().endsWith("?") ||
                                    words.get(getReadingPosition()).toString().endsWith(";")) {
                                startOfRestartPlaying((int) (MILLISECONDS_IN_ONE_MINUTE * TIME_DELTA_DOT / bookInfo.getCurrentSpeed()));
                            } else if (words.get(getReadingPosition()).toString().endsWith(",") ||
                                    words.get(getReadingPosition()).toString().endsWith(":") ||
                                    words.get(getReadingPosition()).toString().endsWith("-")) {
                                startOfRestartPlaying((int) (MILLISECONDS_IN_ONE_MINUTE * TIME_DELTA_COMA / bookInfo.getCurrentSpeed()));
                            }
                        }
                    }

                });
            }
        };
    }

    private void refreshAvailableWordCount() {
        if (bookInfo.getCurrentSpeed() <= SPEED_MAX_VALUE) {
            return;
        }

        wordCountLeft.setText(availableWordCount.decrementAndGet() + "");

        if (availableWordCount.get() <= 0) {
            /**
             * For bad case of concurrent :(
             */

            //availableWordCount.set(0);
            //wordCountLeft.setText("");//availableWordCount.get() + ""

            bookInfo.setCurrentSpeed(bookInfo.getCurrentSpeed() > SPEED_MAX_VALUE ? SPEED_MAX_VALUE : bookInfo.getCurrentSpeed());
            //currentSpeed.setText(bookInfo.getCurrentSpeed() + "");
            newSpeedOnPlaying.setText(getResources().getString(R.string.speed) + " : " + bookInfo.getCurrentSpeed());

            refreshStatus(ReadingStatus.STATUS_PAUSE);

            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.spent_words)
                    .setMessage(R.string.buy_or_ads)
                    .setPositiveButton(R.string.buy_pro, (dialog, which) -> {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PRO_VERSION_PACKAGE)));
                        } catch (ActivityNotFoundException anfe) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + PRO_VERSION_PACKAGE)));
                        }
                    })
                    .setNeutralButton(R.string.cant_buy_pro, (dialog, which) -> {
                        if (mAd.isLoaded()) {
                            mAd.show();
                        } else {
                            availableWordCount.set(0);
                            wordCountLeft.setText(availableWordCount.get() + "");
                        }
                    })
                    .show();

        }
    }

    private void refreshStatus(ReadingStatus newRefreshStatus) {
        if (newRefreshStatus == currentReadingStatus) {
            return;
        }
        currentReadingStatus = newRefreshStatus;

        switch (currentReadingStatus) {
            case STATUS_PAUSE:
                newSpeedOnPlaying.setVisibility(View.VISIBLE);

                appBarLayout.setVisibility(View.VISIBLE);
                topManagePanel.setVisibility(View.VISIBLE);
                bottomManagePanel.setVisibility(View.VISIBLE);

                stopPlaying();
                break;
            case STATUS_PLAYING:
                newSpeedOnPlaying.setVisibility(View.GONE);

                appBarLayout.setVisibility(View.GONE);
                topManagePanel.setVisibility(View.GONE);
                bottomManagePanel.setVisibility(View.GONE);

                startOfRestartPlaying(1000);
                break;
        }
    }


    private void hideSystemUI() {
        mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemUI() {
        mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }


    private void startOfRestartPlaying(int initialDelay) {
        stopPlaying();
        initializeTimerTask();
        timer = new Timer();

        timer.schedule(timerTask, initialDelay == -1 ?
                        (int) (MILLISECONDS_IN_ONE_MINUTE / bookInfo.getCurrentSpeed()) : initialDelay,
                (int) (MILLISECONDS_IN_ONE_MINUTE / bookInfo.getCurrentSpeed()));
    }

    private void stopPlaying() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void showBookEndDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.book_end)
                .setMessage(R.string.book_end_dialog_text)
                .setPositiveButton(R.string.yes, (dialog, which) -> setReadingPosition(0))
                .setNegativeButton(R.string.no, (dialog, which) -> {
                })
                .show();
    }


    public int getReadingPosition() {
        return readingPosition;
    }

    public void setReadingPosition(int readingPosition) {
        this.readingPosition = readingPosition;

        currentPositionSeekBar.setProgress(readingPosition);

        Word currentWord = words.get(readingPosition);
        currentWordLeftPart.setText(currentWord.getLeftPart());
        currentWordCenterPart.setText(currentWord.getCenterLetter());
        currentWordRightPart.setText(currentWord.getRightPart());

        String percent = ((readingPosition + 1.0) / words.size() * 100.0 + "");
        percent = percent.length() > 5 ? percent.substring(0, 5) : percent;


        currentBookProgress.setText(readingPosition + 1 + " / " + words.size() + " (" + percent + "%)");
    }
}
